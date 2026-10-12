package dev.velowin.app.runtime;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure Java policy. Never auto-import Android/Bionic Wine into a Linux/glibc runtime. */
public final class ReleasePolicy {
    public static final long MAX_BYTES = 384L * 1024 * 1024;
    public static final String API = "https://api.github.com/repos/Kron4ek/Wine-Builds/releases?per_page=30";
    private static final String ROOT = "https://github.com/Kron4ek/Wine-Builds/releases/download/";
    private static final Pattern SUM = Pattern.compile("(?m)^\\s*([a-fA-F0-9]{64})\\s+([^\\s]+)\\s*$");
    private ReleasePolicy() {}

    public static boolean supported(String tag, String file) {
        if (tag == null || !tag.matches("[a-zA-Z0-9._-]{2,64}") || file == null) return false;
        if (tag.matches("proton-[0-9][0-9a-zA-Z.\\-]*")
                && !tag.contains("exp") && !tag.contains("beta") && !tag.contains("rc"))
            return file.equals("wine-" + tag + "-amd64-wow64.tar.xz");
        if (tag.matches("[0-9]+\\.[0-9]+"))
            return file.equals("wine-" + tag + "-staging-amd64-wow64.tar.xz");
        return false;
    }

    public static String expectedUrl(String tag, String file) {
        if (!supported(tag, file)) return null;
        return ROOT + tag + "/" + file;
    }

    public static String checksum(String body, String file) {
        if (body == null || file == null) return null;
        Matcher m = SUM.matcher(body);
        while (m.find()) if (file.equals(m.group(2)))
            return m.group(1).toLowerCase(Locale.ROOT);
        return null;
    }

    public static boolean trustedRedirect(String address) {
        try {
            URI uri = URI.create(address);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getUserInfo() != null || uri.getPort() != -1) return false;
            String host = uri.getHost();
            return "release-assets.githubusercontent.com".equals(host)
                || "github-releases.githubusercontent.com".equals(host)
                || "objects.githubusercontent.com".equals(host);
        }
        catch (RuntimeException error) { return false; }
    }

    public static boolean validSize(long bytes) {
        return bytes > 4096 && bytes <= MAX_BYTES;
    }
}
