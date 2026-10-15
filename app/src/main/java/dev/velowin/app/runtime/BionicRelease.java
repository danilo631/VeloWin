package dev.velowin.app.runtime;

/** GitHub-signed metadata is not a code signing root; digest verifies artifact bytes. */
public final class BionicRelease {
    public final String tag, name, uri, digest;
    public final long size;
    public final String arch;
    public BionicRelease(String tag, String name, String uri, String digest, long size, String arch) {
        this.tag=tag; this.name=name; this.uri=uri; this.digest=digest; this.size=size; this.arch=arch;
    }
    @Override public String toString() {
        return tag + " · " + arch + " · " + size/1048576 + " MB";
    }
}
