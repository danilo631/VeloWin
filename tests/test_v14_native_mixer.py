"""Source contracts for the independent, full WinNative-based VeloWin flavor."""
from pathlib import Path
import unittest
import re

ROOT = Path(__file__).resolve().parents[1]

class NativeMixerContracts(unittest.TestCase):
    def test_full_engine_is_pinned_as_real_source(self):
        modules=(ROOT/".gitmodules").read_text(encoding="utf-8")
        self.assertIn('[submodule "third_party/winnative"]',modules)
        self.assertIn("https://github.com/WinNative-Emu/WinNative.git",modules)
        import subprocess
        tree=subprocess.check_output(
            ["git","ls-tree","HEAD","third_party/winnative"], cwd=ROOT,text=True
        ).strip()
        self.assertRegex(tree,r"160000 commit 8409085f706acbc0749881d9bf1e85f2f347dfd4")

    def test_correct_brand_but_upstream_namespace_retained(self):
        source=(ROOT/"tools/prepare-winnative.py").read_text(encoding="utf-8")
        self.assertIn('APPLICATION_ID = "dev.velowin.app"',source)
        self.assertIn('APP_LABEL = "VeloWin"',source)
        self.assertIn("UPSTREAM_SHA =",source)
        self.assertIn("XServerDisplayActivity.java",source)
        self.assertIn("XServerComponent.java",source)
        self.assertIn("ContainerManager.java",source)
        self.assertIn('CREDITS.md',source)
        self.assertIn('EMULATOR_CREDITS.md',source)
        self.assertIn("Original VeloWin MIT portions",source)
        self.assertNotIn('namespace \'dev.velowin.app\'',source)

    def test_full_native_apk_is_primary_build(self):
        workflow=(ROOT/".github/workflows/velowin-native.yml").read_text()
        self.assertIn("assembleStandardDebug",workflow)
        self.assertIn("lfs pull",workflow)
        self.assertIn("submodule update --init --recursive",workflow)
        self.assertIn("tools/prepare-winnative.py",workflow)
        self.assertIn("dev.velowin.app",workflow)
        self.assertIn("aapt",workflow)
        self.assertIn("actions/upload-artifact",workflow)
        self.assertIn("LICENSE",workflow)
        self.assertNotIn("EmbeddedX11Service",workflow)
        self.assertNotIn("ContainerLabActivity",workflow)

    def test_previous_experiment_cannot_replace_runtime(self):
        self.assertFalse((ROOT/".github/workflows/android.yml").exists())
        settings=(ROOT/"settings.gradle").read_text()
        self.assertNotIn("include ':app'",settings)
        self.assertIn("VeloWinNative",settings)
        entry=(ROOT/"build.gradle").read_text()
        self.assertIn("tools/build-native.sh",entry)
        self.assertFalse((ROOT/"app").exists())
        new=(ROOT/"README.md").read_text()
        self.assertIn("WinNative",new)
        self.assertIn("GPL-3.0",new)
        self.assertIn("VeloWin Full Native Runtime",new)

if __name__ == "__main__":
    unittest.main()
