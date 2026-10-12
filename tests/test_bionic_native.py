from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

class NativeBionicTest(unittest.TestCase):
    def test_jni_probe_and_strategy(self):
        src = (ROOT/"app/src/main/cpp/native_probe.c").read_text()
        for val in ("vkCreateInstance", "vkEnumeratePhysicalDevices",
                    "vkGetPhysicalDeviceProperties", "vkDestroyInstance"):
            self.assertIn(val,src)
        strategy=(ROOT/"app/src/main/java/dev/velowin/app/engine/RuntimeStrategy.java").read_text()
        self.assertIn("archive == host",strategy)

    def test_box64_is_opt_in(self):
        smoke=(ROOT/"app/src/main/java/dev/velowin/app/engine/Box64Smoke.java").read_text()
        self.assertIn('nativeLibraryDir',smoke)
        self.assertIn('"-v"',smoke)
        self.assertNotIn("Runtime.getRuntime().exec",smoke)
        action=(ROOT/".github/workflows/box64-bionic.yml").read_text()
        self.assertIn("c6a8c4472f74f9ad204d35195f084b5f996b12e3",action)
        self.assertIn("libvelobox64.so",action)

    def test_bionic_signed_metadata(self):
        pol=(ROOT/"app/src/main/java/dev/velowin/app/runtime/BionicReleasePolicy.java").read_text()
        self.assertIn("sha256:",pol)
        self.assertIn("GameNative/proton-wine",pol)
