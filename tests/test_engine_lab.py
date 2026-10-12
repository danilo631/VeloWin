from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
class EngineLabContracts(unittest.TestCase):
    def test_surface_lab_is_not_a_windows_renderer(self):
        content=(ROOT/"app/src/main/java/dev/velowin/app/hardware/GpuSurfaceLabActivity.java").read_text()
        for symbol in ("GLSurfaceView","GLES20.glDrawArrays",
                       "SystemClock.elapsedRealtimeNanos","onSurfaceCreated"):
            self.assertIn(symbol,content)
        self.assertIn("Não renderiza Wine",content)
        self.assertNotIn("ProcessBuilder",content)
    def test_per_game_box64_settings_persisted(self):
        game=(ROOT/"app/src/main/java/dev/velowin/app/library/Game.java").read_text()
        self.assertIn('out.put("boxPreset", boxPreset)',game)
        ui=(ROOT/"app/src/main/java/dev/velowin/app/MainActivity.java").read_text()
        self.assertIn("Ajustar Box64 Dynarec",ui)
        self.assertIn("Box64Tuning.Preset",ui)
