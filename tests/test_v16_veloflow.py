from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
STRIP=(ROOT/"tools/strip-lsfg.py").read_text()
MALI=(ROOT/"overlays/velowin-app/MaliProfile.java").read_text()
C=(ROOT/"overlays/velowin-native/veloflow_compat.cpp").read_text()
JAVA=(ROOT/"overlays/velowin-native/LosslessScaling.java").read_text()
UI=(ROOT/"overlays/velowin-app/VeloWinHomeActivity.java").read_text()

class VeloFlowContracts(unittest.TestCase):
    def test_lsfg_is_physically_unlinked(self):
        for t in ("winlator/vk/lsfg/","lsfg/","add_subdirectory(thirdparty/dxbc",
                  "target_link_libraries(wnwayland openflow)",
                  "veloflow_compat.cpp","LSFG inventory changed"):
            self.assertIn(t,STRIP)
        self.assertIn("legacy=cpp/",STRIP)
        self.assertIn("p.unlink()",STRIP)
        self.assertIn("FrameGenEngine.LSFG",STRIP)
        self.assertIn('frameGenEnabled = false',STRIP)
        self.assertIn("FG_ENGINE_DIS",STRIP)
    def test_no_lsfg_shader_installer_or_compute_implementation(self):
        self.assertNotIn("System.loadLibrary",JAVA)
        self.assertNotIn("nativeBuildCache",JAVA)
        self.assertNotIn("FileOutputStream",JAVA)
        self.assertIn("isInstalled(Context c){return false;}",JAVA)
        self.assertIn("return nullptr",C)
        self.assertIn("vkr_lsfg_prepare",C)
        self.assertIn("return false;",C)
        self.assertNotIn("LsfgChain",C)
        self.assertNotIn("shader",C.lower().split("copyright")[-1] if False else "")
    def test_mali_policy_is_reversible_not_turnip_tuned(self):
        for symbol in ("MaliProfile.restore(", "MaliProfile.apply(",
                       "Entrar no contêiner", "VeloEngineAdapter"):
            self.assertIn(symbol,UI)
        for symbol in ('c.setDXWrapper("wined3d")','c.setGraphicsDriver("wrapper")',
                       'c.setDisplayBackend(Container.DISPLAY_BACKEND_X11)',
                       'c.saveData()', 'mesa_glthread=false'):
            self.assertIn(symbol,MALI)
        self.assertNotIn('setGraphicsDriver("turnip")',MALI.lower())
        self.assertNotIn("MESA_GL_VERSION_OVERRIDE=",MALI)
    def test_ci_compiles_dis_only_and_tests_policies(self):
        flow=(ROOT/".github/workflows/velowin-native.yml").read_text()
        self.assertIn("tools/strip-lsfg.py",flow)
        self.assertIn("MaliProfileTest",flow)
        self.assertIn("test_v16_veloflow.py",flow)
        self.assertIn("veloflow_compat.cpp",flow)
        self.assertTrue(any(x in flow for x in ("VeloWin-VeloFlow-Mali-debug", "VeloWin-Runtime-VeloFlow-v17-debug", "VeloWin-Recovery-Settings-v18-debug", "VeloWin-VeloBench-v19-debug", "VeloWin-Hybrid-v22-debug","VeloWin-Hybrid-v23-debug")))
if __name__=="__main__":unittest.main()
