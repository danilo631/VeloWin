package com.winlator.cmod.velowin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import dev.velowin.core.GpuBenchMetrics;
import dev.velowin.core.VeloFlowProfile;

/**
 * VeloBench 3D: VeloWin original OpenGL ES 3 benchmark.
 * Inspired by community glmark2 geometry/vertex scene methodology (GPLv3).
 * Implements rotating lighted cubes, actual GPU draw calls and frame metrics.
 * NOT an embedded win32 game or a test of DIS interpolation itself.
 */
public final class VeloBenchActivity extends Activity {
    private static final int BG=0xff081320;
    private GLSurfaceView surface;
    private TextView hud;
    private BenchRenderer renderer;
    private boolean shown;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        TextView title=new TextView(this);
        title.setText("VELO / 3D GPU LAB");
        title.setTextColor(0xff69ddc0);
        title.setTextSize(20);
        title.setPadding(20,12,20,8);
        root.addView(title);
        hud=new TextView(this);
        hud.setTextColor(Color.WHITE);
        hud.setTextSize(13);
        hud.setPadding(18,9,18,9);
        hud.setText("Iniciando benchmark OpenGL ES 3.0...");
        root.addView(hud);
        surface=new GLSurfaceView(this);
        surface.setEGLContextClientVersion(3);
        surface.setEGLConfigChooser(8,8,8,8,24,0);
        surface.setPreserveEGLContextOnPause(true);
        renderer=new BenchRenderer(this);
        surface.setRenderer(renderer);
        surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        root.addView(surface,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout row=new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        String[] names={"Leve","Médio","Intenso"};
        int[] cubes={8,48,128};
        for(int i=0;i<names.length;i++){
            final int index=i;
            Button b=new Button(this);
            b.setAllCaps(false);
            b.setText(names[i]);
            b.setTextSize(12);
            row.addView(b,new LinearLayout.LayoutParams(0,-2,1));
            b.setOnClickListener(v->renderer.setLoad(cubes[index]));
        }
        root.addView(row);
        Button share=new Button(this);
        share.setAllCaps(false);
        share.setText("Copiar resultados / recomendação VeloFlow");
        share.setOnClickListener(v->{
            String report=renderer.report();
            ClipboardManager clip=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            if(clip!=null)clip.setPrimaryClip(ClipData.newPlainText("VeloBench 3D",report));
            new AlertDialog.Builder(this).setTitle("Resultado copiado")
                .setMessage("Relatório de desempenho e recomendações copiados. "
                    +"O FPS medido pertence ao OpenGL ES 3 nativo e não "
                    +"representa FPS interpolado pelo Wine/VeloFlow.")
                .setPositiveButton("OK",null).show();
        });
        root.addView(share);
        setContentView(root);
    }
    private void showStats(String s) {
        runOnUiThread(()->{
            if(isFinishing()||isDestroyed()||hud==null)return;
            hud.setText(s);
        });
    }
    @Override public void onPause(){if(surface!=null)surface.onPause();super.onPause();}
    @Override public void onResume(){super.onResume();if(surface!=null)surface.onResume();}
    @Override protected void onDestroy(){surface=null;hud=null;super.onDestroy();}

