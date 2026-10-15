import dev.velowin.core.VeloImageFsMaintenance;
import dev.velowin.core.VeloContentTransaction;
import dev.velowin.core.VeloInstallCoordinator;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

public class VeloInstallModulesTest {
    static void ok(boolean v,String label){if(!v)throw new AssertionError(label);}
    static void write(Path p,String value)throws Exception{
        Files.createDirectories(p.getParent());
        Files.write(p,value.getBytes(StandardCharsets.UTF_8));
    }
    static void refused(RunnableThrows r,String label)throws Exception{
        try{r.run();throw new AssertionError("Expected refusal: "+label);}
        catch(IOException expected){}
    }
    interface RunnableThrows {void run() throws Exception;}
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("velo-fs-");
        Path outside=Files.createTempDirectory("velo-outside-");
        write(root.resolve("home/xuser-1/.wine/drive_c/save.dat"),"save");
        write(root.resolve("opt/installed-wine/Wine-9/files"),"installed");
        write(root.resolve("opt/previous/system"),"replace");
        write(root.resolve("usr/bin/old"),"old");
        write(outside.resolve("survive"),"outside");
        Files.createSymbolicLink(root.resolve("guest-link"),outside);
        VeloImageFsMaintenance.prepare(root.toFile());
        ok(Files.isRegularFile(root.resolve("home/xuser-1/.wine/drive_c/save.dat")),"home untouched");
        ok(Files.isRegularFile(root.resolve("opt/installed-wine/Wine-9/files")),"installed Wine");
        ok(!Files.exists(root.resolve("usr/bin/old")),"old binaries cleaned");
        ok(!Files.exists(root.resolve("guest-link"),LinkOption.NOFOLLOW_LINKS),"guest link cleaned");
        ok(Files.isRegularFile(outside.resolve("survive")),"do not follow outside link");
        ok(!Files.exists(root.resolve("opt/previous")),"old optional package cleaned");
        Path bad=Files.createTempDirectory("velo-bad-");
        Files.createSymbolicLink(bad.resolve("home"),outside);
        write(bad.resolve("keep"),"keep");
        refused(()->VeloImageFsMaintenance.prepare(bad.toFile()),"symlink home");
        ok(Files.exists(bad.resolve("keep")),"preflight blocks before cleanup");
        Path content=Files.createTempDirectory("velo-install-");
        write(content.resolve("staging/bin/wine"),"wine");
        Path dest=content.resolve("packages/wine-9");
        VeloContentTransaction.commit(content.resolve("staging").toFile(),dest.toFile());
        ok(Files.isRegularFile(dest.resolve("bin/wine")),"atomic commit");
        write(content.resolve("new/bin/wine"),"changed");
        refused(()->VeloContentTransaction.commit(content.resolve("new").toFile(),
            dest.toFile()),"no overwrite");
        ok(Files.exists(content.resolve("new/bin/wine")),"failed commit preserves staging");
        ok(VeloInstallCoordinator.beginImageFs(),"first install");
        ok(!VeloInstallCoordinator.beginImageFs(),"second install blocked");
        VeloInstallCoordinator.endImageFs();
        ok(VeloInstallCoordinator.beginImageFs(),"released");
        VeloInstallCoordinator.endImageFs();
        System.out.println("VeloInstallModulesTest PASS");
    }
}
