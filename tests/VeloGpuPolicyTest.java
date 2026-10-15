import dev.velowin.core.VeloGpuPolicy;

public final class VeloGpuPolicyTest {
    static void check(boolean condition,String reason){
        if(!condition)throw new AssertionError(reason);
    }
    public static void main(String[] a){
        check(VeloGpuPolicy.isMali("Mali-G68 MC4"),"mali");
        check(!VeloGpuPolicy.isMali("Adreno 740"),"adreno");
        check(VeloGpuPolicy.permitTemporal(1080,2340,120,0),"A34 size");
        check(!VeloGpuPolicy.permitTemporal(1080,2340,120,4),"thermal cutoff");
        check(!VeloGpuPolicy.permitTemporal(4000,3000,120,0),"memory cutoff");
        check(!VeloGpuPolicy.permitTemporal(720,1280,Float.NaN,0),"refresh unknown");
        check(VeloGpuPolicy.temporalImageBytes(100,200)==160000L,"RGBA8 buffers");
        System.out.println("VeloGpuPolicyTest PASS");
    }
}
