import dev.velowin.app.runtime.ComponentRelease;
import dev.velowin.app.runtime.ComponentReleasePolicy;
public final class ComponentReleasePolicyTest {
    static void ok(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        ComponentRelease dxvk=new ComponentRelease(ComponentRelease.Kind.DXVK,
            "v3.1.1","dxvk-3.1.1.tar.gz",
            "https://github.com/doitsujin/dxvk/releases/download/v3.1.1/dxvk-3.1.1.tar.gz",
            "40565b4a724aadc4433fa4e010b4b23916d9b1f1baeee64e17186db94f54e608",18041512);
        ok(ComponentReleasePolicy.valid(dxvk),"DXVK public release");
        ComponentRelease vkd3d=new ComponentRelease(ComponentRelease.Kind.VKD3D_PROTON,
            "v3.0.1","vkd3d-proton-3.0.1.tar.zst",
            "https://github.com/HansKristian-Work/vkd3d-proton/releases/download/v3.0.1/vkd3d-proton-3.0.1.tar.zst",
            "3cf2315522af5e43605ef6d3c41dad91387040bf97199934f3f7ab76caaa2f0c",5163266);
        ok(ComponentReleasePolicy.valid(vkd3d),"VKD3D public release");
        ComponentRelease tx=new ComponentRelease(ComponentRelease.Kind.TERMUX_X11_APK,
            "nightly","termux-x11-universal-debug.apk",
            "https://github.com/termux/termux-x11/releases/download/nightly/termux-x11-universal-debug.apk",
            "64995746d1887a0e70bc8977d4a9c5ea56e3e00eadd4a6eeab32ae6048ba733a",14693944);
        ok(ComponentReleasePolicy.valid(tx),"Termux APK official release");
        ok(!ComponentReleasePolicy.valid(new ComponentRelease(dxvk.kind,dxvk.tag,dxvk.name,
            "https://evil.example/dxvk.tar.gz",dxvk.sha256,dxvk.size)),
            "untrusted host rejected");
        ok(!ComponentReleasePolicy.valid(new ComponentRelease(vkd3d.kind,vkd3d.tag,
            "../escape.tar.zst",vkd3d.url,vkd3d.sha256,vkd3d.size)),
            "path traversal rejected");
        ok(!ComponentReleasePolicy.valid(new ComponentRelease(dxvk.kind,dxvk.tag,dxvk.name,
            dxvk.url,"f".repeat(63),dxvk.size)),"weak digest rejected");
        ok(!ComponentReleasePolicy.valid(new ComponentRelease(dxvk.kind,dxvk.tag,dxvk.name,
            dxvk.url,dxvk.sha256,1024L*1024*1024)),"huge download refused");
        ok(ComponentReleasePolicy.digest("sha256:"+dxvk.sha256).equals(dxvk.sha256),
            "official digest normalized");
        ok(ComponentReleasePolicy.digest("SHA256:"+dxvk.sha256)==null,
            "noncanonical algorithm rejected");
        ok(ComponentReleasePolicy.canonical(dxvk.kind,"v3.1.1",
            "dxvk-native-3.1.1-steamrt-sniper.tar.gz")==null,
            "reject Linux native binary for Wine Bionic");
        System.out.println("ComponentReleasePolicyTest PASS");
    }
}
