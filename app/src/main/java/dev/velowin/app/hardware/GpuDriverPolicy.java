package dev.velowin.app.hardware;

import java.util.Locale;

/**
 * Conservative GPU policy for the actual Android host, not a promise of
 * Windows DXVK/VKD3D compatibility. No system GPU driver is overwritten.
 */
public final class GpuDriverPolicy {
    public enum Family { MALI, ADRENO, XCLIPSE, POWERVR, OTHER }
    public enum Driver { SYSTEM, TURNIP }
    public final Family family;
    public final Driver requested;
    public final boolean vulkanDeviceCreated;
    public final boolean deviceReportsSwapchain;
    public final boolean turnipCompatibleHardware;
    public final boolean turnipPackaged;
    public final boolean actualGraphicsDriverReady;
    public final String explanation;

    private GpuDriverPolicy(Family family, Driver requested, boolean nativeVulkan,
                            boolean swapchain, boolean turnipPackage, String explanation) {
        this.family=family;
        this.requested=requested;
        this.vulkanDeviceCreated=nativeVulkan;
        this.deviceReportsSwapchain=swapchain;
        this.turnipCompatibleHardware=family==Family.ADRENO;
        this.turnipPackaged=turnipPackage;
        this.actualGraphicsDriverReady=requested==Driver.SYSTEM && nativeVulkan
            || requested==Driver.TURNIP && family==Family.ADRENO && turnipPackage
            && nativeVulkan;
        this.explanation=explanation;
    }

    public static Family classify(String renderer) {
        String text=renderer==null?"":renderer.toLowerCase(Locale.ROOT);
        if(text.contains("adreno"))return Family.ADRENO;
        if(text.contains("mali") || text.contains("immortalis"))return Family.MALI;
        if(text.contains("xclipse"))return Family.XCLIPSE;
        if(text.contains("powervr"))return Family.POWERVR;
        return Family.OTHER;
    }

    public static GpuDriverPolicy evaluate(String renderer, String requestedDriver,
                                           String nativeReport) {
        Family gpu=classify(renderer);
        Driver requested="TURNIP".equals(requestedDriver)?Driver.TURNIP:Driver.SYSTEM;
        boolean device=nativeReport!=null
            && nativeReport.contains("Vulkan logical device: criado (VkResult 0)");
        boolean swapchain=nativeReport!=null
            && nativeReport.contains("VK_KHR_swapchain: sim");
        // An installed system Vulkan driver is not an installed Turnip driver.
        // Turnip is not distributed or loaded by VeloWin in this release.
        boolean shippedTurnip=false;
        String explanation;
        if(requested==Driver.TURNIP) {
            explanation=gpu!=Family.ADRENO
                ?"Turnip é incompatível com esta família de GPU. Use o driver nativo Android."
                :"Turnip não está incluído e não foi validado. Use Vulkan do Android.";
        } else if(!device) {
            explanation="Driver Vulkan Android não passou pelo teste de criação de VkDevice. "
                +"Execute os laboratórios JNI/WSI para diagnosticar.";
        } else if(!swapchain) {
            explanation="VkDevice foi criado, mas VK_KHR_swapchain não foi anunciada. "
                +"Não habilitar renderização de jogos.";
        } else {
            explanation="Vulkan nativo Android detectado. Ainda faltam ponte X11, DXVK "
                +"e apresentação de frames para executar jogos.";
        }
        return new GpuDriverPolicy(gpu,requested,device,swapchain,shippedTurnip,explanation);
    }

    public String report() {
        return "GPU: "+family
            +"\nDriver selecionado: "+requested
            +"\nVkDevice Android criado: "+(vulkanDeviceCreated?"sim":"não/sem teste")
            +"\nVK_KHR_swapchain: "+(deviceReportsSwapchain?"anunciada":"não/sem teste")
            +"\nTurnip disponível no APK: "+(turnipPackaged?"sim":"não")
            +"\nDriver pedido aplicável: "+(actualGraphicsDriverReady?"parcialmente, no host":"não validado")
            +"\n"+explanation
            +"\nWine/DXVK/VKD3D: nenhuma compatibilidade confirmada por esta verificação.";
    }
}
