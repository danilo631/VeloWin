from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]

class VeloWinV06Contracts(unittest.TestCase):
    def test_primary_apk_compiles_and_contains_box64(self):
        workflow = (ROOT / ".github/workflows/android.yml").read_text()
        self.assertIn("git checkout -q c6a8c4472f74f9ad204d35195f084b5f996b12e3", workflow)
        self.assertIn("-DANDROID=ON -DARM64=ON", workflow)
        self.assertIn("libvelobox64.so", workflow)
        self.assertIn("llvm-readelf", workflow)
        self.assertIn("Confirm APK contains executable Box64", workflow)
        self.assertIn("velowin-v08-bionic-bridge-debug", workflow)

    def test_native_driver_smoke_is_not_render_claim(self):
        native = (ROOT / "app/src/main/cpp/vulkan_wsi.c").read_text()
        for symbol in ("vkCreateSwapchainKHR", "vkDestroySwapchainKHR", "vkGetSwapchainImagesKHR",
                       "VK_PRESENT_MODE_FIFO_KHR", "vkCreateDevice", "vkDestroyDevice",
                       "ANativeWindow_release"):
            self.assertIn(symbol, native)
        self.assertNotIn("vkQueuePresentKHR", native)
        self.assertNotIn("vkCmdDraw", native)

    def test_turnip_does_not_get_enabled_without_package(self):
        driver = (ROOT / "app/src/main/java/dev/velowin/app/hardware/GpuDriverPolicy.java").read_text()
        main = (ROOT / "app/src/main/java/dev/velowin/app/MainActivity.java").read_text()
        self.assertIn("boolean shippedTurnip=false", driver)
        self.assertIn("Turnip não instalado", main)
        self.assertIn("GpuDriverPolicy.evaluate", main)

    def test_workflow_still_runs_security_regressions(self):
        workflow = (ROOT / ".github/workflows/android.yml").read_text()
        self.assertIn("GpuDriverPolicyTest", workflow)
        self.assertIn("BionicReleasePolicyTest", workflow)
        self.assertIn("BionicSessionPlanTest", workflow)
