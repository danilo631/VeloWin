import dev.velowin.app.engine.WineRuntimeAudit;
import java.io.File;
public class WineRuntimeAuditTest {
    static void ok(boolean p,String m){ if(!p)throw new AssertionError(m); }
    public static void main(String[] args) {
        File missing=new File("/tmp/velowin-nonexistent-wcp");
        ok(!WineRuntimeAudit.loaderReady(missing),"missing loader fail closed");
        String report=WineRuntimeAudit.report(missing);
        ok(report.contains("AUSENTE") && report.contains("não executa Wine"),
            "report shows missing files and limits");
        System.out.println("WineRuntimeAuditTest PASS");
    }
}