    private static final class BenchRenderer implements GLSurfaceView.Renderer {
        private static final String VS=
            "#version 300 es\n"
            +"layout(location=0) in vec3 pos;\n"
            +"layout(location=1) in vec3 norm;\n"
            +"uniform mat4 mvp;uniform mat4 model;\n"
            +"out vec3 vnormal;out vec3 vpos;\n"
            +"void main(){gl_Position=mvp*vec4(pos,1.);"
            +"vnormal=mat3(model)*norm;vpos=(model*vec4(pos,1.)).xyz;}\n";
        private static final String FS=
            "#version 300 es\nprecision highp float;\n"
            +"in vec3 vnormal;in vec3 vpos;out vec4 col;\n"
            +"void main(){vec3 n=normalize(vnormal);"
            +"vec3 l=normalize(vec3(0.3,0.7,0.6));"
            +"float diffuse=max(dot(n,l),0.0);"
            +"vec3 base=vec3(0.11,0.65,0.74);"
            +"col=vec4(base*(0.24+0.75*diffuse),1.0);}\n";
        private static final float[] VERTICES={
            // six independent faces; packed position + normal
            -0.5f,-0.5f,0.5f,0,0,1, 0.5f,-0.5f,0.5f,0,0,1,
            0.5f,0.5f,0.5f,0,0,1, -0.5f,0.5f,0.5f,0,0,1,
            0.5f,-0.5f,-0.5f,0,0,-1,-0.5f,-0.5f,-0.5f,0,0,-1,
            -0.5f,0.5f,-0.5f,0,0,-1,0.5f,0.5f,-0.5f,0,0,-1,
            -0.5f,-0.5f,-0.5f,-1,0,0,-0.5f,-0.5f,0.5f,-1,0,0,
            -0.5f,0.5f,0.5f,-1,0,0,-0.5f,0.5f,-0.5f,-1,0,0,
            0.5f,-0.5f,0.5f,1,0,0,0.5f,-0.5f,-0.5f,1,0,0,
            0.5f,0.5f,-0.5f,1,0,0,0.5f,0.5f,0.5f,1,0,0,
            -0.5f,0.5f,0.5f,0,1,0,0.5f,0.5f,0.5f,0,1,0,
            0.5f,0.5f,-0.5f,0,1,0,-0.5f,0.5f,-0.5f,0,1,0,
            -0.5f,-0.5f,-0.5f,0,-1,0,0.5f,-0.5f,-0.5f,0,-1,0,
            0.5f,-0.5f,0.5f,0,-1,0,-0.5f,-0.5f,0.5f,0,-1,0
        };
        private static final byte[] TRI={
            0,1,2,0,2,3,4,5,6,4,6,7,8,9,10,8,10,11,
            12,13,14,12,14,15,16,17,18,16,18,19,20,21,22,20,22,23
        };
        private final VeloBenchActivity host;
        private final GpuBenchMetrics metrics=new GpuBenchMetrics(240);
        private final float[] projection=new float[16],view=new float[16];
        private final float[] model=new float[16],mv=new float[16],mvp=new float[16];
        private volatile int requestedLoad=48;
        private volatile String glDriver="Aguardando contexto EGL";
        private volatile String fault="";
        private long previousFrameNs=0,lastPublishMs=0;
        private int program,uniformMvp,uniformModel;
        private int vertexBuffer,indexBuffer,vao;
        BenchRenderer(VeloBenchActivity host){this.host=host;}
        void setLoad(int count){requestedLoad=Math.max(1,Math.min(128,count));}
        private int shader(int type,String code){
            int id=GLES30.glCreateShader(type);
            GLES30.glShaderSource(id,code);
            GLES30.glCompileShader(id);
            int[] ok=new int[1];
            GLES30.glGetShaderiv(id,GLES30.GL_COMPILE_STATUS,ok,0);
            if(ok[0]==0){
                String error=GLES30.glGetShaderInfoLog(id);
                GLES30.glDeleteShader(id);
                throw new IllegalStateException("shader: "+error);
            }
            return id;
        }
        @Override public void onSurfaceCreated(GL10 gl,EGLConfig config){
            try {
                glDriver=GLES30.glGetString(GLES30.GL_RENDERER)+
                    " • "+GLES30.glGetString(GLES30.GL_VERSION);
                int a=shader(GLES30.GL_VERTEX_SHADER,VS);
                int b=shader(GLES30.GL_FRAGMENT_SHADER,FS);
                program=GLES30.glCreateProgram();
                GLES30.glAttachShader(program,a);
                GLES30.glAttachShader(program,b);
                GLES30.glLinkProgram(program);
                int[] linked=new int[1];
                GLES30.glGetProgramiv(program,GLES30.GL_LINK_STATUS,linked,0);
                GLES30.glDeleteShader(a);GLES30.glDeleteShader(b);
                if(linked[0]==0)throw new IllegalStateException(
                    "GPU link: "+GLES30.glGetProgramInfoLog(program));
                uniformMvp=GLES30.glGetUniformLocation(program,"mvp");
                uniformModel=GLES30.glGetUniformLocation(program,"model");
                FloatBuffer vertices=ByteBuffer.allocateDirect(VERTICES.length*4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
                vertices.put(VERTICES).position(0);
                ByteBuffer indices=ByteBuffer.allocateDirect(TRI.length)
                    .order(ByteOrder.nativeOrder());
                indices.put(TRI).position(0);
                int[] objects=new int[1];
                GLES30.glGenVertexArrays(1,objects,0);vao=objects[0];
                GLES30.glBindVertexArray(vao);
                GLES30.glGenBuffers(1,objects,0);vertexBuffer=objects[0];
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vertexBuffer);
                GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,VERTICES.length*4,vertices,
                    GLES30.GL_STATIC_DRAW);
                GLES30.glGenBuffers(1,objects,0);indexBuffer=objects[0];
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER,indexBuffer);
                GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,TRI.length,indices,
                    GLES30.GL_STATIC_DRAW);
                GLES30.glEnableVertexAttribArray(0);
                GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,24,0);
                GLES30.glEnableVertexAttribArray(1);
                GLES30.glVertexAttribPointer(1,3,GLES30.GL_FLOAT,false,24,12);
                GLES30.glBindVertexArray(0);
                GLES30.glEnable(GLES30.GL_DEPTH_TEST);
                GLES30.glClearColor(0.025f,0.05f,0.08f,1);
                fault="";
                previousFrameNs=0;
            }catch(Exception ex){
                fault=ex.getClass().getSimpleName()+": "+ex.getMessage();
                host.showStats("Benchmark indisponível: "+fault);
            }
        }
        @Override public void onSurfaceChanged(GL10 gl,int w,int h){
            GLES30.glViewport(0,0,w,h);
            float aspect=Math.max(1,w)/(float)Math.max(1,h);
            Matrix.perspectiveM(projection,0,55f,aspect,0.1f,80f);
            Matrix.setLookAtM(view,0,0,3.0f,11f,0,0,0,0,1,0);
        }
        @Override public void onDrawFrame(GL10 gl){
            if(program==0)return;
            long nowNs=System.nanoTime();
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT|GLES30.GL_DEPTH_BUFFER_BIT);
            GLES30.glUseProgram(program);
            GLES30.glBindVertexArray(vao);
            int count=requestedLoad;
            float seconds=SystemClock.uptimeMillis()/1000f;
            for(int i=0;i<count;i++){
                int col=i%8,row=(i/8)%8,layer=i/64;
                Matrix.setIdentityM(model,0);
                Matrix.translateM(model,0,(col-3.5f)*1.12f,(row-3.5f)*0.89f,layer*-1.1f);
                Matrix.rotateM(model,0,(seconds*29f+i*17f)%360f,0.4f,1,0.2f);
                Matrix.multiplyMM(mv,0,view,0,model,0);
                Matrix.multiplyMM(mvp,0,projection,0,mv,0);
                GLES30.glUniformMatrix4fv(uniformMvp,1,false,mvp,0);
                GLES30.glUniformMatrix4fv(uniformModel,1,false,model,0);
                GLES30.glDrawElements(GLES30.GL_TRIANGLES,TRI.length,
                    GLES30.GL_UNSIGNED_BYTE,0);
            }
            GLES30.glBindVertexArray(0);
            // Wait for this batch: CPU/driver/GPU completion is part of the test.
            GLES30.glFinish();
            long doneNs=System.nanoTime();
            if(previousFrameNs!=0)metrics.addFrameNanos(doneNs-previousFrameNs);
            previousFrameNs=doneNs;
            long nowMs=SystemClock.uptimeMillis();
            if(nowMs-lastPublishMs>=1200){
                lastPublishMs=nowMs;
                host.showStats(report());
            }
        }
        String report(){
            GpuBenchMetrics.Snapshot stats=metrics.snapshot();
            VeloFlowProfile.Quality suggestion=
                VeloFlowProfile.suggestQuality(stats.p95Ms,0);
            if(stats.sampleSize<20) return "Coletando quadros reais...\n"+glDriver+
                "\nCubos: "+requestedLoad+(fault.isEmpty()?"":"\nErro: "+fault);
            return "VeloBench GLES3 • "+requestedLoad+" cubos"+
                "\n"+glDriver+
                "\nFPS médio: "+String.format(java.util.Locale.US,"%.1f",stats.averageFps)+
                " | 1% low: "+String.format(java.util.Locale.US,"%.1f",stats.lowOnePercentFps())+
                "\nTempo médio: "+String.format(java.util.Locale.US,"%.2f",stats.averageMs)+
                " ms | p95: "+String.format(java.util.Locale.US,"%.2f",stats.p95Ms)+" ms"+
                "\nAmostras: "+stats.sampleSize+" / "+stats.totalFrames+
                "\nVeloFlow sugerido para teste: "+suggestion.label+
                " ("+suggestion.minSide+" px)"+
                "\nNão mede FPS interpolado nem execução de jogos Wine.";
        }
    }
}
