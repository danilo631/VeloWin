from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/dev/velowin/app"
NS="{http://schemas.android.com/apk/res/android}"

class EmbeddedX11HostContracts(unittest.TestCase):
    def test_host_runs_in_separate_private_android_process(self):
        root=ET.parse(ROOT/"app/src/main/AndroidManifest.xml").getroot()
        app=root.find("application")
        services=[s for s in app.findall("service")
                  if s.get(NS+"name")==".engine.EmbeddedX11Service"]
        self.assertEqual(len(services),1)
        self.assertEqual(services[0].get(NS+"exported"),"false")
        self.assertEqual(services[0].get(NS+"process"),":velowin_x11")
        self.assertNotEqual(services[0].get(NS+"process"),app.get(NS+"process"))

    def test_native_engine_started_only_in_isolated_service(self):
        host=(JAVA/"engine/EmbeddedX11Service.java").read_text()
        self.assertIn("CmdEntryPoint.main",host)
        self.assertIn('Os.setenv("TMPDIR"',host)
        self.assertIn('Os.setenv("XKB_CONFIG_ROOT"',host)
        self.assertIn('"-nolisten","tcp"',host)
        self.assertIn("Process.killProcess(Process.myPid())",host)
        self.assertIn("EmbeddedXkbAssets.ensure",host)
        self.assertNotIn("Runtime.getRuntime().exec",host)

    def test_xserver_lifecycle_controls_and_fail_closed_wine(self):
        ui=(JAVA/"ContainerLabActivity.java").read_text()
        self.assertIn("Iniciar X11 NDK",ui)
        self.assertIn("Tela X11",ui)
        self.assertIn("Parar X11",ui)
        self.assertIn("handshakeEmbeddedX11",ui)
        session=(JAVA/"engine/ContainerSession.java").read_text()
        self.assertIn("X11Protocol.Reply.ACCEPTED",session)
        self.assertIn("wineCommand(guiWine,\"notepad\")",session)
        self.assertIn("LocalBridgeManager.handshakeEmbeddedX11(ctx)",session)
        self.assertNotIn("startService",session)

    def test_xkb_from_licensed_assets(self):
        source=(JAVA/"engine/EmbeddedXkbAssets.java").read_text()
        self.assertIn("rules/evdev",source)
        self.assertIn("symbols/us",source)
        self.assertIn("MAX_TOTAL",source)
        yml=(ROOT/".github/workflows/android.yml").read_text()
        self.assertIn("sudo apt-get install -y -qq xkb-data",yml)
        self.assertIn("xkb-data-copyright.txt",yml)
        self.assertIn("assets/xkb/rules/evdev",yml)

if __name__=="__main__":unittest.main()
