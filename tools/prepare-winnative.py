#!/usr/bin/env python3
"""
Prepare the *actual* consolidated WinNative/Winlator Android runtime as VeloWin.

This script edits only the pinned git submodule working tree before Gradle builds.
The original GPLv3 upstream sources remain publicly traceable via their gitlink;
no native/X11 implementation is reinvented or replaced with a diagnostic console.
"""
import argparse
from pathlib import Path
import re
import shutil
import subprocess
import sys

UPSTREAM_SHA = "8409085f706acbc0749881d9bf1e85f2f347dfd4"
APPLICATION_ID = "dev.velowin.app"
APP_LABEL = "VeloWin"

def fail(message):
    raise SystemExit("VeloWin upstream integration: " + message)

def replace_exact(path: Path, old: str, new: str, count: int):
    data = path.read_text(encoding="utf-8")
    actual = data.count(old)
    if actual == 0 and data.count(new) == count:
        return
    if actual != count:
        fail(f"{path}: expected {count} anchors {old!r}, got {actual}")
    path.write_text(data.replace(old, new), encoding="utf-8")

def prepare(source: Path, overlay: Path):
    source = source.resolve()
    overlay = overlay.resolve()
    if not source.is_dir() or not (source / "LICENSE").is_file():
        fail("WinNative source checkout is missing. Run git submodule update --init --recursive third_party/winnative")
    try:
        commit = subprocess.check_output(
            ["git", "-C", str(source), "rev-parse", "HEAD"], text=True
        ).strip()
    except (OSError, subprocess.CalledProcessError):
        fail("Could not verify the WinNative git revision")
    if commit != UPSTREAM_SHA:
        fail(f"Refusing unpinned code: expected {UPSTREAM_SHA}, got {commit}")
    license_text = (source / "LICENSE").read_text(encoding="utf-8")
    if "GNU GENERAL PUBLIC LICENSE" not in license_text or "Version 3" not in license_text:
        fail("Pinned engine does not carry expected GPLv3 source license")
    if not (source / "app/src/main/runtime/display/XServerDisplayActivity.java").is_file():
        fail("Missing real X11 Surface/renderer component")
    if not (source / "app/src/main/runtime/container/ContainerManager.java").is_file():
        fail("Missing real container manager")
    if not (source / "app/src/main/runtime/display/environment/components/XServerComponent.java").is_file():
        fail("Missing real X11 server component")

    gradle = source / "app/build.gradle"
    replace_exact(gradle, 'applicationId "com.winnative.cmod"',
                  'applicationId "' + APPLICATION_ID + '"', 2)

    translations = list((source / "app/src/main/res").glob("values*/strings.xml"))
    if not translations:
        fail("No Android string resource files")
    changed = 0
    pattern = re.compile(r'(<string name="common_ui_app_name">)([^<]*)(</string>)')
    for file in translations:
        raw = file.read_text(encoding="utf-8")
        new, hits = pattern.subn(lambda m: m.group(1) + APP_LABEL + m.group(3), raw)
        if hits:
            file.write_text(new, encoding="utf-8")
            changed += hits
    if changed == 0:
        fail("No application name strings matched")
    # Preserve upstream package/java namespace! The applicationId is intentionally
    # independent, so BuildConfig, ContentProviders and manifest placeholders
    # should refer to Android's real installed package.
    manifests = [source / "app/src/main/AndroidManifest.xml"]
    for p in manifests:
        if '@string/common_ui_app_name' not in p.read_text(encoding="utf-8"):
            fail("Launcher must use the localized app name resource")
    # VeloWin's own application-facing shell. Keep the battle-tested
    # XServer/renderer/ContainerManager from the Winlator-derived runtime,
    # but do NOT ship a WinNative UI copy as the Android launcher.
    # An independently compilable :velowin-core Java library, owned by VeloWin.
    # Replacing Wine/XServer later only requires a new VeloEnginePort adapter.
    original_core = overlay / "modules/velowin-core"
    copied_core = source / "velowin-core"
    if not (original_core / "build.gradle").is_file():
        fail("VeloWin independent core module is missing")
    shutil.copytree(original_core, copied_core, dirs_exist_ok=True)
    settings = source / "settings.gradle"
    settings_text = settings.read_text(encoding="utf-8")
    if "include ':velowin-core'" not in settings_text:
        settings.write_text(settings_text+"\ninclude ':velowin-core'\n",
            encoding="utf-8")
    app_gradle = source / "app/build.gradle"
    app_code = app_gradle.read_text(encoding="utf-8")
    anchor = "    implementation project(':framegen')"
    if app_code.count(anchor)!=1:
        fail("Upstream Gradle engine project anchor changed")
    app_gradle.write_text(app_code.replace(anchor,
        "    implementation project(':velowin-core')\n"+anchor),encoding="utf-8")
    overlay_sources = overlay / "overlays/velowin-app"
    java_target = source / "app/src/main/app/velowin"
    java_target.mkdir(parents=True, exist_ok=True)
    for filename in ("VeloWinHomeActivity.java", "FrameGenPolicy.java", "MaliProfile.java",
                     "VeloRuntimePlan.java", "VeloRuntimeBridge.java", "VeloBootTarget.java",
                     "VeloSettingsActivity.java", "VeloSessionDiagnostics.java",
                     "VeloBenchActivity.java", "VeloEngineAdapter.java",
                     "VeloSessionJournal.java", "VeloHybridGraphics.java", "VeloBox64Bridge.java",
                     "VeloInstalledRuntimeGuard.java", "VeloRuntimeInstallActivity.java"):
        src = overlay_sources / filename
        if not src.is_file():
            fail("Missing original VeloWin UI module: " + filename)
        shutil.copyfile(src, java_target / filename)
    manifest_file = source / "app/src/main/AndroidManifest.xml"
    manifest = manifest_file.read_text(encoding="utf-8")
    original_launcher = re.compile(
        r'(<activity\b[^>]*android:name="com\.winlator\.cmod\.app\.shell\.UnifiedActivity"'
        r'[^>]*>)(.*?)(</activity>)', re.DOTALL
    )
    matched = original_launcher.search(manifest)
    if matched is None:
        fail("Upstream UnifiedActivity changed; cannot safely replace launcher.")
    original_body = matched.group(2)
    main_filter = re.compile(
        r'<intent-filter>\s*<action android:name="android\.intent\.action\.MAIN"\s*/>'
        r'\s*<category android:name="android\.intent\.category\.LAUNCHER"\s*/>'
        r'\s*</intent-filter>\s*', re.DOTALL
    )
    if len(main_filter.findall(original_body)) != 1:
        fail("Expected exactly one original launcher intent filter.")
    original_body = main_filter.sub("", original_body, count=1)
    manifest = (manifest[:matched.start(2)] + original_body
                + manifest[matched.end(2):])
    new_launcher = """
        <!-- Original VeloWin UI. Full Winlator-derived runtime remains a
             source-linked engine; VeloWin Settings has its own controls. -->
        <activity
            android:name="com.winlator.cmod.velowin.VeloWinHomeActivity"
            android:exported="true"
            android:theme="@style/AppTheme"
            android:screenOrientation="fullUser">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
"""
    new_launcher += """
        <activity
            android:name="com.winlator.cmod.velowin.VeloSettingsActivity"
            android:exported="false"
            android:theme="@style/AppTheme"
            android:screenOrientation="fullUser"/>
"""
    new_launcher += """
        <activity
            android:name="com.winlator.cmod.velowin.VeloBenchActivity"
            android:exported="false"
            android:theme="@style/AppTheme"
            android:screenOrientation="sensorLandscape"/>
"""
    new_launcher += """
        <activity
            android:name="com.winlator.cmod.velowin.VeloRuntimeInstallActivity"
            android:exported="false"
            android:theme="@style/AppTheme"
            android:screenOrientation="fullUser"/>
"""
    if manifest.count("</application>") != 1:
        fail("Android manifest application node changed.")
    manifest = manifest.replace("</application>", new_launcher + "    </application>")
    # A single launcher is essential for predictable home/back navigation.
    if manifest.count('android.intent.category.LAUNCHER') != 1:
        fail("Multiple launcher Activities after VeloWin UI integration.")
    manifest_file.write_text(manifest, encoding="utf-8")

    # The upstream setup wizard normally returns to the WinNative Home.
    # Keep all installation/runtime logic, redirect ONLY post-setup navigation
    # to VeloWin's own Home, avoiding two launcher dashboards in the task stack.
    wizard = source / "app/src/main/feature/setup/SetupWizardActivity.kt"
    wizard_code = wizard.read_text(encoding="utf-8")
    old_finish = """            Intent(this, UnifiedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),"""
    new_finish = """            Intent(this, com.winlator.cmod.velowin.VeloWinHomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),"""
    if wizard_code.count(old_finish) != 1:
        fail("Upstream wizard finish routing changed; refusing ambiguous patch.")
    wizard.write_text(wizard_code.replace(old_finish,new_finish), encoding="utf-8")
    # Keep all upstream notices with the binary distributed to end users.
    assets = source / "app/src/main/assets/velowin/legal"
    assets.mkdir(parents=True, exist_ok=True)
    for filename in ("LICENSE", "CREDITS.md", "EMULATOR_CREDITS.md"):
        original = source / filename
        if not original.is_file():
            fail(f"GPL source attribution file is missing: {filename}")
        shutil.copyfile(original, assets / ("WinNative-" + filename))
    prior = overlay / "LICENSE"
    if prior.is_file():
        shutil.copyfile(prior, assets / "VeloWin-original-MIT-LICENSE.txt")
    note = (
        "VeloWin Native — consolidated runtime\n"
        "Application ID: dev.velowin.app\n"
        "Source: https://github.com/WinNative-Emu/WinNative\n"
        "Pinned WinNative commit: " + UPSTREAM_SHA + "\n"
        "Combined VeloWin/WinNative APK distribution: GNU GPL version 3.\n"
        "Original VeloWin MIT portions retain their copyright notice.\n"
        "Full corresponding sources and build scripts:\n"
        "https://github.com/danilo631/VeloWin\n"
        "Winlator, Mesa, DXVK, Wine, FEX, Box64, Termux:X11 and further authors\n"
        "are credited in WinNative-CREDITS.md / WinNative-EMULATOR_CREDITS.md.\n"
        "Termux:X11 is a study/reference checkout; the embedded display\n"
        "in this flavor comes from WinNative's XServer, not Termux:Lorie.\n"
    )
    (assets / "VELO-WIN-NOTICE.txt").write_text(note, encoding="utf-8")
    print(f"VeloWin: full native graphical engine pinned to {commit}")
    print("VeloWin: package=" + APPLICATION_ID +
          "; Android app name=" + APP_LABEL + f"; labels patched={changed}")
    print("VeloWin: original GameHub-style HomeActivity + actual container and OpenFlow/DIS renderer linked.")
    print("VeloWin: XServer, Wine container manager and GPL credits confirmed.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", default="third_party/winnative")
    parser.add_argument("--root", default=".")
    args = parser.parse_args()
    prepare(Path(args.source), Path(args.root))
