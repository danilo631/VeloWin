package dev.velowin.app.hardware;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Locale;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Native Android OpenGL ES Surface proof-of-concept, independent of Windows/Wine.
 * Never claims that a successful frame proves DXVK, VKD3D or Vulkan WSI support.
 */
public final class GpuSurfaceLabActivity extends Activity {
    private GLSurfaceView surface;
    private TextView statistics;
    private volatile boolean stopped;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout column=new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setBackgroundColor(0xff0c0f14);
        statistics=new TextView(this);
        statistics.setPadding(24,20,24,20);
        statistics.setTextColor(0xffedf4ff);
        statistics.setTextSize(14);
        statistics.setText("Inicializando superfície OpenGL ES 2.0…");
        column.addView(statistics);
        surface=new GLSurfaceView(this);
        surface.setEGLContextClientVersion(2);
        surface.setPreserveEGLContextOnPause(true);
        surface.setRenderer(new TriangleRenderer());
        surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        column.addView(surface,new LinearLayout.LayoutParams(-1,0,1));
        TextView warning=new TextView(this);
        warning.setText("Laboratório Android: desenha em Surface OpenGL ES. Não renderiza Wine, DXVK ou jogos Windows.");
        warning.setTextColor(0xff9aa9be);
        warning.setPadding(20,12,20,22);
        warning.setGravity(Gravity.CENTER);
        column.addView(warning);
        setContentView(column);
    }
    @Override protected void onPause(){super.onPause();surface.onPause();}
    @Override protected void onResume(){super.onResume();if(surface!=null)surface.onResume();}
    @Override protected void onDestroy(){stopped=true;super.onDestroy();}
    private final class TriangleRenderer implements GLSurfaceView.Renderer {
        private final String vertex="attribute vec2 aPos; uniform float uAngle; varying vec2 vColor; "
            +"void main(){ float s=sin(uAngle),c=cos(uAngle);"
            +"vec2 p=vec2(aPos.x*c-aPos.y*s,aPos.x*s+aPos.y*c);"
            +"gl_Position=vec4(p,0.0,1.0);vColor=aPos*0.5+0.5;}";
        private final String fragment="precision mediump float;varying vec2 vColor;"
            +"void main(){gl_FragColor=vec4(vColor.x,0.45+0.4*vColor.y,0.95,1.0);}";
        private final FloatBuffer vertices;
        private int program;
        private int position,rotation;
        private int total;
        private long started;
        private long lastReport;
        private float smoothedMs;
        TriangleRenderer(){
            float[] triangle={0f,0.6f,-0.56f,-0.38f,0.56f,-0.38f};
            vertices=ByteBuffer.allocateDirect(triangle.length*4).order(ByteOrder.nativeOrder())
                .asFloatBuffer();
            vertices.put(triangle).position(0);
        }
        private int compile(int type,String source){
            int shader=GLES20.glCreateShader(type);
            GLES20.glShaderSource(shader,source);
            GLES20.glCompileShader(shader);
            int[] ok=new int[1];
            GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,ok,0);
            if(ok[0]==0){
                String error=GLES20.glGetShaderInfoLog(shader);
                GLES20.glDeleteShader(shader);
                throw new IllegalStateException("Shader: "+error);
            }
            return shader;
        }
        @Override public void onSurfaceCreated(GL10 gl,EGLConfig cfg){
            int vs=compile(GLES20.GL_VERTEX_SHADER,vertex);
            int fs=compile(GLES20.GL_FRAGMENT_SHADER,fragment);
            program=GLES20.glCreateProgram();
            GLES20.glAttachShader(program,vs);
            GLES20.glAttachShader(program,fs);
            GLES20.glLinkProgram(program);
            GLES20.glDeleteShader(vs);
            GLES20.glDeleteShader(fs);
            int[] ok=new int[1];
            GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,ok,0);
            if(ok[0]==0)throw new IllegalStateException(GLES20.glGetProgramInfoLog(program));
            position=GLES20.glGetAttribLocation(program,"aPos");
            rotation=GLES20.glGetUniformLocation(program,"uAngle");
            GLES20.glClearColor(0.06f,0.09f,0.13f,1);
            started=SystemClock.elapsedRealtimeNanos();
            lastReport=started;
            total=0;
            smoothedMs=0;
        }
        @Override public void onSurfaceChanged(GL10 gl,int width,int height){
            GLES20.glViewport(0,0,width,height);
        }
        @Override public void onDrawFrame(GL10 gl){
            long now=SystemClock.elapsedRealtimeNanos();
            float time=(now-started)/1000000000f;
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glUseProgram(program);
            GLES20.glEnableVertexAttribArray(position);
            GLES20.glVertexAttribPointer(position,2,GLES20.GL_FLOAT,false,0,vertices);
            GLES20.glUniform1f(rotation,time);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,3);
            GLES20.glDisableVertexAttribArray(position);
            total++;
            long elapsed=now-lastReport;
            if(elapsed>=1000000000L){
                float fps=total*1000000000f/elapsed;
                final String label=String.format(Locale.ROOT,
                    "OpenGL ES: renderização de teste\nTaxa observada: %.1f quadros/s\n"
                    +"Apenas Surface Android; não é benchmark de jogo.",fps);
                if(!stopped)runOnUiThread(()->{
                    if(!isFinishing()&&!isDestroyed())statistics.setText(label);
                });
                total=0;
                lastReport=now;
            }
        }
    }
}
