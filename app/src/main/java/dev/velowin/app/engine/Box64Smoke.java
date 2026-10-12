package dev.velowin.app.engine;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

/**
 * A controlled --version probe of optional ARM64 Android/ Bionic Box64 binary.
 * APK without the experimental native artifact intentionally reports missing.
 * Never spawns Wine, Windows EXE, root or a shell.
 */
public final class Box64Smoke {
    private Box64Smoke() {}
    private static File executable(Context ctx) {
        return new File(ctx.getApplicationInfo().nativeLibraryDir,"libvelobox64.so");
    }
    public static boolean bundled(Context ctx) {
        File b=executable(ctx);
        return b.isFile() && b.canExecute() && b.length()>1024*1024;
    }
    public static String inspect(Context ctx) {
        File b=executable(ctx);
        if(!bundled(ctx))return "Box64 Bionic não incluído neste APK. "
            +"Use o workflow experimental para compilar o tradutor ARM64.";
        Process process=null;
        try {
            ProcessBuilder builder=new ProcessBuilder(b.getAbsolutePath(),"-v");
            builder.directory(ctx.getFilesDir());
            builder.redirectErrorStream(true);
            builder.environment().put("BOX64_NOBANNER","0");
            process=builder.start();
            boolean completed=process.waitFor(6, TimeUnit.SECONDS);
            if(!completed){
                process.destroyForcibly();
                return "Box64: limite de tempo atingido no autoteste.";
            }
            String output=limited(process.getInputStream());
            return "Box64 --version (exit "+process.exitValue()+"):\n"+output;
        }catch(Exception exception){
            return "Box64 indisponível: "+exception.getClass().getSimpleName()
                +": "+exception.getMessage();
        } finally {
            if(process!=null && process.isAlive())process.destroyForcibly();
        }
    }
    private static String limited(InputStream in) throws IOException {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        byte[] block=new byte[512];
        int n;
        while(output.size()<2048 && (n=in.read(block,0,
                Math.min(block.length,2048-output.size())))!=-1){
            output.write(block,0,n);
        }
        return output.toString("UTF-8");
    }
}
