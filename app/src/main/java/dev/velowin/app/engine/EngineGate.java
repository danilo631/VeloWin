package dev.velowin.app.engine;

import android.content.Context;
import android.os.Build;
import android.content.pm.PackageManager;
import dev.velowin.app.library.Game;
import dev.velowin.app.runtime.RuntimeStore;
import dev.velowin.app.runtime.WinePackageInstaller;
import dev.velowin.app.runtime.BionicArchiveStore;
import dev.velowin.app.runtime.BionicWineInstaller;
import dev.velowin.app.runtime.BionicRelease;

/**
 * Fail-closed execution gate: downloaded wine / a working Vulkan GPU does not
 * mean a runnable Windows session. JNI/GLES/AudioTrack are host-side probes.
 */
public final class EngineGate {
    private EngineGate() {}

    public static String reason(Context context, Game game) {
        if(game==null)return "Adicione um jogo para configurar os componentes.";
        int bionic=0, glibc=0;
        try{
            for(BionicRelease release:BionicArchiveStore.saved(context))
                if(BionicWineInstaller.installed(context,release))bionic++;
            for(RuntimeStore.Installed release:new RuntimeStore(context).all())
                if(WinePackageInstaller.installed(context,release))glibc++;
        }catch(Exception ignored){}
        boolean arm64=false;
        for(String abi:Build.SUPPORTED_ABIS)if("arm64-v8a".equals(abi))arm64=true;
        boolean hasVulkan=context.getPackageManager()
            .hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL);
        EngineReadiness state=new EngineReadiness(arm64,
            Box64Smoke.bundled(context), bionic>0,
            false, /* Wineprefix not initialized */
            false, /* Wine window system absent */
            false, /* Wine audio bridge absent */
            false, /* Wine controller bridge absent */
            hasVulkan, false /* DXVK missing */);
        return "Motor Windows ainda não integrado.\n"
            +"Wine Bionic preparado: "+bionic+" / Wine glibc preparado: "+glibc
            +"\nBox64 experimental: "+(Box64Smoke.bundled(context)?"incluído neste APK":"não incluído")
            +"\n\n"+state.report()
            +"\n\nO Wine Linux/glibc não pode ser usado na pilha Android/Bionic.";
    }

    public static boolean isReady(Context context) {
        return false;
    }
}
