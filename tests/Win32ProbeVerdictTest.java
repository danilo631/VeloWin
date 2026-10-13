import dev.velowin.app.engine.Win32ProbeVerdict;

public class Win32ProbeVerdictTest {
    static void ok(boolean c,String m){if(!c)throw new AssertionError(m);}
    public static void main(String[] args) {
        String marker=Win32ProbeVerdict.MARKER;
        ok(Win32ProbeVerdict.passed(0,false,false,"header\n"+marker+"\n"),"real success");
        ok(!Win32ProbeVerdict.passed(1,false,false,marker),"exit nonzero");
        ok(!Win32ProbeVerdict.passed(0,true,false,marker),"timeout");
        ok(!Win32ProbeVerdict.passed(0,false,true,marker),"cancelled");
        ok(!Win32ProbeVerdict.passed(0,false,false,"Wine error '"+marker+"'"),"embedded spoof");
        ok(!Win32ProbeVerdict.passed(0,false,false,"VELO_WIN32_SMOKE_OK_BUT_NOT"),"partial spoof");
        ok(!Win32ProbeVerdict.passed(0,false,false,null),"null output");
        System.out.println("Win32ProbeVerdictTest PASS");
    }
}
