package com.winlator.cmod.velowin;

import android.content.Context;
import com.winlator.cmod.runtime.container.Container;
import com.winlator.cmod.runtime.container.ContainerManager;
import com.winlator.cmod.runtime.content.ContentProfile;
import com.winlator.cmod.runtime.content.ContentsManager;
import com.winlator.cmod.runtime.system.SessionKeepAliveService;
import java.util.Locale;

/** Original per-container runtime reference protection:
 * informed by Winlator-Ludashi's WineRuntimeGuard, implemented against
 * the existing WinNative-derived Container API without importing its code.
 */
public final class VeloInstalledRuntimeGuard {
    private VeloInstalledRuntimeGuard(){}
    public static String removalBlocker(Context context, ContentProfile profile){
        if(context==null||profile==null||profile.type==null)
            return "Runtime ou contexto inválido";
        if(SessionKeepAliveService.isSessionActive())
            return "Há uma sessão Wine ativa: encerre antes de remover componentes";
        String id=ContentsManager.getEntryName(profile);
        String version=profile.verName+"-"+profile.verCode;
        for(Container c:new ContainerManager(context).getContainers()){
            if(c==null)continue;
            String selected=null;
            switch(profile.type){
                case CONTENT_TYPE_WINE:
                case CONTENT_TYPE_PROTON:
                    selected=c.getWineVersion();
                    break;
                case CONTENT_TYPE_BOX64:
                case CONTENT_TYPE_WOWBOX64:
                    String emu=(c.getEmulator()+","+c.getEmulator64())
                        .toLowerCase(Locale.ROOT);
                    if(emu.contains("box64"))selected=c.getBox64Version();
                    break;
                case CONTENT_TYPE_FEXCORE:
                    if((c.getEmulator()+","+c.getEmulator64())
                        .toLowerCase(Locale.ROOT).contains("fex"))
                        selected=c.getFEXCoreVersion();
                    break;
                default:
                    return null;
            }
            if(selected!=null && (selected.equals(id)
                    || selected.equals(version) || selected.equals(profile.verName)))
                return "Contêiner "+c.getName()+" utiliza "+id;
        }
        return null;
    }
}
