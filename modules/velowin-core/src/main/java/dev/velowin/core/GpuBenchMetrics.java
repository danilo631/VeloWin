package dev.velowin.core;

import java.util.Arrays;

/**
 * CPU-visible frame cadence from real 3D GLES swaps.
 * Separates FPS from 1% low and p95; no fake FrameGen multipliers.
 * Uses fixed allocation and ignores resumed/paused outliers above 1000 ms.
 */
public final class GpuBenchMetrics {
    private final float[] frameMs;
    private int used, cursor;
    private long totalFrames;
    public GpuBenchMetrics(int capacity) {
        if(capacity<10||capacity>10000)throw new IllegalArgumentException("capacity");
        frameMs=new float[capacity];
    }
    public synchronized void addFrameNanos(long ns){
        if(ns<=0 || ns>1_000_000_000L)return;
        frameMs[cursor]=(float)(ns/1_000_000.0);
        cursor=(cursor+1)%frameMs.length;
        if(used<frameMs.length)used++;
        totalFrames++;
    }
    public synchronized Snapshot snapshot(){
        if(used==0)return new Snapshot(0,0,0,0,0,0);
        float[] sorted=Arrays.copyOf(frameMs,used);
        float sum=0;
        for(float x:sorted)sum+=x;
        Arrays.sort(sorted);
        final float average=sum/used;
        float p95=sorted[Math.min(used-1,(int)Math.ceil(0.95*used)-1)];
        float p99=sorted[Math.min(used-1,(int)Math.ceil(0.99*used)-1)];
        return new Snapshot(used,totalFrames,1000f/average,
            average,p95,p99);
    }
    public static final class Snapshot {
        public final int sampleSize;
        public final long totalFrames;
        public final float averageFps,averageMs,p95Ms,p99Ms;
        Snapshot(int size,long frames,float fps,float avg,float p95,float p99){
            sampleSize=size;totalFrames=frames;averageFps=fps;
            averageMs=avg;p95Ms=p95;p99Ms=p99;
        }
        public float lowOnePercentFps(){return p99Ms>0?1000f/p99Ms:0;}
    }
}
