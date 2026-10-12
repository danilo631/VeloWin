import dev.velowin.app.runtime.ReleasePolicy;

public final class ReleasePolicyTest {
    static void ok(boolean actual, String name) {
        if (!actual) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        String proton = "wine-proton-11.0-2-amd64-wow64.tar.xz";
        String staging = "wine-11.19-staging-amd64-wow64.tar.xz";
        String hash = "f166f7daa1a37b3c8e0ade213a6e7915e582091804071a58a4a32c6c672e1595";
        ok(ReleasePolicy.supported("proton-11.0-2", proton), "official proton build");
        ok(ReleasePolicy.supported("11.19", staging), "staging build");
        ok(!ReleasePolicy.supported("proton-exp-11.0", proton), "experimental tag");
        ok(!ReleasePolicy.supported("proton-11.0-2", "proton-wine-11.0-2-arm64ec.wcp.xz"), "bionic excluded");
        ok(!ReleasePolicy.supported("../proton-11.0-2", proton), "unsafe tag");
        ok(!ReleasePolicy.supported("proton-11.0-2", "../" + proton), "unsafe filename");
        ok(ReleasePolicy.expectedUrl("proton-11.0-2", proton).equals(
            "https://github.com/Kron4ek/Wine-Builds/releases/download/proton-11.0-2/" + proton), "canonical url");
        ok(ReleasePolicy.checksum("\n" + hash + "  " + proton + "\n", proton).equals(hash), "correct hash");
        ok(ReleasePolicy.checksum("fake " + proton, proton) == null, "missing hash rejected");
        ok(ReleasePolicy.trustedRedirect("https://release-assets.githubusercontent.com/something"), "github CDN");
        ok(!ReleasePolicy.trustedRedirect("https://release-assets.githubusercontent.com.evil.invalid/file"), "domain spoof");
        ok(!ReleasePolicy.trustedRedirect("http://release-assets.githubusercontent.com/file"), "plain HTTP");
        ok(!ReleasePolicy.trustedRedirect("https://bad@release-assets.githubusercontent.com/file"), "userinfo");
        ok(ReleasePolicy.validSize(80_000_000), "normal size");
        ok(!ReleasePolicy.validSize(ReleasePolicy.MAX_BYTES + 1), "huge size");
        System.out.println("ReleasePolicyTest: PASS");
    }
}
