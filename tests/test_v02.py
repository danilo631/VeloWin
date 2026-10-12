from pathlib import Path
import unittest
ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/dev/velowin/app"
class VeloWinV02Test(unittest.TestCase):
    def test_install_is_not_execution(self):
        installer = (JAVA / "runtime/WinePackageInstaller.java").read_text()
        assert "TarArchiveInputStream" in installer
        assert "XZCompressorInputStream" in installer
        assert "MessageDigest.getInstance(\"SHA-256\")" in installer
        assert "deleteTree(temp)" in installer
        assert "getNextEntry()" in installer
        assert "Runtime.exec" not in installer
        assert "ProcessBuilder" not in installer
        self.assertIn("EngineGate.reason", (JAVA / "MainActivity.java").read_text())

    def test_search_favorites_and_pe(self):
        main = (JAVA / "MainActivity.java").read_text()
        for phrase in ("Mostrar apenas favoritos", "Verificar acesso ao arquivo",
                       "PeInspector.inspect", "Preparar arquivos Wine",
                       "WinePackageInstaller.install"):
            self.assertIn(phrase, main)

    def test_two_phase_game_persistence(self):
        store = (JAVA / "library/GameStore.java").read_text()
        self.assertIn("persist(updated)", store)
        self.assertIn("items.clear()", store)
        self.assertLess(store.index("persist(updated)"), store.index("items.clear()"))
