package dev.velowin.app.library;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

public final class Game {
    public String id, title, uri, backend, graphics;
    public long createdAt;

    public Game(String title, String uri) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.uri = uri;
        this.backend = "BOX64";
        this.graphics = "SYSTEM";
        this.createdAt = System.currentTimeMillis();
    }

    private Game() {}

    public static Game fromJson(JSONObject json) throws JSONException {
        Game game = new Game();
        game.id = json.getString("id");
        game.title = json.getString("title");
        game.uri = json.getString("uri");
        game.backend = json.optString("backend", "BOX64");
        game.graphics = json.optString("graphics", "SYSTEM");
        game.createdAt = json.optLong("createdAt", 0);
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
        return out;
    }
}
