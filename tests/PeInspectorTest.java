import dev.velowin.app.library.PeInspector;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class PeInspectorTest {
    static void ok(boolean b, String reason) { if (!b) throw new AssertionError(reason); }
    static byte[] make(int machine) {
        byte[] exe = new byte[320];
        exe[0]='M'; exe[1]='Z';
        exe[0x3c]=(byte)0x80;
        exe[0x80]='P';exe[0x81]='E';exe[0x82]=0;exe[0x83]=0;
        exe[0x84]=(byte)(machine & 255);exe[0x85]=(byte)(machine >>> 8);
        return exe;
    }
    static boolean fails(byte[] bytes) {
        try { PeInspector.parse(bytes); return false; }
        catch (IOException expected) { return true; }
    }
    public static void main(String[] args) throws Exception {
        ok("x86_64".equals(PeInspector.parse(make(0x8664))), "PE x64");
        ok("x86".equals(PeInspector.parse(make(0x14c))), "PE x86");
        ok("arm64".equals(PeInspector.parse(make(0xaa64))), "PE ARM64 detected");
        ok("x86_64".equals(PeInspector.inspect(new ByteArrayInputStream(make(0x8664)))), "streaming");
        byte[] bad = make(0x8664); bad[0x81]='x';
        ok(fails(bad), "spoof signature");
        bad=make(0x8664);bad[0x3f]=(byte)0xff;bad[0x3e]=(byte)0xff;
        ok(fails(bad), "large offset");
        ok(fails(new byte[5]), "truncated");
        ok(fails(make(0x1234)), "unsupported machine");
        System.out.println("PeInspectorTest: PASS");
    }
}
