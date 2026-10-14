package dev.velowin.app.engine;

import android.content.Context;
import dev.velowin.app.runtime.BionicRelease;
import dev.velowin.app.runtime.BionicWineInstaller;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Short manual smoke test: Box64 -> verified Bionic Wine --version.
 * No wineboot, prefix creation, shell, arbitrary EXE, X server or games.
 */
public final class WineVersionSmoke {
    private WineVersionSmoke() {}

    public static String run(Context context,BionicRelease release) {
        return run(context,release,Box64Tuning.Preset.BALANCED);
    }

    public static String run(Context context,BionicRelease release,Box64Tuning.Preset preset) {
        if(release==null||!"x86_64".equals(release.arch))
            return "Este diagnóstico requer Wine x86_64 Bionic; ARM64EC é outro backend.";
        if(!Box64Smoke.bundled(context))
            return "Box64 Bionic ausente. Use APK experimental do GitHub Actions.";
        if(!BionicWineInstaller.installed(context,release))
            return "Pacote Wine Bionic não instalado ou validação incompleta.";
        try {
            File workspace=BionicWorkspace.prepare(context);
            File wineRoot=BionicWineInstaller.folder(context,release).getCanonicalFile();
            File wine=new File(wineRoot,"bin/wine").getCanonicalFile();
            if(!wine.isFile()||!wine.getPath().startsWith(
                    wineRoot.getCanonicalPath()+File.separator))
                return "O binário Wine aponta para fora do diretório instalado.";
            File box64=new File(context.getApplicationInfo().nativeLibraryDir,
                "libvelobox64.so").getCanonicalFile();
            Map<String,String> env=WineLaunchEnvironment.build(wineRoot,workspace,
                null,preset,java.util.Collections.emptyMap(),false);
            BoundedProcess.Result result=BoundedProcess.run(
                Arrays.asList(box64.getAbsolutePath(),wine.getAbsolutePath(),"--version"),
                workspace,env,9000,new AtomicBoolean(false));
            return "Wine Bionic + Box64 --version (somente diagnóstico):\n"
                +result.summary()
                +"\n\nDiagnóstico: "+FailureClassifier.explain(result.output,result.code,result.timeout)
                +"\n\n"+Box64Tuning.explanation(preset)
                +"\nEste resultado não testa wineboot, DXVK, jogo nem som.";
        }catch(Exception error) {
            return "Wine/Box64 não inicializou: "+error.getClass().getSimpleName()
                +": "+error.getMessage();
        }
    }
}
