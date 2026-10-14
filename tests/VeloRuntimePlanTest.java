import com.winlator.cmod.velowin.VeloRuntimePlan;
import com.winlator.cmod.velowin.VeloRuntimePlan.Mode;
import com.winlator.cmod.velowin.VeloRuntimePlan.Capabilities;

public final class VeloRuntimePlanTest {
    private static void check(boolean yes,String msg){if(!yes)throw new AssertionError(msg);}
    public static void main(String[] args){
        Capabilities basic=new Capabilities(true,true,false,false,false,true);
        check(VeloRuntimePlan.inspect(Mode.BIONIC_BOX64,"x86_64",false,basic).available,
            "Bionic x86_64 Box64 permitted when verified");
        check(!VeloRuntimePlan.inspect(Mode.BIONIC_ARM64EC_FEX,"x86_64",false,basic).available,
            "FEX blocked for wrong wine architecture");
        check(!VeloRuntimePlan.inspect(Mode.BIONIC_BOX64,"arm64ec",false,basic).available,
            "Box64 cannot wrap native ARM64EC Wine");
        Capabilities mixed=new Capabilities(true,true,true,true,true,false);
        check(VeloRuntimePlan.inspect(Mode.BIONIC_ARM64EC_FEX,"arm64ec",false,mixed).available,
            "FEX is valid for verified ARM64EC");
        check(!VeloRuntimePlan.inspect(Mode.BIONIC_ARM64EC_FEX,"x86_64",false,mixed).available,
            "FEX must not silently change existing x86_64 prefix");
        check(VeloRuntimePlan.inspect(Mode.GLIBC_PROOT_GAMESCOPE,"x86_64",true,mixed).available,
            "Linux Gamescope already installed is a real alternate runtime");
        check(!VeloRuntimePlan.inspect(Mode.GLIBC_PROOT_GAMESCOPE,"x86_64",false,mixed).available,
            "Cannot reinterpret Wine container as Linux");
        check(!VeloRuntimePlan.inspect(Mode.GLIBC_PROOT_GAMESCOPE,"x86_64",true,
            new Capabilities(true,true,true,true,true,true)).available,
            "No automatic Turnip-only Linux profile for Mali");
        check(!VeloRuntimePlan.inspect(Mode.EXTERNAL_GLIBC,"x86_64",false,mixed).available,
            "External rootfs unavailable without implementation");
        check(!VeloRuntimePlan.inspect(Mode.BIONIC_BOX64,"x86_64",false,
            new Capabilities(true,false,false,false,false,false)).available,
            "Missing Box64 rejects launch");
        check(!VeloRuntimePlan.inspect(Mode.BIONIC_BOX64,"x86_64",false,
            new Capabilities(false,true,false,false,false,false)).available,
            "Missing ImageFS rejects launch");
        System.out.println("VeloRuntimePlanTest PASS");
    }
}
