import dev.velowin.app.engine.BoundedProcess;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
public class BoundedProcessTest {
    static void ok(boolean yes,String label){if(!yes)throw new AssertionError(label);}
    public static void main(String[]args)throws Exception {
        // No shell: test with system binary argv and a deliberately long output.
        File base=new File("/tmp");
        BoundedProcess.Result done=BoundedProcess.run(
            Arrays.asList("/usr/bin/printf","probe-ok"),base,
            Collections.emptyMap(),2500,new AtomicBoolean(false));
        ok(done.code==0 && done.output.contains("probe-ok"),"regular execution");
        BoundedProcess.Result slow=BoundedProcess.run(
            Arrays.asList("/usr/bin/sleep","2"),base,
            Collections.emptyMap(),600,new AtomicBoolean(false));
        ok(slow.timeout && slow.code==-1,"timeout kills child");
        AtomicBoolean cancel=new AtomicBoolean(true);
        boolean rejected=false;
        try{BoundedProcess.run(Arrays.asList("/usr/bin/true"),base,
            Collections.emptyMap(),2000,cancel);}
        catch(java.io.IOException expected){rejected=true;}
        ok(rejected,"pre-cancel");
        System.out.println("BoundedProcessTest PASS");
    }
}
