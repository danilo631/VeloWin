package dev.velowin.core;

/**
 * Independent VeloWin 3D/FrameGen performance policy.
 * This is NOT the DIS optical-flow algorithm (see original OpenFlow GPL code).
 * Min-side resolutions MUST match the renderer's real 180/252/360 options.
 */
public final class VeloFlowProfile {
    private VeloFlowProfile(){}
    public enum Quality {
        MALI_FAST(180,"Econômico",2),
        BALANCED(252,"Equilibrado",3),
        SHARPNESS(360,"Qualidade",4);
        public final int minSide, costWeight;
        public final String label;
        Quality(int side,String label,int cost){this.minSide=side;this.label=label;costWeight=cost;}
    }
    public static Quality normalize(int stored){
        // Backward compatibility with legacy percent (e.g. 70%=504→360).
        if(stored<=100 && stored>0){
            stored=(stored*720)/100;
        }
        int nearest=Integer.MAX_VALUE;
        Quality choice=Quality.BALANCED;
        for(Quality q:Quality.values()){
            int delta=Math.abs(q.minSide-stored);
            if(delta<nearest){nearest=delta;choice=q;}
        }
        return choice;
    }
    /** Intended ceiling, not an FPS measurement or guarantee. */
    public static int target(int desired,float displayHz,int thermal){
        if(desired<=0)return 0;
        if(!Float.isFinite(displayHz)||displayHz<30.0f)return 0;
        int panel=Math.max(0,Math.min(240,(int)(displayHz+0.25f)));
        if(thermal>=4)panel=Math.min(30,panel);
        else if(thermal>=2)panel=Math.min(60,panel);
        return Math.min(desired,panel);
    }
    /**
     * Conservative suggestion for Mali based on ACTUAL GLES 3D frame time.
     * Does not claim GLES performance equals optical-flow compute performance.
     */
    public static Quality suggestQuality(float p95FrameMs, int thermal) {
        if(!Float.isFinite(p95FrameMs)||p95FrameMs<=0||thermal>=3)
            return Quality.MALI_FAST;
        if(p95FrameMs>20.0f)return Quality.MALI_FAST;
        if(p95FrameMs>11.5f||thermal>=2)return Quality.BALANCED;
        return Quality.SHARPNESS;
    }
    /** New optional scale must not silently modify an existing active session. */
    public static boolean canSuggestSwitch(long lastChangeMs,long nowMs,
                                           float benefitFraction){
        return nowMs>=lastChangeMs+15_000L
                && Float.isFinite(benefitFraction)
                && benefitFraction>=0.15f;
    }
}
