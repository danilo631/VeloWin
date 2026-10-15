package dev.velowin.core;

import java.util.Locale;

/**
 * Immutable measured GLES workload result; never an interpolated/presented FPS claim.
 * Serialized formats contain actual measured samples, not synthetic scores.
 */
public final class VeloBenchReport {
    public final String renderer;
    public final int cubes;
    public final long timestampMs;
    public final long durationMs;
    public final String termination;
    public final GpuBenchMetrics.Snapshot metrics;

    public VeloBenchReport(String renderer, int cubes, long timestampMs,
                           long durationMs, String termination,
                           GpuBenchMetrics.Snapshot metrics) {
        if (renderer == null || termination == null || metrics == null)
            throw new IllegalArgumentException("Missing benchmark data");
        if (cubes < 1 || durationMs < 0 || timestampMs < 0)
            throw new IllegalArgumentException("Invalid benchmark run");
        this.renderer = renderer;
        this.cubes = cubes;
        this.timestampMs = timestampMs;
        this.durationMs = durationMs;
        this.termination = termination;
        this.metrics = metrics;
    }
    private static String n(float value) {
        return String.format(Locale.US, "%.4f", value);
    }
    private static String jsonString(String input) {
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 0x20) out.append(String.format(Locale.US, "\\u%04x", (int)c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
    private static String csvString(String value) {
        return "\"" + value.replace("\"", "\"\"").replace("\r"," ").replace("\n"," ") + "\"";
    }
    public String toJson() {
        return "{" +
          "\"schema\":\"velowin.gles3.v1\"," +
          "\"api\":\"OpenGL ES 3.0\"," +
          "\"renderer\":" + jsonString(renderer) + "," +
          "\"cubes\":" + cubes + "," +
          "\"unix_ms\":" + timestampMs + "," +
          "\"duration_ms\":" + durationMs + "," +
          "\"termination\":" + jsonString(termination) + "," +
          "\"sample_size\":" + metrics.sampleSize + "," +
          "\"total_samples\":" + metrics.totalFrames + "," +
          "\"average_fps\":" + n(metrics.averageFps) + "," +
          "\"one_percent_low_estimate_fps\":" + n(metrics.lowOnePercentFps()) + "," +
          "\"average_frame_ms\":" + n(metrics.averageMs) + "," +
          "\"p95_frame_ms\":" + n(metrics.p95Ms) + "," +
          "\"p99_frame_ms\":" + n(metrics.p99Ms) + "," +
          "\"presented_fps\":null,\"generated_fps\":null" +
          "}";
    }
    public String toCsv() {
        return "schema,api,renderer,cubes,unix_ms,duration_ms,termination," +
          "sample_size,total_samples,average_fps,one_percent_low_estimate_fps," +
          "average_frame_ms,p95_frame_ms,p99_frame_ms\n" +
          "velowin.gles3.v1,OpenGL ES 3.0," + csvString(renderer) + "," +
          cubes + "," + timestampMs + "," + durationMs + "," +
          csvString(termination) + "," + metrics.sampleSize + "," +
          metrics.totalFrames + "," + n(metrics.averageFps) + "," +
          n(metrics.lowOnePercentFps()) + "," + n(metrics.averageMs) +
          "," + n(metrics.p95Ms) + "," + n(metrics.p99Ms) + "\n";
    }
}
