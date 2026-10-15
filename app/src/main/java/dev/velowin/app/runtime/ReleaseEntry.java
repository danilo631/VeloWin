package dev.velowin.app.runtime;

public final class ReleaseEntry {
    public final String tag, name, url, sha256;
    public final long size;
    public final boolean proton;

    public ReleaseEntry(String tag, String name, String url, String sha256, long size) {
        this.tag = tag;
        this.name = name;
        this.url = url;
        this.sha256 = sha256;
        this.size = size;
        this.proton = tag.startsWith("proton-");
    }

    @Override public String toString() {
        return (proton ? "Proton " + tag.substring(7) : "Wine Staging " + tag)
            + "  ·  " + (size / 1048576) + " MB";
    }
}
