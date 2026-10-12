import dev.velowin.app.engine.PrefixArchivePath;

public class PrefixArchivePathTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        check(".wine/dosdevices/c:".equals(
            PrefixArchivePath.member("./.wine/dosdevices/c:")),"C drive alias");
        check(".wine/dosdevices/z:".equals(
            PrefixArchivePath.member(".wine/dosdevices/z:")),"Z drive alias");
        check(PrefixArchivePath.member("C:/windows")==null,"Windows absolute");
        check(PrefixArchivePath.member(".wine/drive_c/g.exe:evil")==null,"NTFS ADS");
        check(PrefixArchivePath.member("../.wine/dosdevices/c:")==null,"parent traversal");
        check(PrefixArchivePath.member(".wine/dosdevices/cc:")==null,"only drive letters");
        check(PrefixArchivePath.member(".wine/dosdevices/c:/system.reg")!=null,
            "a child path under drive alias remains rejected by symlink-ancestor check");
        check(".wine/drive_c".equals(PrefixArchivePath.linkTarget(
            ".wine/dosdevices/c:","../drive_c")),"internal C drive symlink");
        check(PrefixArchivePath.linkTarget(".wine/dosdevices/z:","/")==null,
            "absolute Z link must be policy-skipped");
        check(PrefixArchivePath.linkTarget(".wine/dosdevices/c:","../../../etc")==null,
            "relative escape");
        System.out.println("PrefixArchivePathTest PASS");
    }
}
