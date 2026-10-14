import com.winlator.cmod.velowin.FrameGenPolicy;

public final class FrameGenPolicyTest {
    private static void check(boolean yes,String msg) {
        if(!yes)throw new AssertionError(msg);
    }
    public static void main(String[] args){
        check(!FrameGenPolicy.wantsDis("OFF"),"default off");
        check(FrameGenPolicy.wantsDis("DIS"),"open-flow mode");
        check(!FrameGenPolicy.wantsDis("LSFG"),"no proprietary shader mode");
        check(!FrameGenPolicy.usesProprietaryShaders("DIS"),"open-source backend");
        check(FrameGenPolicy.usesProprietaryShaders("LSFG"),"external LSFG shaders are not silently bundled");
        check(FrameGenPolicy.scale(5)==25,"minimum scale");
        check(FrameGenPolicy.scale(500)==100,"maximum scale");
        check(FrameGenPolicy.targetFps(0)==0,"adaptive frame pacing");
        check(FrameGenPolicy.targetFps(60)==60,"60 fps");
        check(FrameGenPolicy.targetFps(90)==90,"90 fps");
        check(FrameGenPolicy.targetFps(120)==120,"120 fps");
        boolean bad=false;
        try{FrameGenPolicy.targetFps(333);}catch(IllegalArgumentException expected){bad=true;}
        check(bad,"reject invalid frame pacing");
        System.out.println("VeloWin FrameGenPolicyTest PASS");
    }
}
