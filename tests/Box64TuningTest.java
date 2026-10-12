import dev.velowin.app.engine.Box64Tuning;
import java.util.Map;
public class Box64TuningTest {
    static void ok(boolean c,String m){if(!c)throw new AssertionError(m);}
    public static void main(String[]args){
        Map<String,String> stable=Box64Tuning.environment(Box64Tuning.Preset.STABILITY);
        Map<String,String> balanced=Box64Tuning.environment(Box64Tuning.Preset.BALANCED);
        Map<String,String> fast=Box64Tuning.environment(Box64Tuning.Preset.PERFORMANCE);
        ok("2".equals(stable.get("BOX64_DYNAREC_SAFEFLAGS")),"safe flags");
        ok("0".equals(stable.get("BOX64_DYNAREC_BIGBLOCK")),"avoid big blocks");
        ok("2".equals(balanced.get("BOX64_DYNAREC_BIGBLOCK")),"upstream default");
        ok("3".equals(fast.get("BOX64_DYNAREC_BIGBLOCK")),"performance");
        ok(Box64Tuning.fromGame("unknown")==Box64Tuning.Preset.BALANCED,"invalid -> safe default");
        boolean immutable=false;
        try {stable.put("BOX64_DYNAREC_SAFEFLAGS","0");}
        catch(UnsupportedOperationException ok){immutable=true;}
        ok(immutable,"immutable settings");
        System.out.println("Box64TuningTest PASS");
    }
}
