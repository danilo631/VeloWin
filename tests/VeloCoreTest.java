import dev.velowin.core.GpuBenchMetrics;
import dev.velowin.core.VeloFlowProfile;
import dev.velowin.core.VeloEnginePort;

public final class VeloCoreTest {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    public static void main(String[] a) {
        check(VeloFlowProfile.normalize(180)==VeloFlowProfile.Quality.MALI_FAST,"fast");
        check(VeloFlowProfile.normalize(252)==VeloFlowProfile.Quality.BALANCED,"balanced");
        check(VeloFlowProfile.normalize(360)==VeloFlowProfile.Quality.SHARPNESS,"quality");
        check(VeloFlowProfile.normalize(70)==VeloFlowProfile.Quality.SHARPNESS,
            "legacy 70 was 70% of 720 not 70px");
        check(VeloFlowProfile.target(120,60,0)==60,"refresh limit");
        check(VeloFlowProfile.target(120,120,4)==30,"thermal cap");
        check(VeloFlowProfile.target(60,Float.NaN,0)==0,"invalid hz");
        check(VeloFlowProfile.target(0,60,4)==0,"adaptive unchanged");
        check(VeloFlowProfile.suggestQuality(23,0)==VeloFlowProfile.Quality.MALI_FAST,
            "heavy GPU fast");
        check(VeloFlowProfile.suggestQuality(14,0)==VeloFlowProfile.Quality.BALANCED,
            "medium GPU balanced");
        check(VeloFlowProfile.suggestQuality(8,0)==VeloFlowProfile.Quality.SHARPNESS,
            "spare GPU quality");
        check(VeloFlowProfile.suggestQuality(8,3)==VeloFlowProfile.Quality.MALI_FAST,
            "thermal caution");
        check(!VeloFlowProfile.canSuggestSwitch(40000,49000,.5f),"cooldown");
        check(VeloFlowProfile.canSuggestSwitch(40000,56000,.2f),"hysteresis");
        GpuBenchMetrics meter=new GpuBenchMetrics(100);
        for(int i=0;i<100;i++)meter.addFrameNanos(i>95?32_000_000L:16_000_000L);
        GpuBenchMetrics.Snapshot snapshot=meter.snapshot();
        check(snapshot.sampleSize==100 && snapshot.totalFrames==100,"samples");
        check(snapshot.p95Ms==16f && snapshot.p99Ms==32f,"tail latency");
        check(snapshot.lowOnePercentFps()>30f
            &&snapshot.lowOnePercentFps()<32f,"1pct");
        meter.addFrameNanos(0);
        meter.addFrameNanos(2_000_000_000L);
        check(meter.snapshot().totalFrames==100,"bad durations discarded");
        new VeloEnginePort.LaunchRequest(1,
            VeloEnginePort.LaunchKind.WINDOWS_DESKTOP,null);
        boolean rejected=false;
        try {new VeloEnginePort.LaunchRequest(0,
            VeloEnginePort.LaunchKind.WINDOWS_DESKTOP,null);}
        catch(IllegalArgumentException e){rejected=true;}
        check(rejected,"unsafe id rejected");
        System.out.println("VeloCoreTest PASS");
    }
}
