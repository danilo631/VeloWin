import dev.velowin.app.engine.BionicSessionPlan;
import static dev.velowin.app.engine.BionicSessionPlan.Translator.*;
import static dev.velowin.app.engine.BionicSessionPlan.Graphics.*;

public class BionicSessionPlanTest {
    static void check(boolean condition, String name) {
        if(!condition) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        BionicSessionPlan blocked=BionicSessionPlan.evaluate(BOX64,DXVK,"ARM64EC","x86_64",
            true,true,true,true,false,false,false,false,false,false);
        check(!blocked.readyForNativeLaunch(),"incomplete must block");
        check(blocked.diagnostic().contains("ARM64EC exige FEX"),"ABI mismatch");
        check(blocked.diagnostic().contains("janelas"),"display required");
        BionicSessionPlan vk=BionicSessionPlan.evaluate(FEX,VKD3D,"ARM64EC","x86_64",
            true,false,true,true,true,true,true,true,true,true);
        check(!vk.readyForNativeLaunch(),"vulkan 1.3 required");
        BionicSessionPlan ready=BionicSessionPlan.evaluate(BOX64,OPENGL,"X86_64","x86",
            false,false,true,true,true,true,true,true,false,false);
        check(ready.readyForNativeLaunch(),"explicitly ready OpenGL profile");
        check(!ready.diagnostic().contains("bloqueada"),"valid diagnostic");
        System.out.println("BionicSessionPlanTest: PASS");
    }
}
