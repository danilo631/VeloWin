from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/velowin/app"

class VulkanControllerContracts(unittest.TestCase):
    def test_wsi_probe_is_capability_only(self):
        src=(ROOT/"app/src/main/cpp/vulkan_wsi.c").read_text()
        for piece in ("vkCreateAndroidSurfaceKHR","vkGetPhysicalDeviceSurfaceSupportKHR",
                      "vkGetPhysicalDeviceSurfaceCapabilitiesKHR","VK_KHR_SWAPCHAIN_EXTENSION_NAME",
                      "vkDestroySurfaceKHR","ANativeWindow_release"):
            self.assertIn(piece,src)
        for forbidden in ("vkQueuePresentKHR","vkCreateSwapchainKHR","vkCmdDraw"):
            self.assertNotIn(forbidden,src)
        cmake=(ROOT/"app/src/main/cpp/CMakeLists.txt").read_text()
        self.assertIn("vulkan_wsi.c",cmake)
        self.assertIn("ANDROID_LIB",cmake)

    def test_virtual_input_is_independent(self):
        activity=(SRC/"hardware/VirtualControllerLabActivity.java").read_text()
        mapper=(SRC/"hardware/AndroidGamepadMapper.java").read_text()
        self.assertIn("ACTION_POINTER_UP",activity)
        self.assertIn("onGenericMotionEvent",activity)
        self.assertIn("AXIS_HAT_X",mapper)
        self.assertIn("AXIS_LTRIGGER",mapper)
        self.assertNotIn("ProcessBuilder",activity)
        self.assertNotIn("Runtime.exec",activity)

    def test_manifest_and_navigation(self):
        manifest=ET.parse(ROOT/"app/src/main/AndroidManifest.xml")
        ns="{http://schemas.android.com/apk/res/android}"
        names={a.attrib[ns+"name"] for a in manifest.findall("application/activity")}
        self.assertIn(".hardware.VulkanWsiLabActivity",names)
        self.assertIn(".hardware.VirtualControllerLabActivity",names)
        main=(SRC/"MainActivity.java").read_text()
        self.assertIn("Testar apresentação Vulkan nativa",main)
        self.assertIn("Testar controles virtuais",main)
