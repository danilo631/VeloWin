from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]
class Independence(unittest.TestCase):
    def test_real_root_build(self):
        self.assertIn("include ':app'",(R/"settings.gradle").read_text())
        self.assertIn("include ':velowin-core'",(R/"settings.gradle").read_text())
        self.assertFalse((R/".gitmodules").exists())
        self.assertFalse((R/"third_party").exists())
    def test_spatial_pass(self):
        a=(R/"app/src/main/java/dev/velowin/app/graphics/VeloSpatialUpscaler.java").read_text()
        b=(R/"app/src/main/java/dev/velowin/app/benchmark/VeloBenchActivity.java").read_text()
        for v in ("glFramebufferTexture2D","GL_FRAMEBUFFER_COMPLETE","glDrawArrays","uSharpness","uTexel"):
            self.assertIn(v,a)
        for v in ("upscaler.begin(","upscaler.present(","GLES30.glDrawElements"):
            self.assertIn(v,b)
    def test_wine_gui_not_claimed(self):
        s=(R/"app/src/main/java/dev/velowin/app/engine/ContainerSession.java").read_text()
        self.assertIn("Ponte Wine/X11 nativa: não integrada",s)
if __name__=="__main__": unittest.main()
