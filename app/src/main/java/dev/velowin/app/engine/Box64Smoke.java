package dev.velowin.app.engine;

import android.content.Context;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/** Opt-in bounded -v probe of the pinned Bionic ARM64 Box64 binary only. */
public final class Box64Smoke {
    private Box64Smoke() {}
    private static File executable(Context context) {
        return new File(context.getApplicationInfo().nativeLibraryDir, "libvelobox64.so");
    }
    public static boolean bundled(Context context) {
        File file=executable(context);
        return file.isFile() && file.canExecute() && file.length()>1024*1024;
    }
    public static String inspect(Context context) {
        if (!bundled(context)) return "Box64 Bionic não incluído neste APK. "
            +"Instale a versão experimental gerada no GitHub Actions.";
        try {
            File work=BionicWorkspace.prepare(context);
            BoundedProcess.Result result=BoundedProcess.run(
                Arrays.asList(executable(context).getAbsolutePath(),"-v"),work,
                Box64Tuning.environment(Box64Tuning.Preset.BALANCED),6000,
                new AtomicBoolean(false));
            return "Box64 Bionic -v\n"+result.summary()
                +"\nDiagnóstico: "+FailureClassifier.explain(result.output,result.code,result.timeout)
                +"\nEste teste não inicia Wine ou um jogo.";
        } catch (Exception failure) {
            return "Falha no autoteste Box64: "+failure.getClass().getSimpleName()
                +": "+failure.getMessage();
        }
    }
}
