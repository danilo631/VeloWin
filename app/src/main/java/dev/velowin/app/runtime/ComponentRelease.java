package dev.velowin.app.runtime;

/** Verified metadata from the official upstream GitHub release endpoint. */
public final class ComponentRelease {
    public enum Kind { DXVK, TERMUX_X11_APK }
    public final Kind kind;
    public final String tag,name,url,sha256;
    public final long size;
    public ComponentRelease(Kind kind,String tag,String name,String url,String sha256,long size){
        this.kind=kind;this.tag=tag;this.name=name;this.url=url;this.sha256=sha256;
        this.size=size;
    }
    @Override public String toString(){return tag+" · "+size/1048576+" MiB";}
}
