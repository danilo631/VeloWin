from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
def src(path):return (ROOT/path).read_text(encoding="utf-8")

class VeloRuntimeV17Contracts(unittest.TestCase):
    def test_separate_backend_contracts(self):
        policy=src("overlays/velowin-app/VeloRuntimePlan.java")
        adapter=src("overlays/velowin-app/VeloRuntimeBridge.java")
        for x in ("BIONIC_BOX64","BIONIC_ARM64EC_FEX",
                  "GLIBC_PROOT_GAMESCOPE","EXTERNAL_GLIBC",
                  "Instale um prefixo Wine ARM64EC","Box64 não encontrado"):
            self.assertIn(x,policy)
        self.assertIn("LinuxRuntime.isInstalled(context)",adapter)
        self.assertIn("LinuxRuntime.prootBinary(context).isFile()",adapter)
        self.assertIn("LinuxRuntime.prootLoader(context).isFile()",adapter)
        self.assertIn('c.setEmulator64("fexcore")',adapter)
        self.assertIn('c.setEmulator64("box64")',adapter)
        self.assertNotIn("Runtime.getRuntime().exec",adapter)
        self.assertNotIn("new ProcessBuilder",adapter)
    def test_ui_uses_real_capability_backend(self):
        home=src("overlays/velowin-app/VeloWinHomeActivity.java")
        self.assertIn("VeloRuntimeBridge.choices(",home)
        self.assertIn("VeloRuntimeBridge.select(",home)
        self.assertIn('button(card,"Método: Bionic / FEX / Linux PRoot"',home)
        self.assertIn("FrameGenPolicy.safeTarget(",home)
        self.assertIn("power.getCurrentThermalStatus()",home)
    def test_native_governor_is_linked_to_both_presenters(self):
        patch=src("tools/patch-v17.py")
        header=src("overlays/velowin-native/veloflow_pacing.h")
        for x in ("fg_present.c","vk_renderer.c","veloflow_safe_target",
                  "VeloFlow"):
            self.assertTrue(x in patch or x in header)
        self.assertIn("isfinite(display_hz)",header)
        self.assertIn("if(panel>240u)",header)
        self.assertIn("framegen",patch)
        self.assertIn("veloflow_safe_target",patch)
        self.assertIn("vkr_dis_configure",patch)
    def test_local_and_ci_build_consistent(self):
        local=src("tools/build-native.sh")
        cloud=src(".github/workflows/velowin-native.yml")
        for x in ("tools/prepare-winnative.py","tools/strip-lsfg.py",
                  "tools/patch-v17.py"):
            self.assertIn(x,local)
            self.assertIn(x,cloud)
        self.assertIn("VeloRuntimePlanTest",cloud)
        self.assertIn("VeloFlowPacingTest",cloud)
        self.assertTrue(any(name in cloud for name in ("VeloWin-Runtime-VeloFlow-v17-debug", "VeloWin-Recovery-Settings-v18-debug", "VeloWin-VeloBench-v19-debug", "VeloWin-Hybrid-v22-debug", "VeloWin-Hybrid-v23-debug")))
if __name__=="__main__":unittest.main()
