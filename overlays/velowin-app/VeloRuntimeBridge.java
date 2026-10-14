package com.winlator.cmod.velowin;

import android.content.Context;
import com.winlator.cmod.runtime.container.Container;
import com.winlator.cmod.runtime.content.ContentProfile;
import com.winlator.cmod.runtime.content.ContentsManager;
import com.winlator.cmod.runtime.display.environment.ImageFs;
import com.winlator.cmod.runtime.linux.LinuxRuntime;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Sole upstream-facing runtime adapter of VeloWin v17.
 * It does not convert prefixes across Wine architectures or invent a
 * second PRoot Wine environment. All modes use verified installed files.
 */
public final class VeloRuntimeBridge {
    private VeloRuntimeBridge(){}
    public static VeloRuntimePlan.Capabilities probe(
            Context context,Container container,ContentsManager contents){
        File root=ImageFs.find(context).getRootDir();
        boolean image=ImageFs.find(context).isUpToDate();
        boolean box64=new File(root,"usr/bin/box64").isFile()
            ||new File(root,"usr/local/bin/box64").isFile();
        boolean fex=false;
        if(contents!=null){
            box64=box64||installed(context,contents,
                ContentProfile.ContentType.CONTENT_TYPE_BOX64);
            fex=installed(context,contents,
                ContentProfile.ContentType.CONTENT_TYPE_FEXCORE);
        }
        boolean linux=LinuxRuntime.isInstalled(context);
        boolean proot=LinuxRuntime.prootBinary(context).isFile()
            &&LinuxRuntime.prootLoader(context).isFile();
        return new VeloRuntimePlan.Capabilities(image,box64,fex,
            linux,proot,MaliProfile.isApplied(container));
    }
    private static boolean installed(Context context,ContentsManager contents,
                                     ContentProfile.ContentType type){
        List<ContentProfile> candidates=contents.getProfiles(type);
        if(candidates==null)return false;
        for(ContentProfile profile:candidates){
            if(ContentsManager.isInstalled(context,profile))return true;
        }
        return false;
    }
    public static List<VeloRuntimePlan.Decision> choices(
            Context context,Container c,ContentsManager contents){
        VeloRuntimePlan.Capabilities caps=probe(context,c,contents);
        String arch=c.getExtra("wineprefixArch","x86_64");
        boolean linux=c.isGamescopeRuntime();
        List<VeloRuntimePlan.Decision> result=new ArrayList<>();
        for(VeloRuntimePlan.Mode mode:VeloRuntimePlan.Mode.values()){
            result.add(VeloRuntimePlan.inspect(mode,arch,linux,caps));
        }
        return result;
    }
    public static void select(Container c,VeloRuntimePlan.Decision choice){
        if(c==null||choice==null||!choice.available)
            throw new IllegalStateException("Backend não verificado/indisponível");
        switch(choice.mode){
            case BIONIC_BOX64:
                if(c.isGamescopeRuntime()
                    ||"arm64ec".equalsIgnoreCase(c.getExtra("wineprefixArch","x86_64")))
                    throw new IllegalStateException("Prefixo incompatible com Box64 x86_64");
                c.setEmulator("box64");
                c.setEmulator64("box64");
                c.setDisplayBackend(Container.DISPLAY_BACKEND_X11);
                break;
            case BIONIC_ARM64EC_FEX:
                if(c.isGamescopeRuntime()
                    ||!"arm64ec".equalsIgnoreCase(c.getExtra("wineprefixArch","")))
                    throw new IllegalStateException("Instale Wine ARM64EC primeiro");
                c.setEmulator("fexcore");
                c.setEmulator64("fexcore");
                c.setDisplayBackend(Container.DISPLAY_BACKEND_X11);
                break;
            case GLIBC_PROOT_GAMESCOPE:
                // Engine Linux mode was already installed and selected
                // by the upstream Linux installer. Do not silently migrate
                // Wineprefix or inject Turnip on Mali.
                if(!c.isGamescopeRuntime())
                    throw new IllegalStateException("Crie contêiner Linux no painel avançado");
                break;
            default:
                throw new IllegalStateException("Backend glibc externo não foi validado");
        }
        c.saveData();
    }
}
