package dev.velowin.app.library;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

public final class Game {
    public String id, title, uri, backend, graphics, architecture, runtimeTag, notes, boxPreset;
    public boolean favorite;
    public String resolution, renderApi;
    public int fpsLimit;
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
        this.boxPreset = "BALANCED";
        this.resolution = "1280x720";
        this.renderApi = "WINED3D";
        this.fpsLimit = 60;
        this.createdAt = System.currentTimeMillis();
    }

    private Game() {}

    public Game copy() {
        Game g = new Game();
        g.id = id; g.title = title; g.uri = uri;
        g.backend = backend; g.graphics = graphics;
        g.architecture = architecture; g.runtimeTag = runtimeTag;
        g.notes = notes; g.favorite = favorite; g.createdAt = createdAt;
        g.boxPreset = boxPreset;
        g.resolution = resolution; g.renderApi = renderApi; g.fpsLimit = fpsLimit;
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
        game.boxPreset = json.optString("boxPreset", "BALANCED");
        game.resolution = dev.velowin.app.engine.GameSessionConfig.resolution(
            json.optString("resolution", "1280x720"));
        game.renderApi = dev.velowin.app.engine.GameSessionConfig.graphics(
            json.optString("renderApi", "WINED3D")).name();
        game.fpsLimit = dev.velowin.app.engine.GameSessionConfig.fps(json.optInt("fpsLimit",60));
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
        out.put("boxPreset", boxPreset);
        out.put("resolution", resolution);
        out.put("renderApi", renderApi);
        out.put("fpsLimit", fpsLimit);
        return out;
    }
}
