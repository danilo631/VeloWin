package dev.velowin.app.engine;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.system.Os;
import android.util.Log;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Experimental native Xorg host in its OWN Android process.
 * Upstream Lorie calls exit() when dix_main ends. Never run it in the
 * VeloWin UI process or as a thread of the game execution supervisor.
 *
 * This service does NOT claim X11 is ready: the caller must verify a genuine
 * protocol-11 response on the shared private Unix socket before Wine starts.
 */
public final class EmbeddedX11Service extends Service {
    public static final String START="dev.velowin.app.x11.START";
    public static final String STOP="dev.velowin.app.x11.STOP";
    private final AtomicBoolean started=new AtomicBoolean(false);

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        String action=intent==null?"":intent.getAction();
        if(STOP.equals(action)){
            Log.i("VeloWin-X11","Stopping isolated Xorg service process.");
            stopSelf();
            // The upstream native Xorg owns additional threads and can call exit().
            // Kill ONLY this dedicated :velowin_x11 process, never the app UI.
            Process.killProcess(Process.myPid());
            return START_NOT_STICKY;
        }
        if(!START.equals(action))return START_NOT_STICKY;
        if(!started.compareAndSet(false,true))return START_NOT_STICKY;
        Thread worker=new Thread(()->{
            try{
                File workspace=BionicWorkspace.prepare(getApplicationContext());
                File tmp=new File(workspace,"tmp").getCanonicalFile();
                File xkb=EmbeddedXkbAssets.ensure(getApplicationContext());
                // The Wine session and upstream Xorg MUST use identical private TMPDIR.
                Os.setenv("TMPDIR",tmp.getAbsolutePath(),true);
                Os.setenv("XKB_CONFIG_ROOT",xkb.getAbsolutePath(),true);
                // Never introduce the Termux private prefix into an Android app.
                Os.unsetenv("LD_PRELOAD");
                Log.i("VeloWin-X11","Launching GPLv3 Xorg :0 with private XKB/TMPDIR");
                Looper.prepare(); // CmdEntryPoint initializes a Handler on this thread.
                // Unix socket only; no externally accessible TCP listener, no shell.
                com.termux.x11.CmdEntryPoint.main(new String[]{
                    ":0","-nolisten","tcp","-noreset"
                });
            }catch(Throwable failure){
                Log.e("VeloWin-X11","Xorg startup failed; refusing Wine GUI launch.",failure);
                stopSelf();
            }
        },"VeloWin-Lorie-Entry");
        worker.setDaemon(true);
        worker.start();
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        super.onDestroy();
        Log.i("VeloWin-X11","Xorg host service stopped.");
    }
    @Override public IBinder onBind(Intent intent){return null;}
}
