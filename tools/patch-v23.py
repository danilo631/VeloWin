#!/usr/bin/env python3
"""Replace destructive install/cleanup finalization in pinned Wine engine
with VeloWin-owned filesystem/transaction modules. Keep real Wine, XServer,
audio, Vulkan/DIS, asset extraction and native DRI3 unchanged.
"""
from pathlib import Path
import argparse

def one(text,old,new,label):
    count=text.count(old)
    if count!=1:
        raise RuntimeError(f"VeloWin v23 {label}: upstream changed ({count} anchors)")
    return text.replace(old,new)

def patch(source):
    image=source/"app/src/main/runtime/display/environment/ImageFsInstaller.java"
    code=image.read_text(encoding="utf-8")
    code=one(code,
'''    AppUtils.keepScreenOn(activity);
    ImageFs imageFs = ImageFs.find(activity);''',
'''    if (!dev.velowin.core.VeloInstallCoordinator.beginImageFs()) {
      Log.w("VeloWinInstaller", "ImageFS install already running");
      if (listener != null) listener.onFinished(false);
      return;
    }
    AppUtils.keepScreenOn(activity);
    ImageFs imageFs = ImageFs.find(activity);''',"ImageFS concurrency")
    code=one(code,
'''            () -> {
              progressTracker.start();
              clearRootDir(rootDir);

              boolean success =
                  extractImageFs(activity, rootDir, progressTracker.asExtractListener());''',
'''            () -> {
              try {
              progressTracker.start();
              boolean success;
              try {
                dev.velowin.core.VeloImageFsMaintenance.prepare(rootDir);
                success = true;
              } catch (java.io.IOException error) {
                Log.e("VeloWinInstaller", "ImageFS filesystem preparation failed", error);
                success = false;
              }
              if (success) {
                  success = extractImageFs(activity, rootDir, progressTracker.asExtractListener());
              }''',"VeloWin filesystem preparation")
    code=one(code,
'''              if (listener != null) listener.onFinished(success);
            });''',
'''              if (listener != null) listener.onFinished(success);
              } catch (Throwable error) {
                Log.e("VeloWinInstaller", "ImageFS installation aborted", error);
                if (listener != null) listener.onFinished(false);
              } finally {
                dev.velowin.core.VeloInstallCoordinator.endImageFs();
              }
            });''',"always release installation lock")
    start=code.index("  private static void clearRootDir(File rootDir) {")
    end=code.index("\n  private static void installGuestExtras(",start)
    code=code[:start]+code[end:] # Remove old destructive recursive deletion routine
    if "clearRootDir(rootDir)" in code:
        raise RuntimeError("legacy root cleanup still called")
    image.write_text(code,encoding="utf-8")

    content=source/"app/src/main/runtime/content/ContentsManager.java"
    code=content.read_text(encoding="utf-8")
    code=one(code,
'''    if (!installPath.mkdirs()) {
      callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
      return;
    }

    if (!getTmpDir(context).renameTo(installPath)) {
      callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
    }

    repairInstalledContentPermissions(profile);
    com.winlator.cmod.runtime.display.wayland.WineWaylandSupport.invalidate();
    callback.onSucceed(profile);''',
'''    try {
      dev.velowin.core.VeloContentTransaction.commit(getTmpDir(context), installPath);
    } catch (java.io.IOException error) {
      Log.e("VeloWinInstaller", "Component publish failed: "+profile.verName, error);
      callback.onFailed(InstallFailedReason.ERROR_UNKNOWN, null);
      return; // NEVER report success for a failed or partial rename.
    }
    repairInstalledContentPermissions(profile);
    com.winlator.cmod.runtime.display.wayland.WineWaylandSupport.invalidate();
    callback.onSucceed(profile);''',"atomic component publish")
    code=one(code,
'''  public void removeContent(ContentProfile profile) {
    FileUtils.delete(getInstallDir(context, profile));''',
'''  public void removeContent(ContentProfile profile) {
    String blocker=com.winlator.cmod.velowin.VeloInstalledRuntimeGuard.removalBlocker(
        context, profile);
    if (blocker != null) {
      Log.w("VeloWinInstaller", "Refused runtime removal: "+blocker);
      return;
    }
    FileUtils.delete(getInstallDir(context, profile));''',"guard in-use runtime removal")
    content.write_text(code,encoding="utf-8")
    print("VeloWin installer: old ImageFS clearRootDir and unsafe ContentsManager commit removed")
    print("VeloWin installer: own no-follow cleanup, atomic publish and runtime removal guard wired")

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--source",default="third_party/winnative")
    args=p.parse_args()
    patch(Path(args.source))
