package dev.velowin.app.hardware;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * JNI WSI preflight: VkSurfaceKHR, VkDevice and a real VkSwapchainKHR created
 * and destroyed on Android Surface; no frame presentation or Wine windows.
 */
public final class VulkanWsiLabActivity extends Activity implements SurfaceHolder.Callback {
    private static final boolean LOADED;
    static {
        boolean loaded=false;
        try { System.loadLibrary("velowin_probe"); loaded=true; }
        catch(UnsatisfiedLinkError | SecurityException error) {}
        LOADED=loaded;
    }
    private static native String probeSurface(Surface surface);

    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private TextView state;
    private SurfaceView view;
    private volatile boolean closed;
    private volatile int sequence;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24,24,24,24);
        root.setBackgroundColor(0xff0c0f14);
        TextView heading=new TextView(this);
        heading.setText("VeloWin · Vulkan Android Surface");
        heading.setTextSize(19);
        heading.setTextColor(Color.WHITE);
        root.addView(heading);
        state=new TextView(this);
        state.setTextColor(0xff9ec2ff);
        state.setTextSize(13);
        state.setPadding(0,16,0,16);
        state.setText("Iniciando Vulkan WSI...");
        root.addView(state);
        view=new SurfaceView(this);
        view.getHolder().addCallback(this);
        root.addView(view,new LinearLayout.LayoutParams(-1,0,1));
        TextView caveat=new TextView(this);
        caveat.setText("Este laboratório cria e destrói uma swapchain Vulkan numa Surface "
            +"Android real. Ainda NÃO apresenta quadros, não executa Wine e "
            +"não demonstra DXVK/VKD3D nem FPS de jogos.");
        caveat.setTextSize(12);
        caveat.setTextColor(0xff98a6b9);
        caveat.setPadding(0,16,0,16);
        root.addView(caveat);
        setContentView(root);
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        final int ticket=++sequence;
        if(!LOADED) {
            state.setText("Biblioteca nativa Vulkan não incluída neste APK.");
            return;
        }
        final Surface surface=holder.getSurface();
        worker.execute(()->{
            String report;
            try { report=probeSurface(surface); }
            catch(Throwable error) {
                report="Falha na ligação JNI Vulkan: "+error.getClass().getSimpleName();
            }
            final String text=report;
            runOnUiThread(()->{
                if(!closed && ticket==sequence && !isFinishing() && !isDestroyed())
                    state.setText(text);
            });
        });
    }
    @Override public void surfaceChanged(SurfaceHolder holder,int format,int width,int height) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        ++sequence;
    }
    @Override public void onDestroy() {
        closed=true;
        ++sequence;
        view.getHolder().removeCallback(this);
        worker.shutdown();
        super.onDestroy();
    }
}
