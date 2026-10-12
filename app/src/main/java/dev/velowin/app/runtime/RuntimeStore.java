package dev.velowin.app.runtime;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class RuntimeStore {
    public static final class Installed {
        public final String tag, file, sha;
        public final long bytes;
        public Installed(String tag, String file, String sha, long bytes) {
            this.tag = tag; this.file = file; this.sha = sha; this.bytes = bytes;
        }
    }
    private final Context context;
    private final AtomicFile state;
    private final ArrayList<Installed> entries = new ArrayList<>();
    private boolean damaged;

    public RuntimeStore(Context context) {
        this.context = context.getApplicationContext();
        state = new AtomicFile(new File(context.getFilesDir(), "runtimes.json"));
        try (FileInputStream in = state.openRead()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] block = new byte[4096];
            int n;
            while ((n = in.read(block)) != -1) {
                if (buffer.size() + n > 512 * 1024) throw new IOException("registry oversized");
                buffer.write(block, 0, n);
            }
            JSONArray data = new JSONArray(buffer.toString("UTF-8"));
            for (int i = 0; i < data.length(); i++) {
                JSONObject row = data.getJSONObject(i);
                entries.add(new Installed(row.getString("tag"), row.getString("file"),
                    row.getString("sha"), row.getLong("bytes")));
            }
        }
        catch (java.io.FileNotFoundException ignored) {}
        catch (Exception error) {
            damaged = true;
            Log.e("VeloWinRuntime", "Registry damaged; preserving local data", error);
        }
    }

    public File folder() {
        File folder = new File(context.getFilesDir(), "verified-runtimes");
        if (!folder.isDirectory() && !folder.mkdirs())
            throw new IllegalStateException("Could not create runtime storage");
        return folder;
    }

    public synchronized boolean isDamaged() { return damaged; }

    public synchronized List<Installed> all() {
        ArrayList<Installed> valid = new ArrayList<>();
        for (Installed e : entries) {
            if (ReleasePolicy.supported(e.tag, e.file)) {
                File archive = new File(folder(), e.tag + "--" + e.file);
                if (archive.isFile() && archive.length() == e.bytes) valid.add(e);
            }
        }
        return valid;
    }

    public synchronized void add(ReleaseEntry info) throws IOException {
        if (damaged) throw new IOException("Registro de runtimes danificado; preserve seus arquivos.");
        entries.removeIf(e -> e.tag.equals(info.tag) && e.file.equals(info.name));
        entries.add(new Installed(info.tag, info.name, info.sha256, info.size));
        save();
    }

    public synchronized void remove(Installed runtime) throws IOException {
        if (damaged) throw new IOException("Registro de runtimes danificado.");
        File destination = new File(folder(), runtime.tag + "--" + runtime.file);
        if (destination.exists() && !destination.delete())
            throw new IOException("Não foi possível excluir o arquivo.");
        entries.removeIf(e -> e.tag.equals(runtime.tag) && e.file.equals(runtime.file));
        save();
    }

    private void save() throws IOException {
        JSONArray output = new JSONArray();
        for (Installed entry : entries) {
            JSONObject row = new JSONObject();
            try {
                row.put("tag", entry.tag);
                row.put("file", entry.file);
                row.put("sha", entry.sha);
                row.put("bytes", entry.bytes);
            }
            catch (Exception error) { throw new IOException("Failed to serialize runtime", error); }
            output.put(row);
        }
        FileOutputStream stream = null;
        try {
            stream = state.startWrite();
            stream.write(output.toString().getBytes(StandardCharsets.UTF_8));
            state.finishWrite(stream);
        }
        catch (IOException error) {
            if (stream != null) state.failWrite(stream);
            throw error;
        }
    }
}
