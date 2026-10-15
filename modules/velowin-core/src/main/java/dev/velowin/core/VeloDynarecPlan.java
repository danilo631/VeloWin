package dev.velowin.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Opt-in Box64 recipes: configuration semantics from Box64 USAGE.md,
 * Mobox's selectable preset concept; no Mobox/WinNative script copied.
 */
public final class VeloDynarecPlan {
    public static final String CONTAINER_KEY="velowin.hybrid.dynarec";
    private VeloDynarecPlan(){}
    public enum Mode {
        INHERIT("","Manter atual"),
        COMPATIBILITY("compatibility","Compatibilidade"),
        BALANCED("balanced","Equilibrado"),
        PERFORMANCE("performance","Desempenho");
        public final String key,label;
        Mode(String key,String label){this.key=key;this.label=label;}
    }
    public static Mode fromKey(String key){
        if(key==null)return Mode.INHERIT;
        for(Mode m:Mode.values())if(m.key.equals(key))return m;
        return Mode.INHERIT;
    }
    public static Map<String,String> variables(String mode){
        Mode m=fromKey(mode);
        if(m==Mode.INHERIT)return Collections.emptyMap();
        LinkedHashMap<String,String> vars=new LinkedHashMap<>();
        if(m==Mode.COMPATIBILITY){
            vars.put("BOX64_DYNAREC_BIGBLOCK","0");
            vars.put("BOX64_DYNAREC_SAFEFLAGS","2");
            vars.put("BOX64_DYNAREC_STRONGMEM","2");
        }else if(m==Mode.BALANCED){
            vars.put("BOX64_DYNAREC_BIGBLOCK","2");
            vars.put("BOX64_DYNAREC_SAFEFLAGS","1");
            vars.put("BOX64_DYNAREC_STRONGMEM","1");
        }else if(m==Mode.PERFORMANCE){
            vars.put("BOX64_DYNAREC_BIGBLOCK","3");
            vars.put("BOX64_DYNAREC_SAFEFLAGS","1");
            vars.put("BOX64_DYNAREC_STRONGMEM","0");
        }
        return Collections.unmodifiableMap(vars);
    }
}
