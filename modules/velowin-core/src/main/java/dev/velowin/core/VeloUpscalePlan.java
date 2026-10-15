package dev.velowin.core;

/** Android-independent policy for the real Vulkan SGSR1 renderer.
 * SGSR1 is from Qualcomm under BSD-3-Clause in the licensed engine.
 * This class only selects existing modes; it implements no vendor shader.
 */
public final class VeloUpscalePlan {
    public static final String CONTAINER_KEY="velowin.hybrid.sgsr";
    private VeloUpscalePlan(){}
    public enum Mode {
        INHERIT("",false,1,0), NATIVE("native",false,1,0),
        QUALITY("quality",true,3,60), BALANCED("balanced",true,4,55),
        PERFORMANCE("performance",true,6,40);
        public final String key;
        public final boolean enabled;
        public final int nativeMode,sharpness;
        Mode(String key,boolean enabled,int nativeMode,int sharpness){
            this.key=key;this.enabled=enabled;this.nativeMode=nativeMode;
            this.sharpness=sharpness;
        }
    }
    public static final class Settings {
        public final boolean override,enabled,thermalFallback;
        public final int mode,sharpness;
        Settings(boolean override,boolean enabled,int mode,int sharpness,boolean thermalFallback){
            this.override=override;this.enabled=enabled;this.mode=mode;
            this.sharpness=sharpness;this.thermalFallback=thermalFallback;
        }
    }
    public static Mode fromKey(String key){
        if(key==null)return Mode.INHERIT;
        for(Mode m:Mode.values())if(m.key.equals(key))return m;
        return Mode.INHERIT; // unknown persisted setting never overrides engine
    }
    public static Settings resolve(String key,int thermalStatus){
        Mode m=fromKey(key);
        if(m==Mode.INHERIT)return new Settings(false,false,1,0,false);
        if(m==Mode.NATIVE)return new Settings(true,false,1,0,false);
        if(thermalStatus>=4)return new Settings(true,false,1,0,true);
        return new Settings(true,m.enabled,m.nativeMode,m.sharpness,false);
    }
    public static float sourceRatio(int mode){
        switch(mode){
            case 1:return 1f;
            case 2:return .8f;
            case 3:return .75f;
            case 4:return 2f/3f;
            case 5:return .6f;
            case 6:return .5f;
            default:return 1f;
        }
    }
    public static long estimatedGuestPixels(int w,int h,int mode){
        if(w<=0||h<=0)return 0;
        float ratio=sourceRatio(mode);
        return (long)Math.ceil(w*ratio)*(long)Math.ceil(h*ratio);
    }
}
