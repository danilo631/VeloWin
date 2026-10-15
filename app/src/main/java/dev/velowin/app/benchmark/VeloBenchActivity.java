package dev.velowin.app.benchmark;

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
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.net.Uri;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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
import dev.velowin.app.graphics.VeloSpatialUpscaler;
import dev.velowin.app.graphics.VeloTemporalFrameGen;
import dev.velowin.core.VeloBenchReport;
import dev.velowin.core.VeloFlowProfile;
import dev.velowin.core.VeloGpuPolicy;

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
    private static final int EXPORT_DOCUMENT = 701;
    private static final int MAX_HISTORY = 12;
    private volatile VeloBenchReport lastResult;
    private boolean exportCsv;
    private volatile float displayHz=60f;
    private volatile int thermalStatus=0;
    private PowerManager powerManager;
    private PowerManager.OnThermalStatusChangedListener thermalListener;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        displayHz=getWindowManager().getDefaultDisplay().getRefreshRate();
        powerManager=(PowerManager)getSystemService(POWER_SERVICE);
        if(Build.VERSION.SDK_INT>=29 && powerManager!=null){
            try {
                thermalStatus=powerManager.getCurrentThermalStatus();
                thermalListener=status->thermalStatus=status;
                powerManager.addThermalStatusListener(thermalListener);
            }catch(RuntimeException error){
                thermalStatus=4; // Unknown thermal state: fail closed for FG.
            }
        }
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
            b.setOnClickListener(v->{
                lastResult=null;
                renderer.startRun(cubes[index]);
                surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                hud.setText("Nova medição de "+cubes[index]+" cubos: até 30 s. "
                    +"Amostras anteriores descartadas.");
            });
        }
        root.addView(row);
        LinearLayout upscaleRow=new LinearLayout(this);
        VeloSpatialUpscaler.Mode[] choices={
            VeloSpatialUpscaler.Mode.NATIVE,VeloSpatialUpscaler.Mode.QUALITY,
            VeloSpatialUpscaler.Mode.BALANCED,VeloSpatialUpscaler.Mode.PERFORMANCE
        };
        for(VeloSpatialUpscaler.Mode choice:choices){
            Button toggle=new Button(this);
            toggle.setText(choice==VeloSpatialUpscaler.Mode.NATIVE?"Nativo":
                (int)(choice.scale*100)+"%");
            toggle.setTextSize(11);
            toggle.setAllCaps(false);
            upscaleRow.addView(toggle,new LinearLayout.LayoutParams(0,-2,1));
            toggle.setOnClickListener(v->{
                lastResult=null;
                renderer.selectUpscale(choice);
                surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                hud.setText("VeloSpatial: "+choice.label+" · nova medição");
            });
        }
        root.addView(upscaleRow);
        LinearLayout temporalRow=new LinearLayout(this);
        Button off=new Button(this);
        off.setAllCaps(false);off.setText("FG: Desativado");
        Button experimental=new Button(this);
        experimental.setAllCaps(false);experimental.setText("FG: Experimental");
        temporalRow.addView(off,new LinearLayout.LayoutParams(0,-2,1));
        temporalRow.addView(experimental,new LinearLayout.LayoutParams(0,-2,1));
        off.setOnClickListener(v->{
            lastResult=null;
            renderer.setFrameGen(false);
            surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            hud.setText("VeloFlow temporal desativado • nova medição");
        });
        experimental.setOnClickListener(v->{
            lastResult=null;
            renderer.setFrameGen(true);
            surface.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            hud.setText("VeloFlow temporal experimental • pares de quadros reais");
        });
        root.addView(temporalRow);
        Button stop=new Button(this);
        stop.setAllCaps(false);
        stop.setText("Encerrar teste e salvar histórico");
        stop.setOnClickListener(v->finishRun(renderer.getRunId(),"manual"));
        root.addView(stop);
        Button export=new Button(this);
        export.setAllCaps(false);
        export.setText("Exportar resultado • JSON / CSV");
        export.setOnClickListener(v->{
            if(lastResult==null){new AlertDialog.Builder(this)
                .setMessage("Encerre primeiro um teste para exportar dados medidos.")
                .setPositiveButton("OK",null).show();return;}
            new AlertDialog.Builder(this).setTitle("Exportar VeloBench")
                .setItems(new String[]{"JSON","CSV"},(d,index)->{
                    exportCsv=index==1;
                    Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType(exportCsv?"text/csv":"application/json");
                    intent.putExtra(Intent.EXTRA_TITLE,exportCsv?
                        "velowin-gpu-benchmark.csv":"velowin-gpu-benchmark.json");
                    startActivityForResult(intent,EXPORT_DOCUMENT);
                }).show();
        });
        root.addView(export);
        Button history=new Button(this);
        history.setAllCaps(false);
        history.setText("Histórico local (últimos 12)");
        history.setOnClickListener(v->{
            String stored=getSharedPreferences("velowin_gpu_lab",MODE_PRIVATE)
                .getString("bench_history","");
            new AlertDialog.Builder(this).setTitle("VeloBench • histórico JSONL")
                .setMessage(stored.isEmpty()?"Nenhum teste concluído.":stored)
                .setPositiveButton("Fechar",null).show();
        });
        root.addView(history);
        Button share=new Button(this);
        share.setAllCaps(false);
        share.setText("Salvar sugestão VeloFlow / copiar FPS");
        share.setOnClickListener(v->{
            String report=renderer.report();
            ClipboardManager clip=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            if(clip!=null)clip.setPrimaryClip(ClipData.newPlainText("VeloBench 3D",report));
            GpuBenchMetrics.Snapshot sample=lastResult==null?
                null:lastResult.metrics;
            if(sample==null||sample.sampleSize<80||lastResult.cubes<48
                ||renderer.isFrameGenOn()) {
                new AlertDialog.Builder(this).setTitle("Amostra insuficiente")
                    .setMessage("Execute o perfil Médio ou Intenso por pelo menos "
                        +"80 quadros medidos com FG experimental desligado. "
                        +"O relatório disponível foi copiado.")
                    .setPositiveButton("OK",null).show();
                return;
            }
            VeloFlowProfile.Quality suggestion=
                VeloFlowProfile.suggestQuality(sample.p95Ms,0);
            getSharedPreferences("velowin_gpu_lab",MODE_PRIVATE).edit()
                .putInt("quality_side",suggestion.minSide)
                .putLong("measured_at",System.currentTimeMillis())
                .putFloat("p95_frame_ms",sample.p95Ms)
                .putInt("cubes",lastResult.cubes)
                .putString("renderer",renderer.getGpuName()).apply();
            new AlertDialog.Builder(this).setTitle("Sugestão salva")
                .setMessage("VeloFlow sugerido: "+suggestion.label+
                    " ("+suggestion.minSide+" px). O resultado do teste "
                    +"GLES 3 foi copiado. O perfil não foi aplicado em "
                    +"nenhum jogo; use Qualidade VeloFlow no contêiner "
                    +"para decidir. Não mede interpolação DIS.")
                .setPositiveButton("OK",null).show();
        });
        root.addView(share);
        setContentView(root);
    }
    private void finishRun(int revision,String reason) {
        if(renderer==null || revision!=renderer.getRunId() ||
            lastResult!=null)return;
        renderer.halt();
        if(surface!=null)surface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        GpuBenchMetrics.Snapshot data=renderer.snapshot();
        if(!renderer.isCurrentRunReady()||data.sampleSize<2) {
            hud.setText("Nenhuma amostra válida. Execute novamente.");
            return;
        }
        lastResult=new VeloBenchReport(renderer.getGpuName()+" | "+renderer.getUpscaleName()
            +" | FG experimental="+renderer.isFrameGenOn(),renderer.getLoad(),
            System.currentTimeMillis(),renderer.elapsedMs(),reason,data);
        String previous=getSharedPreferences("velowin_gpu_lab",MODE_PRIVATE)
            .getString("bench_history","");
        String[] lines=previous.split("\\n");
        StringBuilder history=new StringBuilder(lastResult.toJson());
        for(int i=0;i<Math.min(MAX_HISTORY-1,lines.length);i++)
            if(!lines[i].isEmpty())history.append('\n').append(lines[i]);
        getSharedPreferences("velowin_gpu_lab",MODE_PRIVATE).edit()
            .putString("bench_history",history.toString()).apply();
        hud.setText(renderer.report()+"\nResultado salvo. Duração: "+
            lastResult.durationMs+" ms | motivo: "+reason);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=EXPORT_DOCUMENT||result!=RESULT_OK||data==null||
            data.getData()==null||lastResult==null)return;
        Uri uri=data.getData();
        try(OutputStream out=getContentResolver().openOutputStream(uri)){
            if(out==null)throw new java.io.IOException("Destino indisponível");
            out.write((exportCsv?lastResult.toCsv():lastResult.toJson())
                .getBytes(StandardCharsets.UTF_8));
        }catch(Exception e){
            new AlertDialog.Builder(this).setTitle("Erro na exportação")
                .setMessage(e.toString()).setPositiveButton("OK",null).show();
        }
    }
    private void showStats(String s) {
        runOnUiThread(()->{
            if(isFinishing()||isDestroyed()||hud==null)return;
            hud.setText(s);
        });
    }
    @Override public void onPause(){
        if(renderer!=null)renderer.halt();
        if(surface!=null){
            surface.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            surface.onPause();
        }
        super.onPause();
    }
    @Override public void onResume(){super.onResume();if(surface!=null)surface.onResume();}
    @Override protected void onDestroy(){
        if(Build.VERSION.SDK_INT>=29 && powerManager!=null && thermalListener!=null)
            powerManager.removeThermalStatusListener(thermalListener);
        surface=null;hud=null;super.onDestroy();
    }

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
        private volatile VeloSpatialUpscaler.Mode selectedUpscale=
            VeloSpatialUpscaler.Mode.NATIVE;
        private VeloSpatialUpscaler upscaler;
        private VeloTemporalFrameGen temporal;
        private volatile boolean frameGenRequested;
        private volatile long gameRenderedFrames,intermediateFrames,glCallbacks;
        private int outputWidth=1,outputHeight=1;
        private volatile int requestedRunId=1;
        private int activeRunId=0;
        private volatile boolean running=true;
        private volatile long startedNs=0;
        private volatile String glDriver="Aguardando contexto EGL";
        private volatile String fault="";
        private long previousFrameNs=0,lastPublishMs=0;
        private int program,uniformMvp,uniformModel;
        private int vertexBuffer,indexBuffer,vao;
        BenchRenderer(VeloBenchActivity host){this.host=host;}
        void setFrameGen(boolean enabled){
            frameGenRequested=enabled;
            requestedRunId++;
            running=true;
        }
        boolean isFrameGenOn(){return frameGenRequested;}
        void selectUpscale(VeloSpatialUpscaler.Mode mode){
            selectedUpscale=mode;
            requestedRunId++;
            running=true;
        }
        void startRun(int count){
            requestedLoad=Math.max(1,Math.min(128,count));
            requestedRunId++;
            running=true;
        }
        void halt(){running=false;}
        boolean isCurrentRunReady(){return activeRunId==requestedRunId;}
        int getRunId(){return requestedRunId;}
        long elapsedMs(){
            return startedNs==0?0:
                Math.max(0,(System.nanoTime()-startedNs)/1_000_000L);
        }
        int getLoad(){return requestedLoad;}
        String getGpuName(){return glDriver;}
        String getUpscaleName(){return selectedUpscale.label;}
        GpuBenchMetrics.Snapshot snapshot(){return metrics.snapshot();}
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
            // GL names from an earlier EGL context cannot be reused.
            program=0;vao=0;vertexBuffer=0;indexBuffer=0;
            upscaler=new VeloSpatialUpscaler();
            temporal=new VeloTemporalFrameGen();
            activeRunId=0;
            metrics.clear();
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
            outputWidth=Math.max(1,w);outputHeight=Math.max(1,h);
            GLES30.glViewport(0,0,w,h);
            float aspect=Math.max(1,w)/(float)Math.max(1,h);
            Matrix.perspectiveM(projection,0,55f,aspect,0.1f,80f);
            Matrix.setLookAtM(view,0,0,3.0f,11f,0,0,0,0,1,0);
        }
        @Override public void onDrawFrame(GL10 gl){
            if(program==0||!running)return;
            int revision=requestedRunId;
            long nowNs=System.nanoTime();
            if(activeRunId!=revision){
                activeRunId=revision;
                previousFrameNs=0;
                startedNs=nowNs;
                metrics.clear();
                if(temporal!=null)temporal.reset();
                gameRenderedFrames=intermediateFrames=glCallbacks=0;
            }
            boolean permitted=frameGenRequested &&
                VeloGpuPolicy.permitTemporal(outputWidth,outputHeight,
                    host.displayHz,host.thermalStatus);
            if(!permitted && temporal!=null && temporal.hasPending())temporal.reset();
            boolean replay=permitted && temporal!=null && temporal.hasPending();
            if(replay){
                temporal.presentLatest();
            }else{
            boolean scaled=upscaler!=null&&upscaler.begin(
                outputWidth,outputHeight,selectedUpscale);
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
            if(scaled)upscaler.present(selectedUpscale);
            gameRenderedFrames++;
            if(permitted && temporal!=null &&
               temporal.capture(outputWidth,outputHeight) &&
               temporal.presentIntermediate()){
                intermediateFrames++;
            }
            }
            glCallbacks++;
            // Wait for this batch: CPU/driver/GPU completion is part of the test.
            GLES30.glFinish();
            long doneNs=System.nanoTime();
            if(previousFrameNs!=0)metrics.addFrameNanos(doneNs-previousFrameNs);
            previousFrameNs=doneNs;
            long nowMs=SystemClock.uptimeMillis();
            if(nowNs-startedNs>=30_000_000_000L){
                running=false;
                host.runOnUiThread(()->host.finishRun(revision,"time_limit"));
            }
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
                "\nVeloFlow temporal: "+(frameGenRequested?"experimental":"off")+
                " | quadros 3D reais: "+gameRenderedFrames+
                " | intermediários calculados: "+intermediateFrames+
                " | callbacks GL: "+glCallbacks+
                "\nTela: "+host.displayHz+" Hz | térmico: "+host.thermalStatus+
                (frameGenRequested && !VeloGpuPolicy.permitTemporal(
                    outputWidth,outputHeight,host.displayHz,host.thermalStatus)?
                    " | FG bloqueado por limite térmico/memória/refresh":"")+
                (frameGenRequested && temporal!=null && !temporal.problem().isEmpty()?
                    "\nFG indisponível: "+temporal.problem():"")+
                "\nVeloSpatial: "+selectedUpscale.label+
                " | fonte: "+(upscaler==null?"nativo":upscaler.resolution())+
                (upscaler!=null&&!upscaler.fallback().isEmpty()?
                    "\nFallback GLES: "+upscaler.fallback():"")+
                "\n"+glDriver+
                "\nFPS médio: "+String.format(java.util.Locale.US,"%.1f",stats.averageFps)+
                " | 1% low: "+String.format(java.util.Locale.US,"%.1f",stats.lowOnePercentFps())+
                "\nTempo médio: "+String.format(java.util.Locale.US,"%.2f",stats.averageMs)+
                " ms | p95: "+String.format(java.util.Locale.US,"%.2f",stats.p95Ms)+" ms"+
                "\nAmostras: "+stats.sampleSize+" / "+stats.totalFrames+
                "\nVeloFlow sugerido para teste: "+suggestion.label+
                " ("+suggestion.minSide+" px)"+
                "\nCadência render GLES/CPU+GPU (glFinish), não FPS "
                    +"efetivamente apresentado pelo compositor."+
                "\nNão mede FrameGen DIS nem execução de jogos Wine.";
        }
    }
}
