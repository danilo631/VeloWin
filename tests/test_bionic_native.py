from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/velowin/app"

class BionicNativePipelineTest(unittest.TestCase):
    def test_real_wcp_installer_inert_and_safe(self):
        s=(SRC/"runtime/BionicWineInstaller.java").read_text()
        for item in ("ZstdInputStream","TarArchiveInputStream","ArchiveSafety.member",
                     "ArchiveSafety.symlinkTarget","MessageDigest.getInstance(\"SHA-256\")",
                     "profile.json","prefixPack.txz","ElfVerifier.isBionicMachine",
                     "Files.move","cleanupUnfinished"):
            self.assertIn(item,s)
        self.assertNotIn("ProcessBuilder",s)
        self.assertNotIn("Runtime.getRuntime()",s)

    def test_bionic_library_and_gamehub_profile(self):
        ui=(SRC/"MainActivity.java").read_text()
        for value in ("Buscar Proton Bionic no GitHub","Preparar arquivos Bionic",
                      "Aplicar configuração automática","Testar áudio, toque e controle",
                      "Testar Box64 Bionic"):
            self.assertIn(value,ui)
        self.assertIn("BionicArchiveStore.saved(this)",ui)

    def test_abi_never_assumed_equal(self):
        strategy=(SRC/"engine/RuntimeStrategy.java").read_text()
        self.assertIn("return archive == host",strategy)
        gate=(SRC/"engine/EngineGate.java").read_text()
        self.assertIn("isReady(Context context)",gate)
        self.assertIn("return false",gate)

    def test_opt_in_wine_execution_only(self):
        wine=(SRC/"engine/WineVersionSmoke.java").read_text()
        self.assertIn('"--version"',wine)
        self.assertIn("BionicWineInstaller.installed",wine)
        self.assertIn("Box64Smoke.bundled",wine)
        self.assertIn("BoundedProcess.run",wine)
        process=(SRC/"engine/BoundedProcess.java").read_text()
        self.assertIn("destroyForcibly",process)
        self.assertIn("streamDone",process)
        self.assertNotIn("getRuntime()",wine)
