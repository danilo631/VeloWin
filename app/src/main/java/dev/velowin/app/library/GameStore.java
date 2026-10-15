package dev.velowin.app.library;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class GameStore {
    private final AtomicFile disk;
    private final ArrayList<Game> items = new ArrayList<>();
    private boolean damaged = false;

    public GameStore(Context context) {
        disk = new AtomicFile(new File(context.getFilesDir(), "library.json"));
        try (FileInputStream in = disk.openRead()) {
            JSONArray records = new JSONArray(readLimited(in, 2 * 1024 * 1024));
            for (int i = 0; i < records.length(); i++)
                items.add(Game.fromJson(records.getJSONObject(i)));
        }
        catch (java.io.FileNotFoundException absent) {
            // First launch: no library yet.
        }
        catch (Exception error) {
            damaged = true;
            Log.e("VeloWinLibrary", "Could not parse existing library; refusing to overwrite", error);
        }
    }

    /** Return deep snapshots; UI changes must never mutate the store before durable write. */
    public synchronized List<Game> all() {
        ArrayList<Game> copies = new ArrayList<>();
        for (Game item : items) copies.add(item.copy());
        return copies;
    }
    public synchronized boolean isDamaged() { return damaged; }

    public synchronized void put(Game game) throws IOException {
        ensureHealthy();
        ArrayList<Game> updated = new ArrayList<>(items);
        boolean found = false;
        for (int i = 0; i < updated.size(); i++) {
            if (updated.get(i).id.equals(game.id)) {
                updated.set(i, game.copy());
                found = true;
                break;
            }
        }
        if (!found) updated.add(game.copy());
        persist(updated);
        items.clear();
        items.addAll(updated);
    }

    public synchronized void remove(String id) throws IOException {
        ensureHealthy();
        ArrayList<Game> updated = new ArrayList<>(items);
        updated.removeIf(item -> item.id.equals(id));
        persist(updated);
        items.clear();
        items.addAll(updated);
    }

    private void ensureHealthy() throws IOException {
        if (damaged) throw new IOException("A biblioteca local está danificada. Faça backup antes de alterar.");
    }

    /** Two-phase update: write JSON first, then commit in-memory snapshot. */
    private void persist(List<Game> snapshot) throws IOException {
        JSONArray data = new JSONArray();
        try { for (Game game : snapshot) data.put(game.toJson()); }
        catch (JSONException error) { throw new IOException("Failed to serialize library", error); }
        FileOutputStream out = null;
        try {
            out = disk.startWrite();
            out.write(data.toString().getBytes(StandardCharsets.UTF_8));
            disk.finishWrite(out);
        }
        catch (IOException error) {
            if (out != null) disk.failWrite(out);
            throw error;
        }
    }

    private static String readLimited(FileInputStream input, int max) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = input.read(buffer)) >= 0) {
            if (out.size() + n > max) throw new IOException("library too large");
            out.write(buffer, 0, n);
        }
        return out.toString("UTF-8");
    }
}
