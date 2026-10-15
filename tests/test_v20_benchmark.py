from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]

class BenchmarkLifecycleTests(unittest.TestCase):
    def test_bounded_run_and_real_export(self):
        bench=(ROOT/"overlays/velowin-app/VeloBenchActivity.java").read_text()
        for expected in ("30_000_000_000L", "metrics.clear()", "GLSurfaceView.RENDERMODE_WHEN_DIRTY",
                         "Intent.ACTION_CREATE_DOCUMENT", "getContentResolver().openOutputStream",
                         "bench_history", "finishRun(", "requestedRunId",
                         "GLES30.glFinish()", "GLES30.glDrawElements"):
            self.assertIn(expected,bench)
        report=(ROOT/"modules/velowin-core/src/main/java/dev/velowin/core/VeloBenchReport.java").read_text()
        self.assertIn("presented_fps",report)
        self.assertIn("generated_fps",report)
        self.assertIn("toCsv()",report)
        self.assertIn("toJson()",report)
        self.assertNotIn("Math.random()",report)
    def test_old_wine_and_framegen_launch_untouched(self):
        patch=(ROOT/"tools/patch-v18.py").read_text()
        self.assertIn("checkVeloWinFirstWindow",patch)
        self.assertIn("VeloBootTarget.select",patch)
        native=(ROOT/"overlays/velowin-native/veloflow_pacing.h").read_text()
        self.assertIn("veloflow_safe_target",native)

if __name__=="__main__":
    unittest.main()
