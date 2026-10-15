"""VeloWin-owned shell and real upstream engine integration contracts."""
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
UI=(ROOT/"overlays/velowin-app/VeloWinHomeActivity.java").read_text()
PREP=(ROOT/"tools/prepare-winnative.py").read_text()

class ModularGameHubContracts(unittest.TestCase):
    def test_one_launcher_with_original_source_module(self):
        self.assertIn("VeloWinHomeActivity.java",PREP)
        self.assertIn("FrameGenPolicy.java",PREP)
        self.assertIn("new_launcher",PREP)
        self.assertIn("main_filter",PREP)
        self.assertIn("LAUNCHER",PREP)
        self.assertIn("VeloWinHomeActivity",PREP)
        self.assertIn('app/src/main/app/velowin',PREP)

    def test_real_container_and_wine_desktop_not_fake_shell(self):
        for symbol in ("new ContainerManager(", "ContainerCreation.createContainerAsync(",
                       "new VeloEngineAdapter(this,containers).launch(",
                       'new VeloEnginePort.LaunchRequest(c.id',
                       "new ContentsManager(", "contents.syncContents()",
                       "ImageFs.find(this).isUpToDate()",
                       "SetupWizardActivity.isSetupComplete(this)"):
            self.assertIn(symbol,UI)
        self.assertNotIn("ContainerLabActivity",UI)
        self.assertNotIn("EmbeddedX11Service",UI)
        self.assertNotIn("Runtime.getRuntime()",UI)
        self.assertNotIn("ProcessBuilder",UI)

    def test_framegen_triggers_real_dis_pipeline(self):
        for symbol in ('c.putExtra("disFrameGen","1")',
                       'c.putExtra("frameGen","0")',
                       'c.putExtra("disFrameGenScale",String.valueOf(quality.minSide))',
                       '"disFrameGenTargetFps"',
                       'c.saveData()', 'PackageManager.FEATURE_VULKAN_HARDWARE_VERSION',
                       "FrameGenPolicy.safeTarget"):
            self.assertIn(symbol,UI)
        self.assertIn('Container.DISPLAY_BACKEND_X11',UI)
        self.assertIn('new Intent(this,VeloSettingsActivity.class)',UI)

    def test_native_workflow_keeps_audit(self):
        yml=(ROOT/".github/workflows/velowin-native.yml").read_text()
        self.assertIn("feature/v15-modular-velowin-gamehub",yml)
        self.assertIn("test_v15_modular.py",yml)
        self.assertIn("FrameGenPolicyTest",yml)
        self.assertIn("VeloWinHomeActivity",yml)
        self.assertTrue(any(x in yml for x in ("VeloWin-Modular-GameHub-FrameGen-debug", "VeloWin-VeloFlow-Mali-debug", "VeloWin-Runtime-VeloFlow-v17-debug", "VeloWin-Recovery-Settings-v18-debug", "VeloWin-VeloBench-v19-debug", "VeloWin-Hybrid-v22-debug")))

if __name__=="__main__":
    unittest.main()
