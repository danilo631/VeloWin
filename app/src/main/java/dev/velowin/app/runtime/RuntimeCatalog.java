package dev.velowin.app.runtime;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import javax.net.ssl.HttpsURLConnection;

/** Official public GitHub releases; only verified Linux/glibc WoW64 candidates. */
public final class RuntimeCatalog {
    private RuntimeCatalog() {}

    public static ArrayList<ReleaseEntry> search() throws Exception {
        HttpsURLConnection connection = open(ReleasePolicy.API);
        try {
            if (connection.getResponseCode() != 200)
                throw new IOException("GitHub API HTTP " + connection.getResponseCode());
            String raw = limitedText(connection.getInputStream(), 3 * 1024 * 1024);
            JSONArray data = new JSONArray(raw);
            ArrayList<ReleaseEntry> result = new ArrayList<>();
            int proton = 0, staging = 0;
            for (int i = 0; i < data.length() && result.size() < 10; i++) {
                JSONObject release = data.optJSONObject(i);
                if (release == null || release.optBoolean("draft") || release.optBoolean("prerelease")) continue;
                String tag = release.optString("tag_name");
                String body = release.optString("body");
                JSONArray assets = release.optJSONArray("assets");
                if (assets == null) continue;
                boolean isProton = tag.startsWith("proton-");
                if (isProton && proton >= 6 || !isProton && staging >= 4) continue;
                for (int j = 0; j < assets.length(); j++) {
                    JSONObject candidate = assets.optJSONObject(j);
                    if (candidate == null) continue;
                    String name = candidate.optString("name");
                    String expected = ReleasePolicy.expectedUrl(tag, name);
                    long size = candidate.optLong("size", 0);
                    String sha = ReleasePolicy.checksum(body, name);
                    if (expected == null || !expected.equals(candidate.optString("browser_download_url"))
                        || !ReleasePolicy.validSize(size) || sha == null) continue;
                    result.add(new ReleaseEntry(tag, name, expected, sha, size));
                    if (isProton) proton++; else staging++;
                    break;
                }
            }
            if (result.isEmpty()) throw new IOException("Nenhum release compatível com hash publicado.");
            return result;
        }
        finally { connection.disconnect(); }
    }

    static HttpsURLConnection open(String address) throws IOException {
        URL url = new URL(address);
        if (!"https".equals(url.getProtocol())) throw new IOException("HTTPS obrigatório");
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "VeloWin/0.1 Android");
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        return connection;
    }

    private static String limitedText(InputStream stream, int max) throws IOException {
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) {
                if (out.size() + n > max) throw new IOException("Resposta GitHub grande demais.");
                out.write(chunk, 0, n);
            }
            return out.toString("UTF-8");
        }
    }
}
