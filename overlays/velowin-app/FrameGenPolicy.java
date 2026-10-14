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
    /** Clamp a fixed target to actual panel refresh and current thermal headroom.
     * Adaptive target 0 remains adaptive in the real Vulkan DIS scheduler.
     * Android thermal levels match PowerManager's status values, but no
     * Android dependency is required here (unit-testable JVM policy).
     */
    public static int safeTarget(int requested,float panelHz,int thermalStatus) {
        int target=targetFps(requested);
        if(target==0)return 0;
        if(!Float.isFinite(panelHz)||panelHz<30.0f)return 0;
        int panel=(int)Math.floor(panelHz+0.25f);
        if(panel>240)panel=240;
        int max=panel;
        if(thermalStatus>=4)max=Math.min(max,30);
        else if(thermalStatus>=2)max=Math.min(max,60);
        return Math.min(target,max);
    }
    public static boolean wantsDis(String mode) {
        return DIS.equals(mode);
    }
    public static boolean usesProprietaryShaders(String mode) {
        return "LSFG".equals(mode); // Only DIS is available in the VeloWin quick panel.
    }
}
