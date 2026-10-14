from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

BASE = Path(__file__).resolve().parents[1]
JAVA = BASE / "app/src/main/java/dev/velowin/app"

class VeloWinFoundationTest(unittest.TestCase):
    def test_unique_identity_and_permissions(self):
        manifest = ET.parse(BASE / "app/src/main/AndroidManifest.xml").getroot()
        uri = "{http://schemas.android.com/apk/res/android}"
        permissions = [e.attrib[uri + "name"] for e in manifest.findall("uses-permission")]
        self.assertEqual(set(permissions), {"android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE",
             "android.permission.REQUEST_INSTALL_PACKAGES"})
        # Installer permission is used only by the user-approved official
        # Termux:X11 APK flow, never for silent system package installation.
        provider=(JAVA/"runtime/ComponentApkProvider.java").read_text()
        self.assertIn('ParcelFileDescriptor.MODE_READ_ONLY',provider)
        self.assertIn('ComponentStore.verifyApk',provider)
        app = manifest.find("application")
        self.assertEqual(app.attrib[uri + "usesCleartextTraffic"], "false")
        gradle = (BASE / "app/build.gradle").read_text()
        self.assertIn("dev.velowin.app", gradle)
        self.assertNotIn("com.winlator", gradle)

    def test_engine_does_not_fake_windows_execution(self):
        engine = (JAVA / "engine/EngineGate.java").read_text()
        self.assertIn("return false;", engine)
        self.assertIn("Motor Windows ainda não integrado", engine)
        self.assertNotIn("Runtime.exec(", engine)

    def test_import_uses_android_saf(self):
        main = (JAVA / "MainActivity.java").read_text()
        self.assertIn("Intent.ACTION_OPEN_DOCUMENT", main)
        self.assertIn("takePersistableUriPermission", main)
        self.assertIn("EngineGate.reason", main)

    def test_downloader_keeps_verified_archives_inert(self):
        downloader = (JAVA / "runtime/RuntimeDownloader.java").read_text()
        self.assertIn('MessageDigest.getInstance("SHA-256")', downloader)
        self.assertIn("entry.sha256.equals(calculated.toString())", downloader)
        self.assertIn('".part"', downloader)
        self.assertNotIn("ProcessBuilder", downloader)
        self.assertNotIn("Runtime.getRuntime()", downloader)

if __name__ == "__main__":
    unittest.main()
