import dev.velowin.app.engine.SmartProfile;
import static dev.velowin.app.engine.SmartProfile.Tier;
public class SmartProfileTest {
    static void check(boolean value) { if(!value) throw new AssertionError(); }
    public static void main(String[] args) {
        SmartProfile mali=SmartProfile.recommend("Mali-G68",1,2,6L<<30,"x86_64",Tier.BALANCED);
        check("SYSTEM".equals(mali.driver));
        check("DXVK_CANDIDATE".equals(mali.direct3d));
        check(mali.note.contains("Mali"));
        SmartProfile old=SmartProfile.recommend("Adreno 610",1,0,3L<<30,"x86",Tier.PERFORMANCE);
        check(old.tier==Tier.BALANCED && "WINED3D".equals(old.direct3d));
        SmartProfile dx12=SmartProfile.recommend("Adreno 750",1,3,12L<<30,"x86_64",Tier.PERFORMANCE);
        check("VKD3D_CANDIDATE".equals(dx12.direct3d));
        check("SYSTEM".equals(dx12.driver));
        SmartProfile arm=SmartProfile.recommend("Mali-G78",1,3,8L<<30,"arm64",Tier.PERFORMANCE);
        check(!arm.direct3d.equals("VKD3D_CANDIDATE"));
        check(arm.translator.startsWith("ARM64EC"));
        System.out.println("SmartProfileTest PASS");
    }
}
