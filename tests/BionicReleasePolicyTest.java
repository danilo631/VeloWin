import dev.velowin.app.runtime.BionicReleasePolicy;
public final class BionicReleasePolicyTest {
    static void ok(boolean b,String reason){if(!b)throw new AssertionError(reason);}
    public static void main(String[] args){
        String tag="proton-11.0-2-20261010", name="proton-11.0-2-x86_64.wcp";
        String url="https://github.com/GameNative/proton-wine/releases/download/"+tag+"/"+name;
        String sha="sha256:056170e0bea7aead870f3e93036a6282921bad64e5e1206f1371d1d5840b46c7";
        ok(BionicReleasePolicy.valid(tag,name,url,sha,66352396),"real GitHub Bionic asset");
        ok(BionicReleasePolicy.url(tag,"proton-11.0-2-arm64ec.wcp")!=null,"arm64ec");
        ok(!BionicReleasePolicy.valid(tag,name,url,"sha256:bad",66352396),"reject missing SHA");
        ok(BionicReleasePolicy.url("../bad",name)==null,"reject traversal");
        ok(BionicReleasePolicy.url(tag,name+".xz")==null,"reject alternate package");
        ok(BionicReleasePolicy.url(tag,"proton-11.0-2-x86_64.wcp/../evil")==null,"reject escape");
        System.out.println("BionicReleasePolicyTest PASS");
    }
}
