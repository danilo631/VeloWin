package dev.velowin.app.engine;

import android.content.Context;
import dev.velowin.app.runtime.BionicRelease;
import dev.velowin.app.runtime.BionicWineInstaller;
import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Explicit, bounded diagnostic: try only Wine --version, never an arbitrary EXE.
 * Needs experimental APK's packaged Bionic Box64 and a SHA-verified WCP install.
 */
public final class WineVersionSmoke {
    private WineVersionSmoke() {}

    public static String run(Context context,BionicRelease release) {
        if(release==null||!"x86_64".equals(release.arch))
            return "Apenas Wine x86_64 Bionic é suportado no autoteste Box64.";
        if(!Box64Smoke.bundled(context))
            return "Este APK não inclui Box64. Use o APK experimental Box64.";
        if(!BionicWineInstaller.installed(context,release))
            return "O Wine Bionic não está preparado/validado.";
        Process process=null;
        try {
            File home=BionicWorkspace.prepare(context);
            File wineDir=BionicWineInstaller.folder(context,release);
            File executable=new File(wineDir,"bin/wine").getCanonicalFile();
            if(!executable.getPath().startsWith(wineDir.getCanonicalPath()+File.separator))
                return "Execução Wine bloqueada: binário saiu do diretório confiável.";
            File translator=new File(context.getApplicationInfo().nativeLibraryDir,"libvelobox64.so");
            ProcessBuilder pb=new ProcessBuilder(translator.getAbsolutePath(),
                executable.getAbsolutePath(),"--version");
            pb.directory(home);
            pb.redirectErrorStream(true);
            pb.environment().remove("LD_PRELOAD");
            pb.environment().put("HOME",new File(home,"home").getAbsolutePath());
            pb.environment().put("TMPDIR",new File(home,"tmp").getAbsolutePath());
            pb.environment().put("BOX64_NOBANNER","1");
            pb.environment().put("WINEDEBUG","-all");
            pb.environment().put("BOX64_LD_LIBRARY_PATH",
                new File(wineDir,"lib/wine/x86_64-unix").getAbsolutePath()
                +":"+new File(wineDir,"lib").getAbsolutePath());
            process=pb.start();
            if(!process.waitFor(9,TimeUnit.SECONDS)){
                process.destroyForcibly();
                return "Wine --version excedeu 9s. Processo encerrado; dependências precisam de revisão.";
            }
            return "Wine Bionic via Box64 terminou com código "+process.exitValue()
                +"\n"+limited(process.getInputStream())
                +"\nNão é teste de prefixo, display, áudio ou DXVK.";
        }catch(Exception error){
            return "Wine/Box64 Bionic não inicializou: "
                +error.getClass().getSimpleName()+": "+error.getMessage();
        }finally{
            if(process!=null && process.isAlive())process.destroyForcibly();
        }
    }

    private static String limited(InputStream stream) throws IOException {
        try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buf=new byte[512];
            int n;
            while(out.size()<3000 && (n=in.read(buf,0,
                Math.min(buf.length,3000-out.size())))!=-1)
                out.write(buf,0,n);
            return out.toString("UTF-8");
        }
    }
}
