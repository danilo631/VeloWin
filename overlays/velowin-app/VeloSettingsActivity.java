package com.winlator.cmod.velowin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.winlator.cmod.runtime.container.Container;
import com.winlator.cmod.runtime.container.ContainerManager;
import com.winlator.cmod.runtime.content.ContentProfile;
import com.winlator.cmod.runtime.content.ContentsManager;
import com.winlator.cmod.runtime.display.environment.ImageFs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Original VeloWin per-container settings and diagnostic UI.
 * No WinNative/UnifiedActivity navigation. Uses selected Winlator-compatible
 * Container APIs only; package manager/installers remain separate services.
 * GPLv3 combined APK, VeloWin copyright.
 */
public final class VeloSettingsActivity extends Activity {
    public static final String CONTAINER_ID="container_id";
    private static final int IMPORT_FILE=703;
    private static final int BG=0xff0a1420, PANEL=0xff1c2c40, FG=0xffedf8ff;
    private static final int DIM=0xff9fb5c7, AQUA=0xff6bddc2;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private ContainerManager manager;
    private Container container;
    private LinearLayout column;

    private int px(int dip){return (int)(dip*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable panel(){
        GradientDrawable d=new GradientDrawable();
        d.setColor(PANEL);d.setCornerRadius(px(12));return d;
    }
    private void label(String s,int size,boolean heading){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);
        t.setTextColor(heading?AQUA:DIM);
        if(heading)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setPadding(px(3),px(9),px(3),px(7));
        column.addView(t);
    }
    private void button(String text,Runnable action){
        Button b=new Button(this);b.setText(text);b.setAllCaps(false);
        b.setTextColor(FG);b.setBackground(panel());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin=px(8);column.addView(b,p);
        b.setOnClickListener(v->action.run());
    }
    private void message(String title,String body){
        new AlertDialog.Builder(this).setTitle(title).setMessage(body)
            .setPositiveButton("OK",null).show();
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        manager=new ContainerManager(this);
        int id=getIntent().getIntExtra(CONTAINER_ID,-1);
        container=manager.getContainerById(id);
        if(container==null){
            message("Contêiner inexistente","O contêiner foi removido ou seu ID mudou.");
            finish();return;
        }
        showSettings();
    }
    private void choose(String title,String[] names,java.util.function.IntConsumer action) {
        new AlertDialog.Builder(this).setTitle(title).setItems(names,
            (d,n)->{try{action.accept(n);container.saveData();showSettings();}
                    catch(Exception e){message("Configuração",e.getMessage());}})
            .setNegativeButton("Cancelar",null).show();
    }
    private void showSettings(){
        ScrollView root=new ScrollView(this);root.setFillViewport(true);
        column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(px(18),px(12),px(18),px(24));
        column.setBackgroundColor(BG);root.addView(column);setContentView(root);
        label("V / CONFIGURAÇÕES",23,true);
        label(container.getName()+"  •  Wine: "+container.getWineVersion(),15,false);
        label("Sessão",16,true);
        button("Entrar no desktop Windows",()->{
            Intent i=new Intent(this,com.winlator.cmod.runtime.display.XServerDisplayActivity.class);
            i.putExtra(CONTAINER_ID,container.id);startActivity(i);
        });
        button("Modo seguro — corrigir tela preta",()->{
            new AlertDialog.Builder(this).setTitle("Recuperação de desktop")
                .setMessage("Desativa FrameGen, usa WineD3D, wrapper do sistema e "
                    +"X11 em 1280×720; define Winefile como gerenciador do desktop. "
                    +"Mantém os arquivos e saves do contêiner. As configurações "
                    +"gráficas anteriores poderão ser restauradas.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Aplicar",(d,n)->{
                    try{MaliProfile.apply(container);
                        container.putExtra(VeloBootTarget.MODE_KEY,"1");
                        container.setStartupSelection(Container.STARTUP_SELECTION_ESSENTIAL);
                        container.saveData();showSettings();
                    }catch(Exception e){message("Recuperação",e.getMessage());}
                }).show();
        });
        button("Voltar ao gerenciador de arquivos Winlator",()->{
            container.putExtra(VeloBootTarget.MODE_KEY,null);
            container.saveData();showSettings();
        });
        button("Restaurar perfil gráfico anterior",()->{
            try{
                MaliProfile.restore(container);container.putExtra(VeloBootTarget.MODE_KEY,null);
                container.saveData();showSettings();
            }catch(Exception e){message("Restaurar",e.getMessage());}
        });
        label("Vídeo e driver",16,true);
        label("Resolução "+container.getScreenSize()+" • Driver "+container.getGraphicsDriver()
             +" • DirectX "+container.getDXWrapper(),12,false);
        button("Resolução",()->choose("Resolução X11",
            new String[]{"960×540 (econômico)","1280×720 (equilibrado)",
                "1600×900","1920×1080"},
            idx->container.setScreenSize(new String[]{"960x540","1280x720","1600x900","1920x1080"}[idx])));
        button("Motor DirectX",()->choose("Tradução gráfica",
            new String[]{"WineD3D (OpenGL, recuperação)","DXVK + VKD3D (Vulkan, requer driver)"},
            idx->container.setDXWrapper(idx==0?"wined3d":"dxvk+vkd3d")));
        button("Servidor gráfico",()->choose("Servidor gráfico",
            new String[]{"X11 (compatível)","Wayland (exige suporte no runtime)"},
            idx->{
                if(idx==1&&"wrapper".equals(container.getGraphicsDriver()))
                    throw new IllegalStateException("Wayland requer driver compatível verificado");
                container.setDisplayBackend(idx==0
                    ?Container.DISPLAY_BACKEND_X11:Container.DISPLAY_BACKEND_WAYLAND);
            }));
        label("Wine, áudio e CPU",16,true);
        label("Emulador: "+container.getEmulator()+" • Áudio: "+container.getAudioDriver()+
              " • Box64: "+container.getBox64Preset(),12,false);
        button("Áudio",()->choose("Servidor de áudio",
            new String[]{"PulseAudio","ALSA"},idx->
                container.setAudioDriver(idx==0?"pulseaudio":"alsa")));
        button("Box64 — perfil",()->choose("Box64",
            new String[]{"Estabilidade","Compatibilidade","Desempenho"},idx->
                container.setBox64Preset(
                    new String[]{"STABILITY","COMPATIBILITY","PERFORMANCE"}[idx])));
        button("Serviços de inicialização do Wine",()->choose("Inicialização",
            new String[]{"Normal","Somente essenciais"},idx->
                container.setStartupSelection(idx==0
                    ?Container.STARTUP_SELECTION_NORMAL:Container.STARTUP_SELECTION_ESSENTIAL)));
        label("Arquivos do contêiner",16,true);
        button("Importar EXE ou arquivos para C:\\VeloWin-Imports",()->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");
            startActivityForResult(i,IMPORT_FILE);
        });
        button("Diagnóstico da tela preta",()->showDiagnostics());
        button("Componentes instalados",()->showComponents());
        label("As alterações são salvas neste contêiner. "
             +"Nenhum driver Adreno/Turnip é instalado automaticamente na Mali.",12,false);
    }
    private void showDiagnostics(){
        int id=container.id;
        worker.execute(()->{
            String result=VeloSessionDiagnostics.collect(this,container,manager);
            runOnUiThread(()->{if(isFinishing()||isDestroyed())return;
                new AlertDialog.Builder(this).setTitle("Diagnóstico contêiner #"+id)
                    .setMessage(result)
                    .setPositiveButton("OK",null)
                    .setNeutralButton("Copiar",(d,n)->{
                        android.content.ClipboardManager clip=(android.content.ClipboardManager)
                            getSystemService(CLIPBOARD_SERVICE);
                        if(clip!=null)clip.setPrimaryClip(android.content.ClipData
                            .newPlainText("VeloWin diagnóstico",result));
                    }).show();
            });
        });
    }
    private void showComponents(){
        worker.execute(()->{
            StringBuilder b=new StringBuilder();
            try{
                ContentsManager cm=new ContentsManager(getApplicationContext());
                cm.syncContents();
                for(ContentProfile.ContentType type:new ContentProfile.ContentType[]{
                    ContentProfile.ContentType.CONTENT_TYPE_BOX64,
                    ContentProfile.ContentType.CONTENT_TYPE_FEXCORE}){
                    b.append(type.name()).append(":\n");
                    List<ContentProfile> profiles=cm.getProfiles(type);
                    if(profiles==null||profiles.isEmpty())b.append("  Nenhum pacote cadastrado\n");
                    else for(ContentProfile p:profiles){
                        b.append("  ").append(p.verName)
                            .append(ContentsManager.isInstalled(this,p)?" [instalado]":" [ausente]")
                            .append("\n");
                    }
                }
                b.append("\nWine selecionado: ").append(container.getWineVersion());
            }catch(Exception e){b.append("Falha: ").append(e.getMessage());}
            String result=b.toString();
            runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())
                message("Componentes — VeloWin",result);});
        });
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=IMPORT_FILE||resultCode!=RESULT_OK||data==null
            ||data.getData()==null)return;
        Uri uri=data.getData();
        worker.execute(()->importToContainer(uri));
    }
    private void importToContainer(Uri uri){
        try{
            String filename="import.bin";
            try(Cursor cursor=getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
                if(cursor!=null&&cursor.moveToFirst())
                    filename=cursor.getString(0);
            }
            if(filename==null)filename="import.bin";
            filename=filename.replaceAll("[^A-Za-z0-9._-]","_");
            if(filename.startsWith(".")||filename.equals("..")||filename.length()>120)
                throw new IllegalArgumentException("Nome do arquivo recusado");
            File root=new File(container.getRootDir(),".wine/drive_c");
            File directory=new File(root,"VeloWin-Imports");
            if(Files.isSymbolicLink(root.toPath())
                ||Files.isSymbolicLink(directory.toPath()))
                throw new SecurityException("Destino simbólico recusado");
            if(!directory.isDirectory()&&!directory.mkdirs())
                throw new java.io.IOException("Não foi possível criar pasta");
            File finalFile=new File(directory,filename);
            File temp=new File(directory,filename+".partial");
            if(finalFile.exists()||Files.isSymbolicLink(finalFile.toPath())
                ||temp.exists()||Files.isSymbolicLink(temp.toPath()))
                throw new java.io.IOException("Arquivo existente; renomeie antes de importar");
            long copied=0;
            try(InputStream in=getContentResolver().openInputStream(uri);
                FileOutputStream out=new FileOutputStream(temp)){
                if(in==null)throw new java.io.IOException("Não foi possível ler o arquivo");
                byte[] buffer=new byte[65536];int n;
                while((n=in.read(buffer))!=-1){
                    copied+=n;
                    if(copied>512L*1024L*1024L)
                        throw new java.io.IOException("Limite de importação: 512 MB");
                    out.write(buffer,0,n);
                }
                out.getFD().sync();
            }catch(Exception e){temp.delete();throw e;}
            if(!temp.renameTo(finalFile)){
                temp.delete();throw new java.io.IOException("Falha na conclusão da importação");
            }
            final String details=filename+" • "+copied+" bytes importados em C:\\VeloWin-Imports";
            runOnUiThread(()->{if(!isFinishing())message("Importação concluída",details);});
        }catch(Exception e){
            final String err=e.getMessage();
            runOnUiThread(()->{if(!isFinishing())message("Importação",err);});
        }
    }
    @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
