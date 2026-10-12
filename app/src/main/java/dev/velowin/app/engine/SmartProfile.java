package dev.velowin.app.engine;

import java.util.Locale;

/**
 * Game-first defaults inspired by automatic, per-title launcher UX.
 * The result is a recommendation, NOT proof that DXVK/Wine/Box64 can run.
 */
public final class SmartProfile {
    public enum Tier { COMPATIBILITY, BALANCED, PERFORMANCE }
    public final Tier tier;
    public final String translator, driver, direct3d, audio, input, note;

    private SmartProfile(Tier t, String cpu, String gpu, String d3d,
                         String sound, String controls, String message) {
        tier=t; translator=cpu; driver=gpu; direct3d=d3d;
        audio=sound; input=controls; note=message;
    }

    public static SmartProfile recommend(String gpu, int vulkanMajor, int vulkanMinor,
                                         long ramBytes, String pe, Tier preference) {
        String n=gpu==null ? "" : gpu.toLowerCase(Locale.ROOT);
        boolean adreno=n.contains("adreno");
        boolean mali=n.contains("mali")||n.contains("immortalis")||n.contains("xclipse");
        boolean hasVulkan=vulkanMajor>=1 && (vulkanMajor>1||vulkanMinor>=1);
        boolean vk13=vulkanMajor>1||(vulkanMajor==1&&vulkanMinor>=3);
        boolean lowRam=ramBytes>0 && ramBytes<5L*1024*1024*1024;
        Tier effective=lowRam && preference==Tier.PERFORMANCE?Tier.BALANCED:preference;
        String cpu="BOX64";
        String driver="SYSTEM";
        String d3d=hasVulkan?"DXVK_CANDIDATE":"WINED3D";
        if ("arm64".equals(pe)) {
            cpu="ARM64EC_REQUIRES_WINE";
            d3d="WINED3D";
        }
        if (effective==Tier.COMPATIBILITY) d3d="WINED3D";
        if (effective==Tier.PERFORMANCE && vk13 && "x86_64".equals(pe))
            d3d="VKD3D_CANDIDATE";
        // Turnip is only an *optional* Adreno profile, never selected without a driver.
        String advice=(mali?"Mali/Xclipse: usar Vulkan do sistema. ":
                       adreno?"Adreno: preferir sistema; Turnip só após instalar e validar. ":
                       "GPU desconhecida: manter driver do sistema. ")
            +(lowRam?"RAM limitada: recomendada configuração equilibrada. ":"")
            +"Seleção gráfica é apenas uma preferência; execução Windows depende de "
            +"Wine Bionic, Box64, display, áudio e extensões Vulkan verificadas.";
        return new SmartProfile(effective,cpu,driver,d3d,"ANDROID_AUDIOTRACK",
            "ANDROID_GAMEPAD_TOUCH",advice);
    }
}
