import dev.velowin.app.engine.PrefixLinkPolicy;

public class PrefixLinkPolicyTest {
    static void check(boolean p,String text){if(!p)throw new AssertionError(text);}
    public static void main(String[] args) {
        check(PrefixLinkPolicy.decide(".wine/dosdevices/c:","../drive_c")
            ==PrefixLinkPolicy.Decision.KEEP_RELATIVE,"C: remains sandboxed");
        check(PrefixLinkPolicy.decide(".wine/dosdevices/z:","/")
            ==PrefixLinkPolicy.Decision.SKIP_HOST_ROOT_Z,"Z: host root omitted");
        check(PrefixLinkPolicy.decide(".wine/dosdevices/c:","/")
            ==PrefixLinkPolicy.Decision.REJECT,"absolute host mount rejected");
        check(PrefixLinkPolicy.decide(".wine/dosdevices/z:","/etc")
            ==PrefixLinkPolicy.Decision.REJECT,"Z: not arbitrary absolute");
        check(PrefixLinkPolicy.decide(".wine/dosdevices/z:","../../../../")
            ==PrefixLinkPolicy.Decision.REJECT,"traversal link rejected");
        check(PrefixLinkPolicy.decide("bad/z:","/")
            ==PrefixLinkPolicy.Decision.REJECT,"other link unchanged");
        System.out.println("PrefixLinkPolicyTest PASS");
    }
}
