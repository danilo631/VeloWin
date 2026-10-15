package dev.velowin.app.runtime;

public final class BionicReleasePolicy {
    public static final String API =
        "https://api.github.com/repos/GameNative/proton-wine/releases?per_page=8";
    private static final String PREFIX =
        "https://github.com/GameNative/proton-wine/releases/download/";
    private BionicReleasePolicy() {}

    public static String url(String tag, String name) {
        if (tag == null || !tag.matches("proton-[0-9]+\\.[0-9]+-[0-9]+-20[0-9]{6}"))
            return null;
        if (name == null ||
                !name.matches("proton-[0-9]+\\.[0-9]+-[0-9]+-(x86_64|arm64ec)\\.wcp"))
            return null;
        return PREFIX + tag + "/" + name;
    }

    public static String hash(String githubAssetDigest) {
        if (githubAssetDigest == null
            || !githubAssetDigest.matches("sha256:[a-fA-F0-9]{64}")) return null;
        return githubAssetDigest.substring(7).toLowerCase(java.util.Locale.ROOT);
    }

    public static boolean valid(String tag, String name, String url,
                                String digest, long size) {
        String expected = url(tag, name);
        return expected != null && expected.equals(url)
            && hash(digest) != null
            && ReleasePolicy.validSize(size);
    }
}
