import dev.velowin.app.hardware.GpuDriverPolicy;
public final class GpuDriverPolicyTest {
    static void check(boolean b,String n){if(!b)throw new AssertionError(n);}
    public static void main(String[] args){
        String success="Vulkan logical device: criado (VkResult 0)\nVK_KHR_swapchain: sim";
        GpuDriverPolicy mali=GpuDriverPolicy.evaluate("Mali-G68 MC4","SYSTEM",success);
        check(mali.family==GpuDriverPolicy.Family.MALI,"Mali detection");
        check(mali.vulkanDeviceCreated,"native driver created");
        check(mali.actualGraphicsDriverReady,"android system driver");
        GpuDriverPolicy invalid=GpuDriverPolicy.evaluate("Mali-G68","TURNIP",success);
        check(!invalid.actualGraphicsDriverReady,"Turnip cannot run Mali");
        check(!invalid.turnipCompatibleHardware,"Mali Turnip absent");
        GpuDriverPolicy adreno=GpuDriverPolicy.evaluate("Adreno 740","TURNIP",success);
        check(adreno.turnipCompatibleHardware && !adreno.turnipPackaged,"Adreno Turnip not bundled");
        check(!adreno.actualGraphicsDriverReady,"must not falsely enable Turnip");
        check(!GpuDriverPolicy.evaluate("Adreno","SYSTEM","").vulkanDeviceCreated,
            "unverified must not be ready");
        check(GpuDriverPolicy.classify("Xclipse 920")==GpuDriverPolicy.Family.XCLIPSE,
            "Xclipse is not Adreno");
        check(GpuDriverPolicy.classify("Immortalis G715")==GpuDriverPolicy.Family.MALI,
            "Immortalis is Mali");
        System.out.println("GpuDriverPolicyTest PASS");
    }
}
