package com.winlator.cmod.velowin;

import com.winlator.cmod.runtime.container.Container;
import com.winlator.cmod.runtime.wine.EnvVars;
import dev.velowin.core.VeloDynarecPlan;
import java.util.Map;
import java.util.Locale;

/** Only changes Box64 guest variables in explicitly opted-in X11/Bionic
 * sessions. Preserves FEX, external PRoot and user defaults by default.
 * Values are sourced from documented ptitSeb/box64 supported settings.
 */
public final class VeloBox64Bridge {
    private VeloBox64Bridge(){}
    public static void select(Container c,VeloDynarecPlan.Mode mode){
        if(c==null||mode==null)throw new IllegalArgumentException("Perfil vazio");
        c.putExtra(VeloDynarecPlan.CONTAINER_KEY,
            mode==VeloDynarecPlan.Mode.INHERIT?null:mode.key);
    }
    public static VeloDynarecPlan.Mode selected(Container c){
        return c==null?VeloDynarecPlan.Mode.INHERIT:
            VeloDynarecPlan.fromKey(c.getExtra(VeloDynarecPlan.CONTAINER_KEY,""));
    }
    public static void apply(Container c,EnvVars env){
        if(c==null||env==null||c.isGamescopeRuntime())return;
        String engine=c.getEmulator64();
        if(engine==null||!engine.toLowerCase(Locale.ROOT).contains("box64"))return;
        for(Map.Entry<String,String> e:VeloDynarecPlan.variables(
                c.getExtra(VeloDynarecPlan.CONTAINER_KEY,"")).entrySet()){
            env.put(e.getKey(),e.getValue());
        }
    }
}
