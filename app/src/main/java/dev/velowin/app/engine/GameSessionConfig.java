package dev.velowin.app.engine;

/** Values saved per title. These are preferences, not active graphics/latency controls. */
public final class GameSessionConfig {
    public enum Graphics { WINED3D, DXVK, VKD3D }
    private static final String[] RESOLUTIONS={
        "960x540","1280x720","1600x900","1920x1080"
    };
    private GameSessionConfig() {}
    public static boolean supportedResolution(String value) {
        if(value==null)return false;
        for(String candidate:RESOLUTIONS)
            if(candidate.equals(value))return true;
        return false;
    }
    public static String resolution(String value) {
        return supportedResolution(value)?value:"1280x720";
    }
    public static int fps(int value) {
        if(value==0 || value==30 || value==45 || value==60 || value==90 || value==120)
            return value;
        return 60;
    }
    public static Graphics graphics(String value) {
        if("DXVK".equals(value))return Graphics.DXVK;
        if("VKD3D".equals(value))return Graphics.VKD3D;
        return Graphics.WINED3D;
    }
    public static String recommendation(long ramBytes,boolean mali,boolean hot) {
        if(hot || (ramBytes>0&&ramBytes<5L*1024*1024*1024))return "960x540";
        if(mali)return "1280x720";
        return "1600x900";
    }
    public static String describe(Graphics graphics,boolean available) {
        switch(graphics){
            case DXVK: return "Direct3D 8/9/10/11 → DXVK"+(available?" instalado":" não integrado");
            case VKD3D: return "Direct3D 12 → VKD3D-Proton"+(available?" instalado":" não integrado");
            default: return "WineD3D (OpenGL); bridge de janela ainda necessário";
        }
    }
}
