package dev.velowin.app.runtime;

import android.content.Context;
import android.system.Os;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Installs a checked, inert Linux Wine filesystem from a downloaded .tar.xz.
 *
 * No binfmt, no root, no shell; a complete rootfs/Box64 engine is NOT provided.
 * Source is re-hashed before extraction; all destination paths are checked;
 * symlinks are installed only after regular files and verified lexically.
 */
public final class WinePackageInstaller {
    private static final int MAX_FILES = 240000;
    private static final long MAX_UNPACKED = 7L * 1024 * 1024 * 1024;
    private static final long MAX_FILE = 2L * 1024 * 1024 * 1024;

    public interface Progress { void update(int entries); }

    private WinePackageInstaller() {}

    public static File destination(Context context, RuntimeStore.Installed source) {
        if (!ReleasePolicy.supported(source.tag, source.file)
            || !ArchiveSafety.supportedArchive(source.file))
            throw new IllegalArgumentException("Identificador Wine inválido.");
        String folder = source.tag + "--" + source.file.substring(0, source.file.length()-7);
        return new File(new File(context.getFilesDir(), "wine-filesystems"), folder);
    }

    public static boolean installed(Context context, RuntimeStore.Installed source) {
        File folder = destination(context, source);
        File manifest = new File(folder, ".velowin.json");
        if (!manifest.isFile() || manifest.length() > 2048) return false;
        try (FileInputStream in = new FileInputStream(manifest)) {
            byte[] bytes = new byte[(int)manifest.length()];
            int off = 0;
            while (off < bytes.length) {
                int n = in.read(bytes, off, bytes.length-off);
                if (n <= 0) return false;
                off += n;
            }
            JSONObject m = new JSONObject(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            String prefix = m.getString("root");
            if (ArchiveSafety.member(prefix) == null || prefix.isEmpty()
                || prefix.contains("/")) return false;
            File wine = new File(new File(folder, prefix), "bin/wine");
            return source.tag.equals(m.getString("tag"))
                && source.sha.equals(m.getString("sha256"))
                && wine.isFile() && contained(folder, wine);
        }
        catch (Exception e) { return false; }
    }

    public static File install(Context context, RuntimeStore.Installed source,
                               AtomicBoolean cancel, Progress progress) throws Exception {
        if (!ReleasePolicy.supported(source.tag, source.file)
                || !ArchiveSafety.supportedArchive(source.file)
                || !source.sha.matches("[a-f0-9]{64}"))
            throw new IOException("Registro de Wine inválido.");

        File file = new File(new RuntimeStore(context).folder(), source.tag + "--" + source.file);
        if (!file.isFile() || file.length() != source.bytes)
            throw new IOException("Arquivo original ausente ou incompleto.");
        if (installed(context, source)) return destination(context, source);
        if (cancel.get()) throw new IOException("Instalação cancelada.");
        String digest = checksum(file, cancel);
        if (!source.sha.equals(digest)) throw new IOException("SHA-256 do pacote mudou. Refaça o download.");

        File base = new File(context.getFilesDir(), "wine-filesystems");
        if (!base.isDirectory() && !base.mkdirs())
            throw new IOException("Falha ao criar pasta de instalação.");
        File finalDir = destination(context, source);
        if (finalDir.exists())
            throw new IOException("A instalação anterior está incompleta. Remova-a antes de tentar novamente.");

        File temp = new File(base, ".staging-" + UUID.randomUUID());
        if (!temp.mkdir()) throw new IOException("Sem acesso à pasta de preparação.");
        long bytes = 0;
        int entries = 0;
        Set<String> visited = new HashSet<>();
        Set<String> links = new HashSet<>();
        List<String[]> pendingLinks = new ArrayList<>();
        String prefix = null;
        try {
            try (InputStream fs = new BufferedInputStream(new FileInputStream(file), 65536);
                 XZCompressorInputStream xz = new XZCompressorInputStream(fs);
                 TarArchiveInputStream tar = new TarArchiveInputStream(xz)) {
                TarArchiveEntry entry;
                byte[] buf = new byte[65536];
                while ((entry = tar.getNextEntry()) != null) {
                    if (cancel.get() || Thread.currentThread().isInterrupted())
                        throw new IOException("Instalação cancelada.");
                    if (!tar.canReadEntryData(entry) || ++entries > MAX_FILES)
                        throw new IOException("Entrada TAR inválida ou excessiva.");

                    String name = ArchiveSafety.member(entry.getName());
                    if (name == null) throw new IOException("Caminho proibido no arquivo TAR.");
                    if (name.isEmpty() && entry.isDirectory()) continue;
                    if (name.isEmpty()) throw new IOException("Entrada TAR sem nome.");
                    if (ArchiveSafety.conflicts(name, links) || !visited.add(name))
                        throw new IOException("Entrada TAR duplicada ou sob link: " + name);

                    String root = ArchiveSafety.rootDir(name);
                    if (prefix == null) prefix = root;
                    else if (!prefix.equals(root))
                        throw new IOException("O pacote contém múltiplas pastas de topo.");

                    long length = entry.getSize();
                    if (length < 0 || length > MAX_FILE || bytes > MAX_UNPACKED-length)
                        throw new IOException("Limite de descompactação excedido.");
                    bytes += length;

                    File dest = new File(temp, name);
                    if (!contained(temp, dest)) throw new IOException("Destino fora da instalação.");
                    if (entry.isSymbolicLink()) {
                        String target = ArchiveSafety.symlinkTarget(name, entry.getLinkName());
                        if (target == null)
                            throw new IOException("Link simbólico fora do diretório Wine.");
                        if (!links.add(name)) throw new IOException("Link duplicado.");
                        pendingLinks.add(new String[]{name, entry.getLinkName()});
                        continue;
                    }
                    if (!entry.isFile() && !entry.isDirectory())
                        throw new IOException("Tipo especial de arquivo TAR bloqueado.");

                    if (dest.exists() && (dest.isFile() && entry.isDirectory()
                            || dest.isDirectory() && entry.isFile()))
                        throw new IOException("Arquivo conflitante.");
                    if (entry.isDirectory()) {
                        if (!dest.isDirectory() && !dest.mkdirs())
                            throw new IOException("Falha ao criar diretório.");
                    }
                    else {
                        File parent = dest.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs())
                            throw new IOException("Falha ao preparar diretório.");
                        if (dest.exists()) throw new IOException("Arquivo sobreposto.");
                        try (FileOutputStream out = new FileOutputStream(dest)) {
                            long count = 0;
                            while (count < length) {
                                if (cancel.get() || Thread.currentThread().isInterrupted())
                                    throw new IOException("Instalação cancelada.");
                                int n = tar.read(buf, 0, (int)Math.min(buf.length, length-count));
                                if (n < 0) throw new IOException("Arquivo TAR truncado.");
                                out.write(buf, 0, n);
                                count += n;
                            }
                        }
                        Os.chmod(dest.getAbsolutePath(), (entry.getMode() & 0111) != 0 ? 0700 : 0600);
                    }
                    if (entries % 300 == 0) progress.update(entries);
                }
            }

            if (prefix == null) throw new IOException("Pacote Wine vazio.");

            // A symlink declared after files might alias an already created directory.
            // Reject this; tar header order must never change extraction meaning.
            for (String[] link : pendingLinks) {
                String name = link[0], target = link[1];
                File dest = new File(temp, name);
                if (dest.exists() || Files.isSymbolicLink(dest.toPath()))
                    throw new IOException("Link sobreposto a um arquivo.");
                File parent = dest.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs())
                    throw new IOException("Diretório de link indisponível.");
                if (!contained(temp, dest)) throw new IOException("Link fora da instalação.");
                if (ArchiveSafety.symlinkTarget(name, target) == null)
                    throw new IOException("Alvo do link inválido.");
                Os.symlink(target, dest.getAbsolutePath());
            }

            File bin = new File(new File(temp, prefix), "bin/wine");
            if (!bin.isFile() || !contained(temp, bin))
                throw new IOException("Pacote não fornece bin/wine.");
            if (!ElfVerifier.isX86_64(bin))
                throw new IOException("O bin/wine deste pacote não é ELF x86_64 válido.");
            // No shell or executable invoked here.
            JSONObject marker = new JSONObject();
            marker.put("tag", source.tag);
            marker.put("file", source.file);
            marker.put("sha256", source.sha);
            marker.put("root", prefix);
            try (FileOutputStream out = new FileOutputStream(new File(temp, ".velowin.json"))) {
                out.write(marker.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            if (cancel.get()) throw new IOException("Instalação cancelada.");
            try {
                Files.move(temp.toPath(), finalDir.toPath(),
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            }
            catch (java.nio.file.AtomicMoveNotSupportedException unavailable) {
                if (!temp.renameTo(finalDir)) throw new IOException("Falha ao concluir instalação.");
            }
            return finalDir;
        }
        catch (Exception e) {
            deleteTree(temp);
            throw e;
        }
    }

    private static String checksum(File file, AtomicBoolean cancel) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) {
                if (cancel.get()) throw new IOException("Cancelado.");
                sha.update(buf, 0, n);
            }
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte b : sha.digest())
            hex.append(String.format(Locale.ROOT, "%02x", b & 255));
        return hex.toString();
    }

    private static boolean contained(File root, File target) {
        try {
            String base = root.getCanonicalPath() + File.separator;
            return target.getCanonicalPath().startsWith(base);
        }
        catch (IOException e) { return false; }
    }

    /** walkFileTree does NOT follow symlinks; prevents deletion outside private storage. */
    public static void deleteTree(File folder) throws IOException {
        if (!folder.exists() && !Files.isSymbolicLink(folder.toPath())) return;
        Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path directory, IOException error)
                    throws IOException {
                if (error != null) throw error;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void uninstall(Context context, RuntimeStore.Installed source) throws IOException {
        File dest = destination(context, source);
        if (!dest.exists()) return;
        // Restrict deletion to a single validated install directory.
        try {
            if (!dest.getCanonicalFile().getParentFile().equals(
                    new File(context.getFilesDir(), "wine-filesystems").getCanonicalFile()))
                throw new IOException("Pasta de instalação fora do destino permitido.");
        }
        catch (IOException error) { throw error; }
        deleteTree(dest);
    }
}
