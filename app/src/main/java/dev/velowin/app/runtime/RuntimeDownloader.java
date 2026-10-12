package dev.velowin.app.runtime;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;

/** Downloads are stored inert, verified, and NOT extracted/executed. */
public final class RuntimeDownloader {
    public interface Progress { void onProgress(int percent); }
    private RuntimeDownloader() {}

    public static File download(Context context, ReleaseEntry entry, AtomicBoolean cancel,
                                Progress progress) throws Exception {
        String expected = ReleasePolicy.expectedUrl(entry.tag, entry.name);
        if (expected == null || !expected.equals(entry.url)
                || !ReleasePolicy.validSize(entry.size)
                || entry.sha256 == null || !entry.sha256.matches("[a-f0-9]{64}"))
            throw new IOException("Pacote de runtime não confiável.");

        RuntimeStore registry = new RuntimeStore(context);
        if (registry.isDamaged()) throw new IOException("Registro local de runtimes danificado.");
        File directory = registry.folder();
        if (directory.getUsableSpace() < entry.size + 128L * 1024 * 1024)
            throw new IOException("Sem espaço livre suficiente para o download.");
        File target = new File(directory, entry.tag + "--" + entry.name);
        File staging = new File(directory, entry.tag + "--" + entry.name + ".part");
        if (staging.exists() && !staging.delete())
            throw new IOException("Não foi possível limpar download anterior.");

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        long bytes = 0;
        try {
            HttpsURLConnection connection = RuntimeCatalog.open(entry.url);
            try {
                for (int i = 0; i < 5 && connection.getResponseCode() / 100 == 3; i++) {
                    String location = connection.getHeaderField("Location");
                    if (!ReleasePolicy.trustedRedirect(location))
                        throw new IOException("Redirecionamento externo bloqueado.");
                    connection.disconnect();
                    connection = RuntimeCatalog.open(location);
                }
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                    throw new IOException("GitHub HTTP " + connection.getResponseCode());
                long headerSize = connection.getContentLengthLong();
                if (headerSize > ReleasePolicy.MAX_BYTES ||
                    headerSize >= 0 && headerSize != entry.size)
                    throw new IOException("Tamanho informado não corresponde ao release.");

                try (InputStream in = connection.getInputStream();
                     FileOutputStream out = new FileOutputStream(staging)) {
                    byte[] block = new byte[65536];
                    int count, reported = -1;
                    while ((count = in.read(block)) != -1) {
                        if (cancel.get() || Thread.currentThread().isInterrupted())
                            throw new IOException("Download cancelado.");
                        bytes += count;
                        if (bytes > entry.size) throw new IOException("Download maior que o esperado.");
                        md.update(block, 0, count);
                        out.write(block, 0, count);
                        int percent = (int)(bytes * 100L / entry.size);
                        if (percent != reported) { reported = percent; progress.onProgress(percent); }
                    }
                    out.getFD().sync();
                }
            }
            finally { connection.disconnect(); }

            if (bytes != entry.size) throw new IOException("Download incompleto.");
            StringBuilder calculated = new StringBuilder(64);
            for (byte b : md.digest())
                calculated.append(String.format(Locale.ROOT, "%02x", b & 255));
            if (!entry.sha256.equals(calculated.toString()))
                throw new IOException("SHA-256 não confere. Download rejeitado.");
            if (cancel.get()) throw new IOException("Download cancelado.");
            if (target.exists() && !target.delete())
                throw new IOException("Não foi possível substituir pacote antigo.");
            if (!staging.renameTo(target))
                throw new IOException("Falha ao finalizar arquivo.");
            registry.add(entry);
            return target;
        }
        catch (Exception error) {
            staging.delete();
            throw error;
        }
    }
}
