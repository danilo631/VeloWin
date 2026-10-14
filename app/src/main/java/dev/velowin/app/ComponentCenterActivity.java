package dev.velowin.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import dev.velowin.app.runtime.ComponentCatalog;
import dev.velowin.app.runtime.ComponentRegistry;
import dev.velowin.app.runtime.ComponentRelease;
import dev.velowin.app.runtime.ComponentStore;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Separate trusted component catalog: downloads are not driver activation.
 * Wine Bionic downloads continue through the existing Runtimes screen.
 */
public final class ComponentCenterActivity extends Activity {
    private static final int BG=0xff0b1018, PANEL=0xff182331, ACCENT=0xff88d7bd;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancel=new AtomicBoolean(false);
    private boolean running;
    private LinearLayout rows;

    private int dp(int n){return Math.round(getResources().getDisplayMetrics().density*n);}
    private TextView label(String s,int size,int color) {
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);
        t.setTextColor(color);return t;
    }
    private GradientDrawable background(int color){
        GradientDrawable b=new GradientDrawable();
        b.setColor(color);b.setCornerRadius(dp(12));return b;
    }
    private void addButton(LinearLayout parent,String name,Runnable callback){
        Button b=new Button(this);b.setText(name);b.setAllCaps(false);
        b.setTextSize(12);b.setTextColor(Color.WHITE);b.setBackground(background(PANEL));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(6);parent.addView(b,p);b.setOnClickListener(v->callback.run());
    }
    private void card(String heading,String description,
                      ComponentRelease.Kind kind) {
        LinearLayout body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(14),dp(13),dp(14),dp(13));
        body.setBackground(background(PANEL));
        LinearLayout.LayoutParams box=new LinearLayout.LayoutParams(-1,-2);
        box.bottomMargin=dp(13);rows.addView(body,box);
        body.addView(label(heading,17,Color.WHITE));
        TextView note=label(description,12,0xffa0b0bf);
        LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);
        np.topMargin=dp(5);body.addView(note,np);
        ComponentRelease found=ComponentRegistry.get(this,kind);
        if(found!=null) {
            TextView saved=label("Pacote SHA-256 verificado: "+found.tag,12,ACCENT);
            body.addView(saved);
            if(kind==ComponentRelease.Kind.TERMUX_X11_APK){
                addButton(body,"Instalar APK externo (confirmação Android)",
                    ()->offerApk(found));
                addButton(body,"Abrir aplicativo Termux:X11 instalado",this::openX11External);
            }else {
                addButton(body,"Inspecionar bibliotecas do pacote",
                    ()->report(kind,found));
            }
            addButton(body,"Apagar componente armazenado",
                ()->confirmRemoval(kind,found));
        }
        addButton(body,"Buscar versão oficial e baixar",()->lookup(kind));
    }
    private void message(String title,String text){
        new AlertDialog.Builder(this).setTitle(title).setMessage(text)
            .setPositiveButton("OK",null).show();
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        render();
    }
    private void render(){
        LinearLayout page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(15),dp(15),dp(15),dp(10));
        page.setBackgroundColor(BG);
        TextView title=label("V / CENTRAL DE COMPONENTES",18,Color.WHITE);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);page.addView(title);
        TextView intro=label("Pacotes oficiais com SHA-256. Baixar não habilita automaticamente o motor gráfico.",12,0xffa0b0bf);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,-2);
        ip.topMargin=dp(7);ip.bottomMargin=dp(15);
        page.addView(intro,ip);
        ScrollView scroll=new ScrollView(this);
        rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        card("DXVK — Direct3D 9/10/11",
            "DLLs Windows x86/x64. Disponíveis para teste isolado; exigem Wine, X11, GPU Vulkan e override das DLLs.",
            ComponentRelease.Kind.DXVK);
        card("VKD3D-Proton — Direct3D 12",
            "Pacote x86/x64 com tradução D3D12→Vulkan. Requisitos reais variam com GPU, extensões e versão.",
            ComponentRelease.Kind.VKD3D_PROTON);
        card("Termux:X11 — servidor X.org externo",
            "APK oficial experimental. Instalar este APK separado não cria automaticamente servidor X11 interno nem liga seu socket ao Wine do VeloWin.",
            ComponentRelease.Kind.TERMUX_X11_APK);
        LinearLayout more=new LinearLayout(this);more.setOrientation(LinearLayout.VERTICAL);
        rows.addView(more);
        addButton(more,"WineHQ e Wine Bionic",()->message("Wine", 
            "Wine Bionic verificado da GameNative já está disponível na aba Runtimes. "
            +"Releases WineHQ usuais são fontes ou Linux/glibc, não binários executáveis diretamente no Android/Bionic."));
        addButton(more,"Mesa / drivers GPU",()->message("Mesa",
            "Mesa/Turnip exigem binário Vulkan Android, ICD, ABI e GPU compatíveis. "
            +"O VeloWin não baixa nem substitui drivers indiscriminadamente. Mali usa driver do sistema. "
            +"Adreno só aceitará builds validadas depois da integração do loader."));
        addButton(more,"WineD3D (OpenGL)",()->message("WineD3D",
            "WineD3D é parte da distribuição Wine instalada; não é necessário baixar DXVK para usá-lo. "
            +"A renderização real depende da ponte X11 e de OpenGL do Wine."));
        addButton(page,"Voltar aos runtimes",this::finish);
        setContentView(page);
    }
    private void lookup(ComponentRelease.Kind kind) {
        if(running){message("Operação ativa","Aguarde o término do comando atual.");return;}
        running=true;
        ProgressDialog dialog=new ProgressDialog(this);
        dialog.setTitle("Catálogo oficial");
        dialog.setMessage("Consultando releases GitHub...");
        dialog.setCancelable(false);dialog.show();
        worker.execute(()->{
            ComponentRelease entry=null;Exception error=null;
            try{entry=ComponentCatalog.latest(kind);}catch(Exception failure){error=failure;}
            final ComponentRelease release=entry;final Exception problem=error;
            runOnUiThread(()->{
                running=false;dialog.dismiss();
                if(isFinishing()||isDestroyed())return;
                if(problem!=null){message("Falha no catálogo",problem.getMessage());return;}
                new AlertDialog.Builder(this)
                    .setTitle("Obter "+release.name+"?")
                    .setMessage("Fonte: "+release.url+"\nTamanho: "+release.size/1048576
                        +" MiB\nSHA-256: "+release.sha256+"\n"
                        +"O pacote será verificado e guardado no armazenamento privado. "
                        +"Nenhum driver será ativado automaticamente.")
                    .setNegativeButton("Cancelar",null)
                    .setPositiveButton("Baixar", (d,w)->download(release)).show();
            });
        });
    }
    private void download(ComponentRelease release) {
        if(running)return;
        running=true;cancel.set(false);
        ProgressDialog dialog=new ProgressDialog(this);
        dialog.setTitle("Baixar "+release.name);
        dialog.setMessage("Preparando...");
        dialog.setCancelable(false);
        dialog.setButton(ProgressDialog.BUTTON_NEGATIVE,"Cancelar",(d,w)->cancel.set(true));
        dialog.show();
        worker.execute(()->{
            Exception error=null;
            try {
                ComponentStore.downloadAndPrepare(getApplicationContext(),release,cancel,
                    s->runOnUiThread(()->{
                        if(!isFinishing()&&!isDestroyed())dialog.setMessage(s);
                    }));
                ComponentRegistry.put(getApplicationContext(),release);
            }catch(Exception failure){error=failure;}
            final Exception result=error;
            runOnUiThread(()->{
                running=false;dialog.dismiss();
                if(isFinishing()||isDestroyed())return;
                render();
                message(result==null?"Componente verificado":"Falha ao instalar pacote",
                    result==null?"Arquivos autenticados pelo SHA-256 e armazenados. "
                        +"Não há ativação automática no Wine ou no driver Android."
                        :result.getMessage());
            });
        });
    }
    private void report(ComponentRelease.Kind kind,ComponentRelease e) {
        message("Bibliotecas armazenadas",e.kind+" "+e.tag
            +"\nValidação: arquitetura Windows PE x64 e DLLs essenciais confirmadas."
            +"\nAinda não há prova de renderização DXVK/VKD3D.");
    }
    private void confirmRemoval(ComponentRelease.Kind kind,ComponentRelease e) {
        if(running)return;
        new AlertDialog.Builder(this).setTitle("Excluir "+e.name+"?")
            .setMessage("Somente o pacote na Central de Componentes será excluído; "
                +"jogos e prefixos Wine não serão alterados.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Excluir",(d,w)->{
                running=true;
                worker.execute(()->{
                    Exception error=null;
                    try{
                        ComponentStore.remove(getApplicationContext(),e);
                        ComponentRegistry.forget(getApplicationContext(),kind);
                    }catch(Exception fail){error=fail;}
                    final Exception result=error;
                    runOnUiThread(()->{
                        running=false;if(isFinishing()||isDestroyed())return;
                        render();
                        message(result==null?"Componente removido":"Falha",
                            result==null?"Pacote removido; prefixos preservados.":result.getMessage());
                    });
                });
            }).show();
    }
    private void openX11External() {
        Intent launch=getPackageManager().getLaunchIntentForPackage("com.termux.x11");
        if(launch==null){
            message("X11 externo","Termux:X11 não está instalado ou não há Activity visível.");
            return;
        }
        try{startActivity(launch);}
        catch(Exception error){message("X11 externo",error.getMessage());}
    }
    private void offerApk(ComponentRelease e) {
        new AlertDialog.Builder(this).setTitle("Termux:X11 externo")
            .setMessage("O Android instalará o aplicativo oficial com confirmação. "
                +"Isso NÃO executa automaticamente Wine e o X11 ainda não estará integrado "
                +"ao VeloWin sem um bridge de socket.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Continuar",(d,w)->{
                if(running)return;
                running=true;
                worker.execute(()->{
                    boolean verified=false;
                    try{verified=ComponentStore.verifyApk(getApplicationContext(),e);}
                    catch(Exception ignored){}
                    final boolean ok=verified;
                    runOnUiThread(()->{
                        running=false;
                        if(isFinishing()||isDestroyed())return;
                        if(!ok){message("Integridade","Falha SHA-256 do APK.");return;}
                        if(Build.VERSION.SDK_INT>=26
                            && !getPackageManager().canRequestPackageInstalls()){
                            new AlertDialog.Builder(this).setTitle("Permitir instalador externo?")
                                .setMessage("O Android precisa autorizar a instalação de APKs "
                                    +"da origem VeloWin. Volte e pressione instalar novamente "
                                    +"após conceder a permissão.")
                                .setNegativeButton("Cancelar",null)
                                .setPositiveButton("Configurar",(settingsDialog,settingsButton)->{
                                    Intent settings=new Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:"+getPackageName()));
                                    startActivity(settings);
                                }).show();
                            return;
                        }
                        Intent intent=new Intent(Intent.ACTION_INSTALL_PACKAGE);
                        intent.setDataAndType(Uri.parse("content://dev.velowin.app.componentapk/termux-x11"),
                            "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        try{startActivity(intent);}
                        catch(Exception failure){
                            message("Instalador indisponível",failure.getMessage());
                        }
                    });
                });
            }).show();
    }
    @Override protected void onDestroy(){
        cancel.set(true);worker.shutdownNow();super.onDestroy();
    }
}
