#!/usr/bin/env python3
"""Small guarded hooks into *real* WinNative X11/Wine session.
Do not change XServer rendering, first-window recovery, audio, controllers
or upstream source without an exact matching GPLv3 pinned anchor.
"""
import argparse
from pathlib import Path

def exact(code,old,new,name):
    count=code.count(old)
    if count!=1:
        raise RuntimeError(f"VeloHybrid {name}: expected one anchor, got {count}")
    return code.replace(old,new)

def patch(source):
    file=source/"app/src/main/runtime/display/XServerDisplayActivity.java"
    code=file.read_text(encoding="utf-8")
    load_anchor="""        loadScreenEffects();
    }

    private void loadScreenEffects() {"""
    load_replacement="""        // VeloWin per-container override only; preserve shortcut/engine settings.
        // SGSR1 is a real Vulkan compositor effect shipped by this GPL engine.
        if (shortcut == null && container != null) {
            dev.velowin.core.VeloUpscalePlan.Settings velosr =
                com.winlator.cmod.velowin.VeloHybridGraphics.forLaunch(this,container);
            if (velosr.override) {
                sgsrEnabled = velosr.enabled;
                sgsrUpscaleMode = velosr.mode;
                sgsrSharpness = velosr.sharpness;
                android.util.Log.i("VeloHybrid", "SGSR1 enabled="+sgsrEnabled
                    +" mode="+sgsrUpscaleMode+" thermalFallback="+velosr.thermalFallback);
            }
        }
        loadScreenEffects();
    }

    private void loadScreenEffects() {"""
    code=exact(code,load_anchor,load_replacement,"Vulkan SGSR settings")
    launch_anchor="""        if (overrideEnvVars != null) {
            envVars.putAll(overrideEnvVars);
            overrideEnvVars.clear();
        }

        applyColorCursors();"""
    launch_replacement="""        if (overrideEnvVars != null) {
            envVars.putAll(overrideEnvVars);
            overrideEnvVars.clear();
        }
        // Mobox-style presets without replacing Wine/Box64 or existing FEX.
        // Only explicitly selected per-container Box64 recipes can override.
        if (!isDependencyInstall && shortcut == null && container != null) {
            com.winlator.cmod.velowin.VeloBox64Bridge.apply(container,envVars);
        }

        applyColorCursors();"""
    code=exact(code,launch_anchor,launch_replacement,"Wine/Box64 launch env")
    if "void setupXEnvironment()" not in code or "void checkVeloWinFirstWindow()" not in code:
        raise RuntimeError("real XServer and v18 recovered desktop must remain present")
    file.write_text(code,encoding="utf-8")
    print("VeloHybrid: real Wine/X11, SGSR1 Vulkan, Box64 and v18 window recovery preserved")

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--source",default="third_party/winnative")
    args=parser.parse_args()
    patch(Path(args.source))
