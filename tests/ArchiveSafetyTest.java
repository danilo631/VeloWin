import dev.velowin.app.runtime.ArchiveSafety;
import java.util.Set;
import java.util.HashSet;

public class ArchiveSafetyTest {
    private static void ok(boolean b, String name) { if (!b) throw new AssertionError(name); }
    public static void main(String[] args) {
        ok("wine-11/bin/wine".equals(ArchiveSafety.member("./wine-11/bin/wine")), "normal member");
        ok(ArchiveSafety.member("../../etc/passwd") == null, "traversal");
        ok(ArchiveSafety.member("a/b/../../x") == null, "any parent traversal");
        ok(ArchiveSafety.member("/system/bin/sh") == null, "absolute");
        ok(ArchiveSafety.member("C:/Windows") == null, "drive");
        ok(ArchiveSafety.member("a\\b") == null, "windows separator");
        ok(ArchiveSafety.member("a/evil\0name") == null, "NUL");
        ok("foo/bar".equals(ArchiveSafety.symlinkTarget("foo/lib/x", "../bar")), "relative link");
        ok(ArchiveSafety.symlinkTarget("foo/lib/x", "../../../etc/passwd") == null, "link escape");
        ok(ArchiveSafety.symlinkTarget("foo/bin/wine", "/system/bin/linker64") == null, "absolute symlink");
        ok(ArchiveSafety.symlinkTarget("foo/lib/x", "C:/passwd") == null, "colon link");
        ok(ArchiveSafety.symlinkTarget("foo/link", ".") != null, "relative directory link");
        ok(ArchiveSafety.conflicts("wine/lib/file", Set.of("wine/lib")), "symlink ancestor");
        ok(ArchiveSafety.conflicts("wine/lib", Set.of("wine/lib")), "symlink replacement");
        ok(!ArchiveSafety.conflicts("wine/bin/wine", Set.of("wine/lib")), "independent path");
        ok(ArchiveSafety.supportedArchive("wine-proton-11.0-2-amd64-wow64.tar.xz"), "proton layout");
        ok(ArchiveSafety.supportedArchive("wine-11.19-staging-amd64-wow64.tar.xz"), "staging layout");
        ok(!ArchiveSafety.supportedArchive("proton-11.0-2-arm64ec.wcp"), "Bionic package");
        System.out.println("ArchiveSafetyTest: PASS");
    }
}
