import dev.velowin.app.engine.BridgeProfile;
import java.util.Map;

public class BridgeProfileTest {
    private static void ok(boolean condition,String message){
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args) {
        BridgeProfile disabled=BridgeProfile.disabled();
        ok(disabled.environment().isEmpty(),"disabled environment");
        ok(!disabled.x11Enabled(),"disabled X11");
        BridgeProfile x=BridgeProfile.of(0,true);
        Map<String,String> env=x.environment();
        ok("127.0.0.1:0".equals(env.get("DISPLAY")),"loopback display");
        ok("tcp:127.0.0.1:4713".equals(env.get("PULSE_SERVER")),"local Pulse");
        ok(x.x11Port()==6000,"display port 6000");
        ok(BridgeProfile.of(9,false).x11Port()==6009,"display port 6009");
        for(int p:new int[]{-100,-2,10,6000}){
            boolean rejected=false;
            try{BridgeProfile.of(p,true);}catch(IllegalArgumentException e){rejected=true;}
            ok(rejected,"reject arbitrary display "+p);
        }
        ok(!BridgeProfile.of(-1,true).environment().containsKey("DISPLAY"),
            "no DISPLAY without X11");
        System.out.println("BridgeProfileTest PASS");
    }
}
