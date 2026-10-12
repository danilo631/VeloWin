package dev.velowin.app.engine;

import dev.velowin.app.runtime.ArchiveSafety;

/**
 * Conservative policy for unpacked Wine prefix symlinks.
 * Linux Wine packs commonly link Z: to absolute /, which must never expose
 * Android's host filesystem; omit this link rather than allowing it.
 */
public final class PrefixLinkPolicy {
    public enum Decision { KEEP_RELATIVE, SKIP_HOST_ROOT_Z, REJECT }
    private PrefixLinkPolicy() {}
    public static Decision decide(String member,String target) {
        String safe=ArchiveSafety.member(member);
        if(safe==null || safe.isEmpty() || target==null)return Decision.REJECT;
        if(safe.endsWith("/dosdevices/z:") && "/".equals(target))
            return Decision.SKIP_HOST_ROOT_Z;
        if(ArchiveSafety.symlinkTarget(safe,target)!=null)
            return Decision.KEEP_RELATIVE;
        return Decision.REJECT;
    }
}
