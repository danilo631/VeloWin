package com.winlator.cmod.velowin;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import com.winlator.cmod.runtime.container.Container;
import dev.velowin.core.VeloUpscalePlan;

/** Dedicated bridge from original VeloWin controls to licensed Vulkan SGSR1.
 * Session-local decisions do not mutate the guest Wineprefix or driver.
 */
public final class VeloHybridGraphics {
    private VeloHybridGraphics(){}
    public static void select(Container c,VeloUpscalePlan.Mode mode){
        if(c==null||mode==null)throw new IllegalArgumentException("Contêiner/perfil vazio");
        c.putExtra(VeloUpscalePlan.CONTAINER_KEY,
            mode==VeloUpscalePlan.Mode.INHERIT?null:mode.key);
    }
    public static VeloUpscalePlan.Mode selected(Container c){
        return c==null?VeloUpscalePlan.Mode.INHERIT:
            VeloUpscalePlan.fromKey(c.getExtra(VeloUpscalePlan.CONTAINER_KEY,""));
    }
    public static VeloUpscalePlan.Settings forLaunch(Context ctx,Container c){
        int thermal=0;
        if(Build.VERSION.SDK_INT>=29){
            try{
                PowerManager power=(PowerManager)ctx.getSystemService(Context.POWER_SERVICE);
                if(power!=null)thermal=power.getCurrentThermalStatus();
                else thermal=4;
            }catch(RuntimeException error){thermal=4;}
        }
        return VeloUpscalePlan.resolve(
            c==null?"":c.getExtra(VeloUpscalePlan.CONTAINER_KEY,""),thermal);
    }
}
