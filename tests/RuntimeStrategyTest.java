import dev.velowin.app.engine.RuntimeStrategy;
import static dev.velowin.app.engine.RuntimeStrategy.*;
public final class RuntimeStrategyTest {
    private static void ok(boolean a, String b) { if (!a) throw new AssertionError(b); }
    public static void main(String[] argv) {
        Selection x=RuntimeStrategy.recommend("x86_64", false);
        ok(x.host==Host.BIONIC && x.translator==Translator.BOX64, "Bionic x64");
        Selection a=RuntimeStrategy.recommend("arm64", true);
        ok(a.wine==WineArch.ARM64EC, "opt-in arm64ec");
        ok(!RuntimeStrategy.compatible(Host.GLIBC,Host.BIONIC), "reject mixed ABI");
        ok(RuntimeStrategy.compatible(Host.BIONIC,Host.BIONIC), "allow same ABI");
        System.out.println("RuntimeStrategyTest PASS");
    }
}
