package dev.velowin.app.runtime;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * Path rules for streamed TAR extraction. Pure Java so tests run without Android.
 * Conservative: no absolute paths, Windows separators, NTFS ADS, or traversal.
 */
public final class ArchiveSafety {
    private ArchiveSafety() {}

    public static String member(String path) {
        if (path == null || path.length() > 4096 || path.isEmpty() || path.charAt(0) == '/'
                || path.indexOf('\\') != -1 || path.indexOf('\0') != -1) return null;
        ArrayDeque<String> parts = new ArrayDeque<>();
        for (String segment : path.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment)) continue;
            if ("..".equals(segment) || segment.length() > 240 ||
                segment.indexOf(':') != -1) return null;
            parts.add(segment);
        }
        return parts.isEmpty() ? "" : String.join("/", parts);
    }

    /** Check the symlink's *lexical* target while preserving its original relative spelling. */
    public static String symlinkTarget(String member, String link) {
        String name = member(member);
        if (name == null || name.isEmpty() || link == null || link.isEmpty() ||
                link.length() > 4096 || link.charAt(0) == '/' ||
                link.indexOf('\\') != -1 || link.indexOf('\0') != -1) return null;
        String parent = name.lastIndexOf('/') < 0 ? "" : name.substring(0, name.lastIndexOf('/'));
        ArrayDeque<String> parts = new ArrayDeque<>();
        if (!parent.isEmpty()) for (String part : parent.split("/")) parts.add(part);
        for (String part : link.split("/", -1)) {
            if (part.isEmpty() || ".".equals(part)) continue;
            if ("..".equals(part)) {
                if (parts.isEmpty()) return null;
                parts.removeLast();
            }
            else if (part.length() > 240 || part.indexOf(':') >= 0) return null;
            else parts.addLast(part);
        }
        return parts.isEmpty() ? null : String.join("/", parts);
    }

    /** Files written under an archive symlink may redirect writes outside root. */
    public static boolean conflicts(String path, Set<String> links) {
        if (path == null || links == null) return true;
        if (links.contains(path)) return true;
        int slash = path.indexOf('/');
        while (slash >= 0) {
            if (links.contains(path.substring(0, slash))) return true;
            slash = path.indexOf('/', slash + 1);
        }
        return false;
    }

    public static String rootDir(String name) {
        String result = member(name);
        return result == null || result.isEmpty() ? null : result.split("/")[0];
    }

    public static boolean supportedArchive(String name) {
        return name != null && name.matches("wine-(proton-[0-9][A-Za-z0-9.-]*|[0-9]+\\.[0-9]+-staging)-amd64-wow64\\.tar\\.xz");
    }
}
