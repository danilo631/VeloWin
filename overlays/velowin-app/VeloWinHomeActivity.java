package com.winlator.cmod.velowin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.winlator.cmod.app.shell.UnifiedActivity;
import com.winlator.cmod.feature.setup.SetupWizardActivity;
import com.winlator.cmod.runtime.container.Container;
import com.winlator.cmod.runtime.container.ContainerCreation;
import com.winlator.cmod.runtime.container.ContainerManager;
import com.winlator.cmod.runtime.content.ContentProfile;
import com.winlator.cmod.runtime.content.ContentsManager;
import com.winlator.cmod.runtime.display.XServerDisplayActivity;
import com.winlator.cmod.runtime.display.environment.ImageFs;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * VeloWin's own GameHub-inspired front end, NOT upstream's WinNative UI.
 * Actual sessions continue through the attributed, source-linked Winlator
 * ContainerManager + Wine/Box64 + in-process XServer and Surface renderer.
 * Frame generation is provided by upstream's real OpenFlow/DIS Vulkan path.
 */
public final class VeloWinHomeActivity extends Activity {
    private static final int BG=0xff080e17, CARD=0xff152437;
    private static final int FG=0xffeaf4ff, MUTED=0xff96a9bb, ACCENT=0xff6bddc2;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private LinearLayout list,screen;
    private ContainerManager containers;
    private ContentsManager contents;
    private boolean working;

    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable panel(int color){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(14));return d;
    }
    private TextView text(String label,int size,int color,boolean strong){
        TextView t=new TextView(this);t.setText(label);t.setTextColor(color);t.setTextSize(size);
        if(strong)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    private void button(LinearLayout parent,String label,Runnable click) {
        Button b=new Button(this);b.setText(label);b.setAllCaps(false);
        b.setBackground(panel(CARD));b.setTextColor(FG);b.setTextSize(13);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(7);parent.addView(b,lp);
        b.setOnClickListener(v->click.run());
    }
    private void note(String title,String details){
        new AlertDialog.Builder(this).setTitle(title).setMessage(details)
            .setPositiveButton("OK",null).show();
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        renderLayout();
    }
    @Override public void onResume(){
        super.onResume();
        if(screen==null)return;
        if(!SetupWizardActivity.isSetupComplete(this)
            || !ImageFs.find(this).isUpToDate()){
            // The original wizard installs the real ImageFS/Wine components.
            buttonIfMissingSetup();
            return;
        }
        reload();
    }
    private void buttonIfMissingSetup(){
        list.removeAllViews();
        list.addView(text("Ambiente Windows ainda não instalado",17,FG,true));
        list.addView(text("O assistente instalará o ImageFS, Wine e os componentes "
            +"necessários antes da primeira sessão. Verifique espaço e conexão.",12,MUTED,false));
        button(list,"Instalar runtime / completar configuração",
            ()->startActivity(new Intent(this,SetupWizardActivity.class)));
    }
    private void renderLayout(){
        screen=new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setPadding(dp(16),dp(18),dp(16),dp(12));
        screen.setBackgroundColor(BG);
        screen.addView(text("V / VELOWIN",24,ACCENT,true));
        screen.addView(text("Seus jogos Windows, seu ambiente",13,MUTED,false));
        button(screen,"+ Criar contêiner Windows",this::createContainer);
        button(screen,"Componentes, drivers e biblioteca",
            ()->startActivity(new Intent(this,UnifiedActivity.class)));
        button(screen,"Atualizar lista",this::reload);
        ScrollView scroll=new ScrollView(this);
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0,dp(12),0,dp(20));
        scroll.addView(list);
        screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        screen.addView(text("Motor Winlator • FrameGen OpenFlow/DIS • "
            +"créditos e licenças nos arquivos do aplicativo",11,MUTED,false));
        setContentView(screen);
    }
    private void reload(){
        if(working)return;
        list.removeAllViews();
        list.addView(text("Carregando contêineres...",13,MUTED,false));
        io.execute(()->{
            ArrayList<Container> result=null;
            Throwable failure=null;
            try{
                contents=new ContentsManager(getApplicationContext());
                contents.syncContents();
                containers=new ContainerManager(getApplicationContext());
                result=new ArrayList<>(containers.getContainers());
            }catch(Throwable error){failure=error;}
            final ArrayList<Container> finalResult=result;
            final Throwable finalFailure=failure;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed())return;
                list.removeAllViews();
                if(finalFailure!=null){
                    note("Falha no ambiente",finalFailure.getMessage());return;
                }
                list.addView(text("CONTÊINERES  ·  "
                    +finalResult.size(),15,ACCENT,true));
                if(finalResult.isEmpty())list.addView(
                    text("Ainda não há contêiner. Use + Criar contêiner "
                        +"para criar um prefixo Wine de verdade.",13,MUTED,false));
                for(Container c:finalResult)addContainer(c);
            });
        });
    }
    private void addContainer(Container c) {
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(13),dp(12),dp(13),dp(13));card.setBackground(panel(CARD));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(11);
        list.addView(card,p);
        card.addView(text(c.getName(),17,FG,true));
        card.addView(text("Windows · "+c.getScreenSize()+" · "
            +c.getWineVersion()+" · X11",11,MUTED,false));
        String status="1".equals(c.getExtra("disFrameGen","0"))
            ?"OpenFlow/DIS "+c.getExtra("disFrameGenTargetFps","0")+" FPS alvo"
            :"FrameGen desativado";
        card.addView(text(status,12,ACCENT,false));
        button(card,"Entrar no contêiner / Desktop Wine",()->launch(c));
        button(card,"Frame Generation e desempenho",()->graphics(c));
        button(card,"Abrir arquivos e opções avançadas",
            ()->startActivity(new Intent(this,UnifiedActivity.class)));
    }
    private void createContainer(){
        if(working){note("Operação ativa","A criação anterior ainda está em execução.");return;}
        if(!SetupWizardActivity.isSetupComplete(this)
            ||!ImageFs.find(this).isUpToDate()){
            buttonIfMissingSetup();return;
        }
        if(contents==null||containers==null){
            note("Aguarde","O gerenciador está preparando os componentes instalados.");return;
        }
        final ContentProfile runtime=ContainerCreation.newestInstalledRuntime(contents);
        if(runtime==null){
            note("Wine/Proton ausente","Instale um runtime Wine ou Proton pelo "
                +"gerenciador de componentes antes de criar um contêiner.");
            return;
        }
        EditText input=new EditText(this);
        input.setSingleLine(true);input.setText("VeloWin Game");
        new AlertDialog.Builder(this)
            .setTitle("Criar contêiner com Wine/Proton instalado")
            .setMessage("Um prefixo Windows real será criado a partir de "
                +runtime.verName+". Nenhum conteúdo do jogo será excluído.")
            .setView(input).setNegativeButton("Cancelar",null)
            .setPositiveButton("Criar",(d,w)->{
                String requested=input.getText().toString().trim();
                if(requested.isEmpty()||requested.length()>60
                    ||!requested.matches("[A-Za-z0-9À-ÿ _.-]+")){
                    note("Nome inválido","Use até 60 letras, números, espaços, _ . ou -.");
                    return;
                }
                working=true;
                final String name=ContainerCreation.uniqueName(containers,requested);
                final String version=ContentsManager.getEntryName(runtime);
                ContainerCreation.createContainerAsync(this,containers,contents,name,
                    version, created->{
                        working=false;
                        if(isFinishing()||isDestroyed())return;
                        if(created==null)note("Falha criando prefixo",
                            "Veja os logs do runtime e a instalação do Wine/Box64.");
                        reload();
                    });
            }).show();
    }
    private void launch(Container c){
        if(c==null || containers==null){note("Falha","Contêiner indisponível.");return;}
        if(!ImageFs.find(this).isUpToDate()){
            buttonIfMissingSetup();return;
        }
        // Actual Wine + Box64 + XServer + Android Surface execution, not a console stub.
        Intent desktop=new Intent(this,XServerDisplayActivity.class);
        desktop.putExtra("container_id",c.id);
        startActivity(desktop);
    }
    private void graphics(Container c){
        if(working)return;
        if(c.isGamescopeRuntime()) {
            note("Modo gráfico incompatível",
                "GameScope utiliza runtime Linux/Wayland. Abra suas opções avançadas "
                +"para configurar o compositor; este perfil OpenFlow/DIS é para "
                +"contêineres Windows com X11.");
            return;
        }
        boolean vulkan=getPackageManager().hasSystemFeature(
            PackageManager.FEATURE_VULKAN_HARDWARE_VERSION);
        boolean active="1".equals(c.getExtra("disFrameGen","0"));
        String[] options={"Desativado","OpenFlow/DIS — 2× adaptativo",
            "OpenFlow/DIS — alvo 60 FPS","OpenFlow/DIS — alvo 90 FPS",
            "OpenFlow/DIS — alvo 120 FPS"};
        new AlertDialog.Builder(this).setTitle("VeloWin FrameGen · "+c.getName())
            .setMessage("Backend real: Vulkan OpenFlow/DIS do motor gráfico. "
                +"Ativar solicita interpolação; não comprova FPS dobrado. "
                +(vulkan?"Vulkan detectado.":"O Android não anunciou Vulkan — "
                    +"a geração ficará indisponível até confirmar o driver.")+" "
                +"X11 recomendado para Mali. Não utiliza shaders proprietários LSFG.")
            .setSingleChoiceItems(options,active
                ? selectedFrameGenItem(c.getExtra("disFrameGenTargetFps","0")):0,
                (dialog,which)->{
                if(which!=0&&!vulkan){
                    note("Vulkan não detectado","Mantenha FrameGen desativado "
                        +"ou verifique os drivers no menu avançado.");return;
                }
                int[] targets={0,0,60,90,120};
                int fps=FrameGenPolicy.targetFps(targets[which]);
                if(which==0){
                    c.putExtra("disFrameGen","0");
                    c.putExtra("frameGen","0");
                }else{
                    c.putExtra("frameGen","0"); // LSFG and DIS are mutually exclusive.
                    c.putExtra("disFrameGen","1");
                    c.putExtra("disFrameGenScale","70");
                    c.putExtra("disFrameGenTargetFps",String.valueOf(fps));
                }
                c.setDisplayBackend(Container.DISPLAY_BACKEND_X11);
                c.saveData();dialog.dismiss();reload();
            }).setNegativeButton("Cancelar",null).show();
    }
    private int selectedFrameGenItem(String fps) {
        switch(fps) {
            case "60": return 2;
            case "90": return 3;
            case "120": return 4;
            default: return 1;
        }
    }
    @Override protected void onDestroy(){
        io.shutdownNow();super.onDestroy();
    }
}
