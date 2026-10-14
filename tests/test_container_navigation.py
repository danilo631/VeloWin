from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
APP=ROOT/"app/src/main/java/dev/velowin/app"

class ContainerNavigationContracts(unittest.TestCase):
    def test_manifest_private_files_activity(self):
        app=ET.parse(ROOT/"app/src/main/AndroidManifest.xml").getroot().find("application")
        ns="{http://schemas.android.com/apk/res/android}"
        activities=[a for a in app.findall("activity")
                    if a.get(ns+"name")==".ContainerFilesActivity"]
        self.assertEqual(len(activities),1)
        self.assertEqual(activities[0].get(ns+"exported"),"false")

    def test_per_game_container_opens_navigator(self):
        source=(APP/"ContainerLabActivity.java").read_text()
        self.assertIn("Explorar arquivos do contêiner",source)
        self.assertIn("this::openExplorer",source)
        self.assertIn("new Intent(this,ContainerFilesActivity.class)",source)
        self.assertIn('intent.putExtra("game_id",game.id)',source)

    def test_scoped_files_without_shell_or_arbitrary_intent_path(self):
        source=(APP/"ContainerFilesActivity.java").read_text()
        self.assertIn("new ContainerSession(this,game)",source)
        self.assertIn("new GameStore(this).all()",source)
        self.assertIn("session.sessionId()",source)
        self.assertIn('UUID.fromString(id)',source)
        self.assertIn('roots.put("cdrive"',source)
        self.assertIn('roots.put("logs"',source)
        self.assertIn('roots.put("wine"',source)
        self.assertIn("ContainerFileBrowser",source)
        self.assertIn("onBackPressed",source)
        self.assertNotIn("Runtime.getRuntime()",source)
        self.assertNotIn("ProcessBuilder",source)
        self.assertNotIn('getStringExtra("path")',source)

    def test_browser_fails_closed(self):
        source=(APP/"engine/ContainerFileBrowser.java").read_text()
        for expected in ("Files.isSymbolicLink", "getCanonicalFile",
                         "MAX_ENTRIES=300", "MAX_PREVIEW=16*1024",
                         "resolve(relative)", "boolean writable"):
            self.assertIn(expected,source)
        workflow=(ROOT/".github/workflows/android.yml").read_text()
        self.assertIn("ContainerFileBrowserTest",workflow)

if __name__=="__main__":
    unittest.main()
