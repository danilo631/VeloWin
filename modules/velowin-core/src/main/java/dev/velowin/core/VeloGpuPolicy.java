package dev.velowin.core;

import java.util.Locale;

/**
 * Guard rails for Android GLES3 experiments; not a proof of GPU Vulkan,
 * driver extensions, compositor pacing or Wine game compatibility.
 */
public final class VeloGpuPolicy {
    private VeloGpuPolicy(){}
    public static boolean isMali(String renderer){
        return renderer!=null && renderer.toLowerCase(Locale.ROOT).contains("mali");
    }
    public static long temporalImageBytes(int width,int height){
        if(width<=0||height<=0)return 0;
        return 2L*width*height*4L; // two RGBA8 textures, excludes driver overhead.
    }
    /**
     * Thermal >= 4 (Android severe) disables experimental optical matching.
     * 4 Mpixel and refresh constraints avoid unbounded GL bandwidth.
     */
    public static boolean permitTemporal(int width,int height,float displayHz,int thermal){
        if(width<2||height<2||((long)width*height)>4_000_000L)return false;
        if(!Float.isFinite(displayHz)||displayHz<45f||displayHz>240f)return false;
        return thermal>=0 && thermal<4;
    }
}
