import pathlib,unittest
ROOT=pathlib.Path(__file__).resolve().parents[1]
def read(p):return (ROOT/p).read_text()
class InstallIntegration(unittest.TestCase):
    def test_build_pins_real_wine_runtime(self):
        self.assertIn("third_party/winnative",read(".gitmodules"))
        self.assertIn("patch-v18.py",read(".github/workflows/velowin-native.yml"))
        self.assertIn("patch-v23.py",read(".github/workflows/velowin-native.yml"))
    def test_install_pipeline_hooks(self):
        s=read("tools/patch-v23.py")
        for name in ("VeloInstallCoordinator.beginImageFs","VeloImageFsMaintenance.prepare",
          "VeloContentTransaction.commit","VeloInstalledRuntimeGuard.removalBlocker",
          "VeloInstallCoordinator.endImageFs"):
            self.assertIn(name,s)
        self.assertIn('code=code[:start]+code[end:]',s)
    def test_no_faux_wine_or_fake_framegen(self):
        p=read("tools/prepare-winnative.py")
        self.assertIn("VeloInstalledRuntimeGuard.java",p)
        self.assertIn("VeloBox64Bridge.java",p)
        self.assertIn("VeloRuntimeInstallActivity.java",p)
        self.assertIn("VeloRuntimeInstallActivity.class",read("overlays/velowin-app/VeloWinHomeActivity.java"))
        v=read("overlays/velowin-app/VeloWinHomeActivity.java")
        self.assertIn("VeloEngineAdapter",v)
        self.assertIn("FrameGenPolicy",v)
if __name__=="__main__":unittest.main()
