package dev.velowin.app.library;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

public final class Game {
    public String id, title, uri, backend, graphics, architecture, runtimeTag, notes;
    public boolean favorite;
    public long createdAt;

    public Game(String title, String uri) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.uri = uri;
        this.backend = "BOX64";
        this.graphics = "SYSTEM";
        this.architecture = "unknown";
        this.runtimeTag = "";
        this.notes = "";
        this.favorite = false;
        this.createdAt = System.currentTimeMillis();
    }

    private Game() {}

    public Game copy() {
        Game g = new Game();
        g.id = id; g.title = title; g.uri = uri;
        g.backend = backend; g.graphics = graphics;
        g.architecture = architecture; g.runtimeTag = runtimeTag;
        g.notes = notes; g.favorite = favorite; g.createdAt = createdAt;
        return g;
    }

    public static Game fromJson(JSONObject json) throws JSONException {
        Game game = new Game();
        game.id = json.getString("id");
        game.title = json.getString("title");
        game.uri = json.getString("uri");
        game.backend = json.optString("backend", "BOX64");
        game.graphics = json.optString("graphics", "SYSTEM");
        game.createdAt = json.optLong("createdAt", 0);
        game.architecture = json.optString("architecture", "unknown");
        game.runtimeTag = json.optString("runtimeTag", "");
        game.notes = json.optString("notes", "");
        game.favorite = json.optBoolean("favorite", false);
        return game;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject out = new JSONObject();
        out.put("id", id);
        out.put("title", title);
        out.put("uri", uri);
        out.put("backend", backend);
        out.put("graphics", graphics);
        out.put("createdAt", createdAt);
        out.put("architecture", architecture);
        out.put("runtimeTag", runtimeTag);
        out.put("notes", notes);
        out.put("favorite", favorite);
        return out;
    }
}
