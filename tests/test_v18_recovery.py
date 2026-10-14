"""VeloWin v18 launch-failure and independent UI regression contracts."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
def load(p):return (ROOT/p).read_text(encoding="utf-8")
class RecoveryContracts(unittest.TestCase):
    def test_launch_routing_and_watchdog_are_real(self):
        p=load("tools/patch-v18.py")
        for token in ("XServerDisplayActivity.java","setupXEnvironment();",
            "ensureWinePrefixEssentialFiles();","VeloBootTarget.select",
            "checkVeloWinFirstWindow","firstGuestWindowShown.get()",
            "postDelayed","35000L",
            "VeloSettingsActivity.class","ContentDialog.alert"):
            self.assertIn(token,p)
        self.assertIn('"wfm.exe"',load("overlays/velowin-app/VeloSessionDiagnostics.java"))
        self.assertIn("ImageFs",load("overlays/velowin-app/VeloSessionDiagnostics.java"))
    def test_original_settings_replace_winnative_dashboard(self):
        home=load("overlays/velowin-app/VeloWinHomeActivity.java")
        settings=load("overlays/velowin-app/VeloSettingsActivity.java")
        self.assertNotIn("UnifiedActivity.class",home)
        self.assertIn("VeloSettingsActivity.class",home)
        for item in ("Modo seguro","VeloWin-Imports","Box64","WineD3D",
                     "PulseAudio","ACTION_OPEN_DOCUMENT","VeloSessionDiagnostics",
                     "ContentsManager","CONTAINER_ID"):
            self.assertIn(item,settings)
        self.assertIn("getContainerById",settings)
        self.assertIn("512L*1024L*1024L",settings)
        self.assertIn("Files.isSymbolicLink",settings)
    def test_pipeline_applies_patch_to_actual_engine(self):
        cloud=load(".github/workflows/velowin-native.yml")
        local=load("tools/build-native.sh")
        prepare=load("tools/prepare-winnative.py")
        for item in ("patch-v18.py","strip-lsfg.py","patch-v17.py"):
            self.assertIn(item,cloud)
            self.assertIn(item,local)
        self.assertIn("VeloSettingsActivity.java",prepare)
        self.assertIn("VeloBootTarget.java",prepare)
        self.assertIn("VeloSessionDiagnostics.java",prepare)
        self.assertIn('android:exported="false"',prepare)
        self.assertIn("VeloBootTargetTest",cloud)
if __name__=="__main__":unittest.main()
