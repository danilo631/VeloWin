from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]
class HybridSafety(unittest.TestCase):
    def test_keeps_real_native_engine(self):
        p=(R/".gitmodules").read_text()
        self.assertIn("third_party/winnative",p)
        s=(R/"tools/patch-v18.py").read_text()
        self.assertIn("checkVeloWinFirstWindow",s)
        self.assertIn("onFirstGuestWindow",s)
    def test_does_not_replace_xserver(self):
        s=(R/"tools/patch-v22.py").read_text()
        self.assertIn("XServerDisplayActivity.java",s)
        self.assertIn("loadScreenEffects",s)
        self.assertIn("VeloHybridGraphics.forLaunch",s)
        self.assertIn("VeloBox64Bridge.apply",s)
        self.assertIn("shortcut == null",s)
        self.assertIn("isDependencyInstall",s)
        self.assertNotIn("onFirstGuestWindow() {",s)
    def test_real_upscaler_not_android_lab(self):
        s=(R/"overlays/velowin-app/VeloSettingsActivity.java").read_text()
        self.assertIn("VeloSR",s)
        self.assertIn("VeloBox64Bridge",s)
        self.assertIn("VeloEngineAdapter",s)
        self.assertFalse((R/"app").exists())
if __name__=="__main__": unittest.main()
