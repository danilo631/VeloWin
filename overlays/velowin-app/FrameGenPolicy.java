package com.winlator.cmod.velowin;

/**
 * VeloWin-owned policy for the open OpenFlow/DIS frame-generation path.
 * Source engine: WinNative's renderer, which contains the actual Vulkan
 * optical-flow + frame-pacing implementation. This class only selects a
 * profile; it deliberately never fakes GPU support or an active FPS boost.
 */
public final class FrameGenPolicy {
    private FrameGenPolicy() {}
    public static final String DIS="DIS", OFF="OFF";
    public static int scale(int value) {
        return Math.max(25,Math.min(100,value));
    }
    public static int targetFps(int fps) {
        if(fps==0||fps==30||fps==40||fps==45||fps==60||fps==90||fps==120)
            return fps;
        throw new IllegalArgumentException("FPS não suportado.");
    }
    public static boolean wantsDis(String mode) {
        return DIS.equals(mode);
    }
    public static boolean usesProprietaryShaders(String mode) {
        return "LSFG".equals(mode); // Only DIS is available in the VeloWin quick panel.
    }
}
