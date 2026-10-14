import com.winlator.cmod.velowin.VeloBootTarget;
public final class VeloBootTargetTest {
    private static void check(boolean condition,String label){
        if(!condition)throw new AssertionError(label);
    }
    public static void main(String[] args){
        check(VeloBootTarget.WFM.equals(VeloBootTarget.select(true,true,false)),
              "healthy Winlator file manager");
        check(VeloBootTarget.WINEFILE.equals(VeloBootTarget.select(false,true,false)),
              "wfm missing, builtin Winefile fallback");
        check(VeloBootTarget.WINEFILE.equals(VeloBootTarget.select(true,true,true)),
              "user forced recovery Winefile");
        check(VeloBootTarget.EXPLORER.equals(VeloBootTarget.select(false,false,false)),
              "missing handler use Wine builtin explorer, never invisible wrapper");
        check(VeloBootTarget.EXPLORER.equals(VeloBootTarget.select(true,false,true)),
              "forced recovery cannot execute missing handler");
        check(VeloBootTarget.isRecovery("1")&&!VeloBootTarget.isRecovery("0"),
              "recovery flag validation");
        System.out.println("VeloBootTargetTest PASS");
    }
}
