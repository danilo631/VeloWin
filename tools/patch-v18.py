#!/usr/bin/env python3
"""VeloWin v18: patch only launch-boundary hooks; keep Winlator XServer code.

Root issue under investigation: XServer cursor renders while no guest X11
application window was mapped. The Winefile fallback, preflight diagnostics,
and first-window watchdog make that state visible and recoverable.

No fake container, duplicate XServer, or destructive prefix reset.
"""
from pathlib import Path
import argparse

def replace_once(s, before, after, description):
    count=s.count(before)
    if count!=1:
        raise RuntimeError(f"VeloWin v18 {description}: upstream ABI changed ({count})")
    return s.replace(before,after)

def patch(source):
    path=source/"app/src/main/runtime/display/XServerDisplayActivity.java"
    code=path.read_text(encoding="utf-8")
    old="""            if (envVars.has("EXTRA_EXEC_ARGS")) {
                args = envVars.get("EXTRA_EXEC_ARGS");
                envVars.remove("EXTRA_EXEC_ARGS");
            } else {
                args = "\\"wfm.exe\\"";
            }"""
    new="""            if (envVars.has("EXTRA_EXEC_ARGS")) {
                args = envVars.get("EXTRA_EXEC_ARGS");
                envVars.remove("EXTRA_EXEC_ARGS");
            } else {
                java.io.File windows = new java.io.File(container.getRootDir(),
                    ".wine/drive_c/windows");
                boolean hasWfm = new java.io.File(windows, "wfm.exe").isFile();
                boolean hasHandler = new java.io.File(windows, "winhandler.exe").isFile();
                boolean recovery = "1".equals(container.getExtra(
                    com.winlator.cmod.velowin.VeloBootTarget.MODE_KEY, "0"));
                args = com.winlator.cmod.velowin.VeloBootTarget.select(
                    hasWfm, hasHandler, recovery);
                android.util.Log.i("VeloWinSession", "desktop target=" + args
                    + " wfm=" + hasWfm + " handler=" + hasHandler
                    + " winefileRecovery=" + recovery);
            }"""
    code=replace_once(code,old,new,"desktop fallback")
    old='''        ensureWinePrefixEssentialFiles();

        String dxwrapper ='''
    new='''        ensureWinePrefixEssentialFiles();
        java.io.File desktopDir = new java.io.File(container.getRootDir(),
            ".wine/drive_c/windows");
        android.util.Log.i("VeloWinSession", "prefix prepared: wfm="
            + new java.io.File(desktopDir, "wfm.exe").isFile()
            + " winhandler=" + new java.io.File(desktopDir, "winhandler.exe").isFile()
            + " prefix=" + new java.io.File(container.getRootDir(), ".wine/system.reg").isFile());

        String dxwrapper ='''
    code=replace_once(code,old,new,"prefix probe after repair")
    old='''                        setupXEnvironment();
                    } catch (PackageManager.NameNotFoundException e) {'''
    new='''                        setupXEnvironment();
                        // Detect an XServer-only black screen, not a healthy Wine
                        // session: the pointer can render with zero guest windows.
                        new Handler(Looper.getMainLooper()).postDelayed(
                            this::checkVeloWinFirstWindow, 35000L);
                    } catch (PackageManager.NameNotFoundException e) {'''
    code=replace_once(code,old,new,"35-second no-window watchdog")
    insertion='''    private void onFirstGuestWindow() {'''
    method='''    private void checkVeloWinFirstWindow() {
        if (activityDestroyed.get() || isFinishing() || isDestroyed()
                || firstGuestWindowShown.get() || gamescopeMode || isDependencyInstall)
            return;
        android.util.Log.w("VeloWinSession", "X11 cursor/surface exists, "
            + "but no guest application window has mapped after 35 seconds");
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("VeloWin • nenhuma janela Windows")
            .setMessage("O XServer está aberto, mas nenhum aplicativo Wine "
                + "apresentou uma janela. Isso pode ocorrer com wfm.exe, "
                + "Box64/Wine ou driver gráfico. O cursor sozinho não confirma "
                + "que o contêiner iniciou. No VeloWin, teste o modo seguro "
                + "WineD3D/X11 e o fallback Winefile. Nenhum dado foi apagado.")
            .setPositiveButton("Diagnóstico", (dialog, which) -> {
                if (container != null) {
                    Intent diagnostic = new Intent(this,
                        com.winlator.cmod.velowin.VeloSettingsActivity.class);
                    diagnostic.putExtra("container_id", container.id);
                    startActivity(diagnostic);
                }
            })
            .setNeutralButton("Continuar", null)
            .setNegativeButton("Sair", (dialog, which) -> exit())
            .show();
    }

'''
    code=replace_once(code,insertion,method+insertion,"watchdog method")
    old='''            } else {
                WinToast.show(this, getString(R.string.preloader_launch_failed, message));
            }'''
    new='''            } else {
                com.winlator.cmod.shared.ui.dialog.ContentDialog.alert(
                    this, getString(R.string.preloader_launch_failed, message), this::exit);
            }'''
    code=replace_once(code,old,new,"persistent error on launch crash")
    if code.count("checkVeloWinFirstWindow")!=2:
        raise RuntimeError("unexpected watchdog call sites")
    path.write_text(code,encoding="utf-8")
    print("VeloWin v18: boot fallback, prefix verification, no-window watchdog,"
          " and persistent launch-failure dialog patched into real XServer Activity")

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--source",default="third_party/winnative")
    args=p.parse_args()
    patch(Path(args.source).resolve())
