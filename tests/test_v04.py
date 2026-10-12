from pathlib import Path
import re
import unittest

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/velowin/app"

class GameSessionV04Contracts(unittest.TestCase):
    def test_version_and_profiles(self):
        app=(ROOT/"app/build.gradle").read_text()
        self.assertIn("versionName '0.4.0-dev'",app)
        game=(JAVA/"library/Game.java").read_text()
        for field in ('"resolution"','"renderApi"','"fpsLimit"'):
            self.assertIn('out.put('+field,game)
        ui=(JAVA/"MainActivity.java").read_text()
        for item in ("Preparar prefixo Windows","Pré-verificação de sessão",
                     "Testar Wine deste jogo","Resolução virtual e FPS",
                     "Renderizador Direct3D","Compartilhar perfil"):
            self.assertIn(item,ui)

    def test_prefix_pack_cannot_escape_private_workspace(self):
        source=(JAVA/"engine/WinePrefixManager.java").read_text()
        for contract in ("ArchiveSafety.member","ArchiveSafety.symlinkTarget",
                         "ArchiveSafety.conflicts","Files.move","ATOMIC_MOVE",
                         "WinePackageInstaller.deleteTree(stage)",
                         "MAX_TOTAL","MAX_ENTRIES","LinkOption.NOFOLLOW_LINKS",
                         'root,"system.reg"','"prefixPack.txz"',
                         '"TEMPLATE_ONLY"'):
            self.assertIn(contract,source)
        self.assertNotIn("ProcessBuilder",source)
        self.assertNotIn("Runtime.getRuntime()",source)
        self.assertNotIn("getExternalStorageDirectory",source)
        self.assertNotIn("wineboot",source.split("public static File prepare(")[1].split("public static void remove(")[0].lower().replace("wineboot or game process", ""))

    def test_bionic_and_linux_never_auto_merge(self):
        ui=(JAVA/"MainActivity.java").read_text()
        self.assertIn("GameRuntimeChoice.bionic",ui)
        self.assertIn("BionicWineInstaller.installed",ui)
        self.assertIn("WinePackageInstaller.installed",ui)
        gate=(JAVA/"engine/EngineGate.java").read_text()
        self.assertIn("return false;",gate)

    def test_real_vulkan_probe_without_game_claim(self):
        native=(ROOT/"app/src/main/cpp/native_probe.c").read_text()
        for symbol in ("vkCreateInstance","vkEnumeratePhysicalDevices",
                       "vkCreateDevice","vkGetDeviceQueue","vkCreateCommandPool",
                       "vkDestroyCommandPool","vkDestroyDevice"):
            self.assertIn(symbol,native)
        self.assertIn("does NOT test DXVK",native)

if __name__=="__main__":
    unittest.main()
