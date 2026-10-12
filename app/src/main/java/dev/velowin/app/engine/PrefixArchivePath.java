package dev.velowin.app.engine;

import java.util.ArrayDeque;

/**
 * A deliberately narrower path parser for Wineprefix templates.
 * TAR archive paths are still sandboxed. Only the DOS device symlink name
 * such as ".wine/dosdevices/c:" may contain a colon.
 *
 * The generic ArchiveSafety parser continues to forbid every colon in regular
 * Wine releases; never relax those rules for user selected paths.
 */
public final class PrefixArchivePath {
    private PrefixArchivePath() {}

    public static String member(String path) {
        if(path==null || path.isEmpty() || path.length()>4096 ||
            path.charAt(0)=='/' || path.indexOf('\\')>=0 || path.indexOf('\0')>=0)
            return null;
        ArrayDeque<String> parts=new ArrayDeque<>();
        for(String segment:path.split("/",-1)) {
            if(segment.isEmpty() || segment.equals("."))continue;
            if(segment.equals("..") || segment.length()>240)return null;
            if(segment.indexOf(':')>=0) {
                if(parts.isEmpty() || !parts.peekLast().equals("dosdevices")
                    || !segment.matches("[A-Za-z]:"))return null;
            }
            parts.addLast(segment);
        }
        return String.join("/",parts);
    }

    public static String linkTarget(String member,String target) {
        String validated=member(member);
        if(validated==null || validated.isEmpty() || target==null || target.isEmpty()
            || target.length()>4096 || target.charAt(0)=='/'
            || target.indexOf('\\')>=0 || target.indexOf('\0')>=0)
            return null;
        int last=validated.lastIndexOf('/');
        ArrayDeque<String> parts=new ArrayDeque<>();
        if(last>=0)for(String s:validated.substring(0,last).split("/"))parts.addLast(s);
        for(String component:target.split("/",-1)) {
            if(component.isEmpty() || component.equals("."))continue;
            if(component.equals("..")){
                if(parts.isEmpty())return null;
                parts.removeLast();
            } else {
                if(component.length()>240 || component.indexOf(':')>=0)return null;
                parts.addLast(component);
            }
        }
        return parts.isEmpty()?null:String.join("/",parts);
    }
}
