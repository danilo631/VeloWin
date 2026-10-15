package dev.velowin.core;

import java.util.concurrent.atomic.AtomicBoolean;

/** Prevent overlapping extraction/upgrades and exit on every failure path.
 * DroidDeck's single-install-job concept adapted as independent VeloWin code.
 */
public final class VeloInstallCoordinator {
    private static final AtomicBoolean IMAGEFS_BUSY=new AtomicBoolean();
    private VeloInstallCoordinator(){}
    public static boolean beginImageFs(){return IMAGEFS_BUSY.compareAndSet(false,true);}
    public static void endImageFs(){IMAGEFS_BUSY.set(false);}
    public static boolean isImageFsBusy(){return IMAGEFS_BUSY.get();}
}
