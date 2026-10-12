import dev.velowin.app.engine.EngineReadiness;
public final class EngineReadinessTest {
    static void ok(boolean b,String reason){if(!b)throw new AssertionError(reason);}
    public static void main(String[] args){
        EngineReadiness none=new EngineReadiness(true,false,false,false,false,false,false,true,false);
        ok(!none.canLaunchGame(),"incomplete cannot launch");
        ok(none.missing().contains("Wine Bionic instalado e validado"),"Wine gate");
        EngineReadiness gpuOnly=new EngineReadiness(true,false,false,false,false,true,true,true,false);
        ok(!gpuOnly.canLaunchGame(),"native Vulkan and Android audio do not mean a Windows engine");
        ok(new EngineReadiness(true,true,true,true,true,true,true,true,true).canLaunchGame(),
            "all prerequisite flags");
        System.out.println("EngineReadinessTest PASS");
    }
}
