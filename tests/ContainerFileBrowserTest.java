import dev.velowin.app.engine.ContainerFileBrowser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ContainerFileBrowserTest {
    private static void ok(boolean value,String reason) {
        if(!value)throw new AssertionError(reason);
    }
    private static void rejected(ContainerFileBrowser browser,String path) {
        try{browser.resolve(path);throw new AssertionError("accepted "+path);}
        catch(java.io.IOException expected){}
    }
    public static void main(String[] args)throws Exception {
        Path parent=Files.createTempDirectory("velowin-file-browser-");
        Path root=Files.createDirectory(parent.resolve("root"));
        Path outside=Files.createDirectory(parent.resolve("secret"));
        Files.write(outside.resolve("private.txt"),"HOST_SECRET".getBytes(StandardCharsets.UTF_8));
        ContainerFileBrowser writable=new ContainerFileBrowser(root.toFile(),true);
        writable.makeFolder("","drive_c");
        writable.makeFolder("drive_c","Program Files");
        Files.write(root.resolve("drive_c/readme.txt"),
            "Hello Wine!".getBytes(StandardCharsets.UTF_8));
        Files.write(root.resolve("drive_c/run.exe"),new byte[]{77,90,0,0});
        ok(writable.list("").get(0).directory,"directories first");
        ok(writable.list("drive_c").size()==3,"all regular entries listed");
        ok(writable.parent("drive_c/Program Files").equals("drive_c"),"up navigation");
        ok(writable.parent("").equals(""),"root navigation");
        ok(writable.preview("drive_c/readme.txt").contains("Hello Wine"),"text viewer");
        ok(writable.inspect("drive_c/run.exe").contains("Nenhum executável"),
            "binary metadata without execution");
        boolean cannotPreview=false;
        try {writable.preview("drive_c/run.exe");}
        catch(java.io.IOException expected) {cannotPreview=true;}
        ok(cannotPreview,"binary extension preview prohibited");
        for(String path:new String[]{"../secret/private.txt","drive_c/../../secret",
            "/etc/passwd","drive_c//readme.txt","drive_c/./readme.txt",
            "drive_c/../readme.txt","drive_c\\readme.txt"})rejected(writable,path);
        for(String name:new String[]{"../danger","x/y","..",".","bad\\dir"}) {
            boolean failed=false;
            try{writable.makeFolder("",name);}catch(java.io.IOException ex){failed=true;}
            ok(failed,"folder name rejected: "+name);
        }
        long imported=writable.importFile("drive_c","helper.dll",
            new java.io.ByteArrayInputStream(new byte[]{1,2,3,4}));
        ok(imported==4 && Files.size(root.resolve("drive_c/helper.dll"))==4,
            "SAF data copied into private game folder");
        boolean overwriteRejected=false;
        try{writable.importFile("drive_c","helper.dll",
            new java.io.ByteArrayInputStream(new byte[]{9}));}
        catch(java.io.IOException expected){overwriteRejected=true;}
        ok(overwriteRejected && Files.size(root.resolve("drive_c/helper.dll"))==4,
            "existing game resource never overwritten");
        boolean maliciousRejected=false;
        try{writable.importFile("drive_c","../../evil.dll",
            new java.io.ByteArrayInputStream(new byte[]{9}));}
        catch(java.io.IOException expected){maliciousRejected=true;}
        ok(maliciousRejected,"untrusted SAF display name confined");
        Files.createSymbolicLink(root.resolve("external"),outside);
        rejected(writable,"external/private.txt");
        ok(writable.list("").stream().noneMatch(e->e.name.equals("external")),
            "symlinks never exposed");
        boolean rootLinkRejected=false;
        try{new ContainerFileBrowser(root.resolve("external").toFile(),false);}
        catch(java.io.IOException expected){rootLinkRejected=true;}
        ok(rootLinkRejected,"root symlink rejected");
        ContainerFileBrowser readonly=new ContainerFileBrowser(root.toFile(),false);
        boolean restricted=false;
        try{readonly.makeFolder("","not-allowed");}
        catch(java.io.IOException expected){restricted=true;}
        ok(restricted,"runtime/prefix root stays read-only");
        boolean readonlyImportRejected=false;
        try{readonly.importFile("","override.dll",
            new java.io.ByteArrayInputStream(new byte[]{9}));}
        catch(java.io.IOException expected){readonlyImportRejected=true;}
        ok(readonlyImportRejected,"Wine/prefix read-only file import refused");

        Files.write(root.resolve("notes.log"),new byte[500*1024]);
        boolean largeRejected=false;
        try{readonly.preview("notes.log");}
        catch(java.io.IOException expected){largeRejected=true;}
        ok(largeRejected,"binary or oversized preview rejected");
        System.out.println("ContainerFileBrowserTest PASS");
    }
}
