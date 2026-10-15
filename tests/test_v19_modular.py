from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
def file(path):return (ROOT/path).read_text(encoding="utf-8")

class Velo19Contracts(unittest.TestCase):
    def test_core_has_no_upstream_android_dep(self):
        root=ROOT/"modules/velowin-core/src/main/java"
        names=list(root.rglob("*.java"))
        self.assertGreaterEqual(len(names),3)
        for name in names:
            content=name.read_text()
            for forbidden in ("import android.", "import com.winlator.",
                    "import com.winnative.","System.loadLibrary"):
                self.assertNotIn(forbidden,content,name.as_posix())
        self.assertIn("id 'java-library'",file("modules/velowin-core/build.gradle"))
        self.assertIn("interface VeloEnginePort",file(
            "modules/velowin-core/src/main/java/dev/velowin/core/VeloEnginePort.java"))
    def test_real_gpu_workload(self):
        src=file("overlays/velowin-app/VeloBenchActivity.java")
        for symbol in ("GLSurfaceView", "GLES30.glDrawElements",
             "GLES30.glFinish()", "GLES30.glCompileShader",
             "Matrix.perspectiveM","GpuBenchMetrics", "lowOnePercentFps",
             "VeloFlowProfile.suggestQuality", "requestedLoad=48"):
            self.assertIn(symbol,src)
        self.assertNotIn("Math.random()",src)
        self.assertIn("glmark2",src)
        self.assertIn("onPause()",src)
    def test_runtime_and_quality_are_replaceable(self):
        prep=file("tools/prepare-winnative.py")
        home=file("overlays/velowin-app/VeloWinHomeActivity.java")
        settings=file("overlays/velowin-app/VeloSettingsActivity.java")
        bridge=file("overlays/velowin-app/VeloEngineAdapter.java")
        for symbol in ("VeloBenchActivity.java","VeloEngineAdapter.java",
             "shutil.copytree(original_core","include ':velowin-core'",
             "implementation project(':velowin-core')"):
            self.assertIn(symbol,prep)
        self.assertIn("new VeloEngineAdapter",home)
        self.assertIn("new VeloEngineAdapter",settings)
        self.assertIn("XServerDisplayActivity.class",bridge)
        self.assertNotIn("XServerDisplayActivity.class",home)
        self.assertNotIn("XServerDisplayActivity.class",settings)
        self.assertIn('VeloFlowProfile.normalize(',home)
        self.assertIn('String.valueOf(quality.minSide)',home)
        self.assertNotIn('c.putExtra("disFrameGenScale","70")',home)
        self.assertIn("VeloBenchActivity.class",home)
    def test_ci_and_build(self):
        workflow=file(".github/workflows/velowin-native.yml")
        self.assertIn("VeloCoreTest",workflow)
        self.assertIn("test_v19_modular.py",workflow)
        self.assertTrue(any(name in workflow for name in ("VeloWin-VeloBench-v19-debug","VeloWin-Hybrid-v22-debug")) )
        self.assertIn("velowin-core",workflow)
if __name__=="__main__":
    unittest.main()
