import dev.velowin.core.VeloUpscalePlan;
public class VeloUpscalePlanTest {
    static void yes(boolean ok){if(!ok)throw new AssertionError();}
    public static void main(String[]args){
        VeloUpscalePlan.Settings old=VeloUpscalePlan.resolve("",0);
        yes(!old.override);
        yes(!VeloUpscalePlan.resolve("???",0).override);
        yes(VeloUpscalePlan.resolve("quality",0).enabled);
        yes(VeloUpscalePlan.resolve("quality",0).mode==3);
        yes(VeloUpscalePlan.resolve("balanced",0).mode==4);
        yes(VeloUpscalePlan.resolve("performance",0).mode==6);
        yes(!VeloUpscalePlan.resolve("performance",4).enabled);
        yes(VeloUpscalePlan.resolve("performance",4).thermalFallback);
        yes(!VeloUpscalePlan.resolve("native",0).enabled);
        yes(VeloUpscalePlan.estimatedGuestPixels(1920,1080,6)==960L*540L);
        yes(VeloUpscalePlan.estimatedGuestPixels(-1,1080,6)==0);
        System.out.println("VeloUpscalePlanTest OK");
    }
}
