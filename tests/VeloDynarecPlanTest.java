import dev.velowin.core.VeloDynarecPlan;
import java.util.Map;
public class VeloDynarecPlanTest {
    static void yes(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[]args){
        yes(VeloDynarecPlan.variables("").isEmpty());
        yes(VeloDynarecPlan.variables("??").isEmpty());
        Map<String,String> c=VeloDynarecPlan.variables("compatibility");
        yes(c.get("BOX64_DYNAREC_SAFEFLAGS").equals("2"));
        yes(c.get("BOX64_DYNAREC_BIGBLOCK").equals("0"));
        yes(VeloDynarecPlan.variables("balanced").get("BOX64_DYNAREC_BIGBLOCK").equals("2"));
        yes(VeloDynarecPlan.variables("performance").get("BOX64_DYNAREC_BIGBLOCK").equals("3"));
        yes(!VeloDynarecPlan.variables("performance").containsKey("VK_ICD_FILENAMES"));
        try{c.put("BAD","1");throw new AssertionError();}catch(UnsupportedOperationException ok){}
        System.out.println("VeloDynarecPlanTest OK");
    }
}
