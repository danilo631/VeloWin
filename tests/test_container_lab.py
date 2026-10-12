from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
APP=ROOT/"app/src/main/java/dev/velowin/app"
class ContainerLabContracts(unittest.TestCase):
    def test_private_activity_and_navigation(self):
        manifest=ET.parse(ROOT/"app/src/main/AndroidManifest.xml").getroot()
        ns="{http://schemas.android.com/apk/res/android}"
        activities=manifest.find("application").findall("activity")
        console=[x for x in activities if x.attrib.get(ns+"name")==".ContainerLabActivity"]
        self.assertEqual(len(console),1)
        self.assertEqual(console[0].attrib.get(ns+"exported"),"false")
        main=(APP/"MainActivity.java").read_text()
        self.assertIn("Entrar no contêiner",main)
        self.assertIn("openContainer(game)",main)
        self.assertIn("openContainer(null)",main)

    def test_restricted_commands_no_shell(self):
        parser=(APP/"engine/ContainerCommand.java").read_text()
        self.assertIn("requiresConsent",parser)
        self.assertIn("case \"wineboot -u\"",parser)
        self.assertIn("case \"run exe\"",parser)
        core=(APP/"engine/ContainerSession.java").read_text()
        self.assertIn("BoundedProcess.run",core)
        self.assertIn("BionicWineInstaller.installed",core)
        self.assertIn("WinePrefixManager.templateReady",core)
        self.assertIn("PeInspector.inspect",core)
        self.assertIn('if(!"content".equals(uri.getScheme()))',core)
        self.assertNotIn("Runtime.getRuntime()",core)
        self.assertNotIn("new ProcessBuilder",core)
        self.assertNotIn('"/system/bin/sh"',core)
        self.assertNotIn('"-c"',core)

    def test_diagnostics_and_consent(self):
        ui=(APP/"ContainerLabActivity.java").read_text()
        for needle in ("Compartilhar log","Cancelar teste","wineserver",
                       "requiresConsent","ContainerCommand.Action.PREFIX_PREPARE","setPositiveButton"):
            self.assertIn(needle,ui)
        self.assertIn("versionName '0.7.0-dev'",
            (ROOT/"app/build.gradle").read_text())
