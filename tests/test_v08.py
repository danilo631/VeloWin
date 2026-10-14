from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parents[1]
APP=ROOT/"app/src/main/java/dev/velowin/app"

class VeloWinV08Test(unittest.TestCase):
    def test_localhost_only_bridge(self):
        cfg=(APP/"engine/BridgeProfile.java").read_text()
        self.assertIn('"127.0.0.1:"',cfg)
        self.assertIn('"tcp:127.0.0.1:4713"',cfg)
        self.assertIn('display > 9',cfg)
        probe=(APP/"engine/LocalBridgeManager.java").read_text()
        self.assertIn('new InetSocketAddress("127.0.0.1",port)',probe)
        self.assertIn('client.connect',probe)
        self.assertIn("X11Protocol.setupRequest()",probe)
        self.assertIn("X11Protocol.parseReply",probe)
        self.assertIn('getSharedPreferences',probe)

    def test_guided_real_processes(self):
        session=(APP/"engine/ContainerSession.java").read_text()
        for item in ('runGuided(cancelled,feedback)',"Etapa 1/5",
                     'wineCommand(wine,"cmd","/c","ver")',
                     'ownSmokeBinary(cancelled)',
                     'Win32ProbeVerdict.passed','bridges().environment()',
                     'WineLaunchEnvironment.build'):
            self.assertIn(item,session)
        self.assertNotIn("new ProcessBuilder",session)

    def test_gui_optin_and_explicit_consent(self):
        ui=(APP/"ContainerLabActivity.java").read_text()
        for name in ("Configurar X11 local","Áudio Pulse local",
                     "Diagnóstico guiado","Testar portas locais",
                     "configureX11()","configurePulse()"):
            self.assertIn(name,ui)
        grammar=(APP/"engine/ContainerCommand.java").read_text()
        self.assertIn('action == Action.DIAGNOSE',grammar)
        self.assertIn('case "bridge status"',grammar)
        self.assertIn('case "diagnose"',grammar)

    def test_release_metadata(self):
        gradle=(ROOT/"app/build.gradle").read_text()
        self.assertIn("versionName '0.8.0-dev'",gradle)
        pipeline=(ROOT/".github/workflows/android.yml").read_text()
        self.assertIn("velowin-v08-bionic-bridge-debug",pipeline)
        self.assertIn("BridgeProfileTest",pipeline)
        self.assertIn("Win32ProbeVerdictTest",pipeline)
