import dev.velowin.app.engine.Box64Tuning;
import dev.velowin.app.engine.WineLaunchEnvironment;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public class WineLaunchEnvironmentTest {
    static void ok(boolean p,String label){if(!p)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception {
        Path root=Files.createTempDirectory("velowin-bionic-wcp-");
        Path wine=root.resolve("bin/wine");
        Files.createDirectories(wine.getParent());
        // Synthetic ELF64 / PT_INTERP = /system/bin/linker64, NOT an executable test.
        byte[] elf=new byte[160];
        elf[0]=0x7f; elf[1]='E'; elf[2]='L'; elf[3]='F';
        elf[4]=2; elf[5]=1; elf[16]=3; elf[18]=62;
        elf[32]=64; elf[54]=56; elf[56]=1;
        elf[64]=3; elf[72]=120;
        byte[] interp="/system/bin/linker64".getBytes(StandardCharsets.UTF_8);
        elf[96]=(byte)(interp.length+1);
        System.arraycopy(interp,0,elf,120,interp.length);
        Files.write(wine,elf);
        Files.write(root.resolve("bin/wineserver"),elf);
        Files.createDirectories(root.resolve("lib/wine/x86_64-unix"));
        File workspace=Files.createTempDirectory("velowin-work-").toFile();
        File prefix=new File(workspace,"prefix");
        Map<String,String> env=WineLaunchEnvironment.build(root.toFile(),workspace,prefix,
            Box64Tuning.Preset.BALANCED,Collections.emptyMap(),true);
        ok(env.get("PATH").startsWith(root.resolve("bin").toString()+":"),
            "Bionic wine/bin first");
        ok(env.get("PATH").endsWith(":/system/bin:/system/xbin"),
            "no arbitrary host PATH");
        ok(env.get("WINESERVER").equals(root.resolve("bin/wineserver").toString()),
            "explicit wineserver");
        ok(env.get("WINELOADER").equals(wine.toString()),"explicit wine loader");
        ok(env.get("WINEPREFIX").equals(prefix.getCanonicalPath()),"prefix chosen");
        ok(env.get("BOX64_LD_LIBRARY_PATH").contains("x86_64-unix"),"x86_64 libs");
        ok(!env.containsKey("LD_LIBRARY_PATH"),"native Android linker untouched");
        ok("C.UTF-8".equals(env.get("LC_ALL")),"stable locale");
        Map<String,String> lite=WineLaunchEnvironment.build(root.toFile(),workspace,
            null,Box64Tuning.Preset.STABILITY,Collections.emptyMap(),false);
        ok(!lite.containsKey("WINEPREFIX"),"Wine version doesn't touch prefix");
        ok("-all".equals(lite.get("WINEDEBUG")),"low-noise default");
        Files.delete(wine);
        boolean rejected=false;
        try {
            WineLaunchEnvironment.build(root.toFile(),workspace,null,
                Box64Tuning.Preset.BALANCED,Collections.emptyMap(),false);
        } catch(IOException expected) { rejected=true; }
        ok(rejected,"invalid Wine ELF fail closed");
        System.out.println("WineLaunchEnvironmentTest PASS");
    }
}
