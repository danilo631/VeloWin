from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"app/src/main/java/dev/velowin/app"

class ComponentCenterContract(unittest.TestCase):
    def test_ui_registration(self):
        app=ET.parse(ROOT/"app/src/main/AndroidManifest.xml").getroot().find("application")
        a="{http://schemas.android.com/apk/res/android}"
        activity=[x for x in app.findall("activity")
                  if x.get(a+"name")==".ComponentCenterActivity"]
        self.assertEqual(len(activity),1)
        self.assertEqual(activity[0].get(a+"exported"),"false")
        providers=[x for x in app.findall("provider")
            if x.get(a+"name")==".runtime.ComponentApkProvider"]
        self.assertEqual(len(providers),1)
        self.assertEqual(providers[0].get(a+"exported"),"false")
        self.assertEqual(providers[0].get(a+"grantUriPermissions"),"true")

    def test_runtime_navigation(self):
        screen=(SRC/"MainActivity.java").read_text()
        self.assertIn("Abrir Central de Componentes",screen)
        self.assertIn("ComponentCenterActivity.class",screen)
        self.assertIn("manageGraphicsOverlay(game)",screen)

    def test_pipeline_no_arbitrary_overwrites(self):
        store=(SRC/"runtime/ComponentStore.java").read_text()
        overlay=(SRC/"engine/GameGraphicsOverlay.java").read_text()
        for word in ("SHA-256","TarArchiveInputStream","PeInspector.inspect",
                     "ReleasePolicy.trustedRedirect","MAX_UNPACKED"):
            self.assertIn(word,store)
        for word in ("WINEDLLOVERRIDES","GameGraphicsOverlay.overrides"):
            self.assertIn(word,(SRC/"engine/ContainerSession.java").read_text())
        self.assertIn("!dll.isFile()",overlay)
        self.assertIn("Files.exists(target.toPath()",overlay)
        self.assertIn("Não sobrescrever",overlay)
        self.assertIn("getCanonicalFile",overlay)
        self.assertNotIn('Runtime.getRuntime().exec',store)
        self.assertNotIn('"/system/bin/sh"',store)

if __name__=="__main__":unittest.main()
