package dev.velowin.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import dev.velowin.app.engine.ContainerCommand;
import dev.velowin.app.engine.ContainerSession;
import dev.velowin.app.engine.BionicWorkspace;
import dev.velowin.app.engine.BridgeProfile;
import dev.velowin.app.engine.LocalBridgeManager;
import dev.velowin.app.library.Game;
import dev.velowin.app.library.GameStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interactive, bounded Android/Bionic container diagnostics.
 * The restricted console does NOT expose a Linux shell.
 * Wine/Windows process tests require an explicit action.
 */
public final class ContainerLabActivity extends Activity {
    private static final int BACKGROUND=0xff0b1018;
    private static final int PANEL=0xff182331;
    private static final int ACCENT=0xff88d7bd;
    private static final int MUTED=0xffa0b0bf;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final AtomicBoolean cancelled=new AtomicBoolean(false);
    private Game game;
    private ContainerSession session;
    private TextView screen;
    private TextView subtitle;
    private ScrollView scroll;
    private EditText input;
    private volatile boolean running=false;
    private final StringBuilder transcript=new StringBuilder();

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density*v);
    }
    private TextView text(String s,int size,int color,boolean bold) {
        TextView v=new TextView(this);
        v.setText(s);v.setTextSize(size);v.setTextColor(color);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    private GradientDrawable background(int color) {
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);d.setCornerRadius(dp(12));return d;
    }
    private Button button(String label,Runnable callback,boolean accent) {
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(12);
        b.setTextColor(accent?BACKGROUND:Color.WHITE);
        b.setBackground(background(accent?ACCENT:PANEL));
        b.setPadding(dp(8),dp(10),dp(8),dp(10));
        b.setOnClickListener(v->callback.run());
        return b;
    }
    private void labelRow(LinearLayout root,String a,ContainerCommand.Action ca,
                          String b,ContainerCommand.Action cb) {
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams full=new LinearLayout.LayoutParams(-1,-2);
        full.topMargin=dp(6);
        root.addView(row,full);
        Button left=button(a,()->dispatch(ca),false);
        Button right=button(b,()->dispatch(cb),false);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1f);
        lp.rightMargin=dp(6);
        row.addView(left,lp);
        row.addView(right,new LinearLayout.LayoutParams(0,-2,1f));
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        String gameId=getIntent().getStringExtra("game_id");
        if(gameId!=null) {
            try {
                for(Game candidate:new GameStore(this).all()) {
                    if(candidate.id.equals(gameId)){game=candidate;break;}
                }
            } catch(Exception ignored) {}
        }
        session=new ContainerSession(this,game);
        LinearLayout main=new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(13),dp(12),dp(13),dp(10));
        main.setBackgroundColor(BACKGROUND);

        TextView title=text("V / CONTÊINER BIONIC",19,Color.WHITE,true);
        title.setLetterSpacing(0.06f);
        main.addView(title);
        subtitle=text(game==null?"Laboratório técnico · sem jogo"
            :game.title+" · espaço Windows privado",12,MUTED,false);
        LinearLayout.LayoutParams subtitleParams=new LinearLayout.LayoutParams(-1,-2);
        subtitleParams.topMargin=dp(4);subtitleParams.bottomMargin=dp(12);
        main.addView(subtitle,subtitleParams);

        LinearLayout actions=new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        main.addView(actions);
        Button explore=button("📁 Explorar arquivos do contêiner",this::openExplorer,true);
        LinearLayout.LayoutParams exploreParams=new LinearLayout.LayoutParams(-1,-2);
        exploreParams.bottomMargin=dp(8);
        actions.addView(explore,exploreParams);
        labelRow(actions,"Status",ContainerCommand.Action.STATUS,
            "Box64 -v",ContainerCommand.Action.BOX64_VERSION);
        labelRow(actions,"Auditar Wine",ContainerCommand.Action.WINE_PREFLIGHT,
            "Status do prefixo",ContainerCommand.Action.PREFIX_STATUS);
        labelRow(actions,"Wine --version",ContainerCommand.Action.WINE_VERSION,
            "Preparar prefixo",ContainerCommand.Action.PREFIX_PREPARE);
        labelRow(actions,"CMD /c ver",ContainerCommand.Action.CMD_VERSION,
            "Testar Win32",ContainerCommand.Action.WIN32_SMOKE);
        labelRow(actions,"Wineboot -u",ContainerCommand.Action.WINEBOOT,
            "Arquivos",ContainerCommand.Action.FILES);
        labelRow(actions,"Testar EXE",ContainerCommand.Action.RUN_EXE,
            "Encerrar Wine",ContainerCommand.Action.STOP_WIN_SERVER);
        labelRow(actions,"Diagnóstico guiado",ContainerCommand.Action.DIAGNOSE,
            "Testar portas locais",ContainerCommand.Action.BRIDGE_PROBE);
        LinearLayout bridgeRow=new LinearLayout(this);
        bridgeRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bridgeParams=new LinearLayout.LayoutParams(-1,-2);
        bridgeParams.topMargin=dp(7);
        actions.addView(bridgeRow,bridgeParams);
        Button x11=button("Configurar X11 local",this::configureX11,false);
        LinearLayout.LayoutParams xl=new LinearLayout.LayoutParams(0,-2,1f);
        xl.rightMargin=dp(6);
        bridgeRow.addView(x11,xl);
        bridgeRow.addView(button("Áudio Pulse local",this::configurePulse,false),
            new LinearLayout.LayoutParams(0,-2,1f));

        LinearLayout consoleArea=new LinearLayout(this);
        consoleArea.setOrientation(LinearLayout.VERTICAL);
        consoleArea.setPadding(dp(12),dp(12),dp(12),dp(12));
        consoleArea.setBackground(background(PANEL));
        LinearLayout.LayoutParams consoleBox=new LinearLayout.LayoutParams(-1,0,1f);
        consoleBox.topMargin=dp(12);consoleBox.bottomMargin=dp(9);
        main.addView(consoleArea,consoleBox);
        TextView caption=text("● CONSOLE LOCAL  ·  COMANDOS RESTRITOS",11,ACCENT,true);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);
        cp.bottomMargin=dp(7);
        consoleArea.addView(caption,cp);

        scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        screen=text("",12,0xffd1f5e5,false);
        screen.setTypeface(Typeface.MONOSPACE);
        screen.setTextIsSelectable(true);
        screen.setLineSpacing(dp(3),1.0f);
        scroll.addView(screen);
        consoleArea.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));

        LinearLayout inputRow=new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);
        rowParams.bottomMargin=dp(7);
        main.addView(inputRow,rowParams);
        input=new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(MUTED);
        input.setBackground(background(PANEL));
        input.setHint("help, status, wine --version...");
        input.setTextSize(13);
        input.setPadding(dp(10),dp(9),dp(10),dp(9));
        LinearLayout.LayoutParams editor=new LinearLayout.LayoutParams(0,-2,1f);
        editor.rightMargin=dp(7);
        inputRow.addView(input,editor);
        input.setOnEditorActionListener((v,action,event)->{
            submitInput();return true;
        });
        inputRow.addView(button("Executar",this::submitInput,true));

        LinearLayout bottom=new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        main.addView(bottom);
        Button cancel=button("Cancelar teste",()->{
            if(running){cancelled.set(true);append("\n[Cancelamento solicitado]\n");}
            else append("\n[Nenhum processo ativo]\n");
        },false);
        Button share=button("Compartilhar log",this::shareLog,false);
        Button close=button("Voltar",this::finish,false);
        LinearLayout.LayoutParams part=new LinearLayout.LayoutParams(0,-2,1f);
        part.rightMargin=dp(5);bottom.addView(cancel,part);
        LinearLayout.LayoutParams part2=new LinearLayout.LayoutParams(0,-2,1f);
        part2.rightMargin=dp(5);bottom.addView(share,part2);
        bottom.addView(close,new LinearLayout.LayoutParams(0,-2,1f));

        setContentView(main);
        append("VeloWin Console 0.8 · ARM64/Bionic\n"
            +"Este é um ambiente privado do aplicativo, não um contêiner Linux completo.\n"
            +"Comandos disponíveis: help\n\n");
        append(session.status()+"\n\n");
        if(game==null)append("Laboratório sem jogo: instale Wine Bionic em Runtimes, "
            +"execute prefix prepare e depois win32 test.\n");
    }

    private void openExplorer() {
        Intent intent=new Intent(this,ContainerFilesActivity.class);
        if(game!=null)intent.putExtra("game_id",game.id);
        startActivity(intent);
    }

    private void configureX11() {
        if(running){append("[Finalize o teste antes de alterar X11]\n");return;}
        BridgeProfile old=session.bridges();
        String[] displays={"Desativado (mais seguro)",
            "Servidor X11 local :0 (TCP 6000)","Servidor X11 local :1 (TCP 6001)",
            "Servidor X11 local :2 (TCP 6002)"};
        int checked=old.display<0?0:Math.min(old.display+1,3);
        new AlertDialog.Builder(this).setTitle("X11 experimental — só no próprio telefone")
            .setMessage("Exige um servidor X11 TCP que você iniciou no Android. "
                +"O VeloWin não instala nem inicia X11. X11 TCP pode permitir que "
                +"outros processos locais leiam a interface; use só em um "
                +"ambiente de teste confiável. Nenhuma porta remota é aceita.")
            .setSingleChoiceItems(displays,checked,(dialog,choice)->{
                int display=choice-1;
                try {
                    LocalBridgeManager.put(this,session.sessionId(),
                        BridgeProfile.of(display,old.pulseAudio));
                    append("\n[Ponte X11] "+session.bridges().summary()+"\n");
                }catch(Exception e){append("\n[Erro na configuração X11] "+e.getMessage()+"\n");}
                dialog.dismiss();
            }).setNegativeButton("Cancelar",null).show();
    }

    private void configurePulse() {
        if(running){append("[Finalize o teste antes de alterar áudio]\n");return;}
        BridgeProfile old=session.bridges();
        new AlertDialog.Builder(this).setTitle("PulseAudio TCP local")
            .setMessage("Esta opção só configura PULSE_SERVER para o endereço "
                +"127.0.0.1:4713. Exige um servidor PulseAudio externo, "
                +"já iniciado por você. AudioTrack do Android sozinho não "
                +"recebe áudio do Wine.")
            .setNegativeButton("Cancelar",null)
            .setNeutralButton("Desativar",(dialog,which)->persistBridge(
                BridgeProfile.of(old.display,false)))
            .setPositiveButton("Ativar",(dialog,which)->persistBridge(
                BridgeProfile.of(old.display,true))).show();
    }

    private void persistBridge(BridgeProfile profile) {
        try {
            LocalBridgeManager.put(this,session.sessionId(),profile);
            append("\n[Ponte local atualizada]\n"+session.bridges().summary()+"\n");
        } catch(IOException error) {
            append("\n[Falha ao salvar ponte local] "+error.getMessage()+"\n");
        }
    }

    private void submitInput() {
        String command=input.getText().toString().trim();
        input.setText("");
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(input.getWindowToken(),0);
        ContainerCommand.Action action=ContainerCommand.parse(command);
        if(action==null) {
            append("\n> "+command+"\nComando não permitido. Digite help.\n");
            return;
        }
        dispatch(action);
    }

    private void dispatch(ContainerCommand.Action action) {
        if(running){
            append("\n[Operação em andamento. Use Cancelar teste.]\n");
            return;
        }
        if(action==ContainerCommand.Action.CLEAR){
            transcript.setLength(0);screen.setText("");return;
        }
        if(ContainerCommand.requiresConsent(action)) {
            String prompt=action==ContainerCommand.Action.DIAGNOSE
                ? "Executará uma sequência de testes reais Box64, Wine Bionic, "
                  +"preparação do template Wineprefix, Wine CMD e programa "
                  +"Win32 próprio do VeloWin. Não inicia wineboot, não executa "
                  +"jogos importados e pode modificar somente o prefixo privado "
                  +"do contêiner. A conexão X11 externa só será usada quando ativada."
                :action==ContainerCommand.Action.WIN32_SMOKE
                ? "Executará um pequeno programa Win32 do próprio VeloWin, compilado "
                  +"no GitHub Actions, via Wine Bionic e Box64. Ele escreve um marcador "
                  +"de sucesso no console. Não abre janela, testa GPU ou inicia jogos."
                :action==ContainerCommand.Action.RUN_EXE
                ? "O VeloWin tentará executar o arquivo Windows importado, "
                  +"em processo Wine/Box64 experimental. Programas desconhecidos podem "
                  +"acessar dados do aplicativo. A importação copia somente o EXE "
                  +"(até 384 MiB), sem DLLs ou arquivos auxiliares. Não existe "
                  +"servidor gráfico Wine embutido e o teste termina após 30 segundos."
                : "O Wine tentará inicializar o prefixo deste jogo. "
                  +"Isso inicia código Wine de terceiros, pode criar arquivos "
                  +"e processos filhos e falhar sem X11. A execução principal "
                  +"tem limite de 30 segundos, mas wineserver pode permanecer "
                  +"ativo; use Encerrar Wine depois.";
            new AlertDialog.Builder(this)
                .setTitle(action==ContainerCommand.Action.DIAGNOSE
                    ?"Executar diagnóstico guiado?"
                    :action==ContainerCommand.Action.RUN_EXE
                    ?"Testar executável Windows?":action==ContainerCommand.Action.WIN32_SMOKE
                    ?"Executar teste Win32 do VeloWin?":"Executar wineboot?")
                .setMessage(prompt)
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Eu entendo — testar",(d,w)->runAction(action)).show();
        }else runAction(action);
    }

    private void runAction(ContainerCommand.Action action) {
        if(running)return;
        running=true;cancelled.set(false);
        append("\n> "+action.name().toLowerCase().replace('_',' ')+"\n");
        worker.execute(()->{
            String output;
            try {
                output=session.execute(action,cancelled,
                    value->runOnUiThread(()->{
                        if(!isFinishing()&&!isDestroyed())append("[progresso] "+value+"\n");
                    }));
            }catch(Exception error){
                output="Falha: "+error.getClass().getSimpleName()+": "
                    +(error.getMessage()==null?"Sem detalhes":error.getMessage());
            }
            final String result=output;
            try{persistLog(action,result);}catch(IOException ignored){}
            runOnUiThread(()->{
                running=false;
                if(!isFinishing()&&!isDestroyed())
                    append(result+"\n$ ");
            });
        });
    }

    private void append(String value) {
        transcript.append(value);
        if(transcript.length()>26000)
            transcript.delete(0,transcript.length()-22000);
        if(screen!=null){
            screen.setText(transcript.toString());
            if(scroll!=null)scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
        }
    }

    private File logFile() throws IOException {
        File base=new File(BionicWorkspace.prepare(this),"logs");
        String name=game==null?"diagnostics.log":"container-"+game.id+".log";
        return new File(base,name);
    }

    private void persistLog(ContainerCommand.Action action,String result) throws IOException {
        File f=logFile();
        if(f.isFile()&&f.length()>128*1024) {
            byte[] old=Files.readAllBytes(f.toPath());
            int from=Math.max(0,old.length-48*1024);
            try(FileOutputStream trim=new FileOutputStream(f,false)){
                trim.write(old,from,old.length-from);
            }
        }
        String msg="\n--- "+System.currentTimeMillis()+" "+action+" ---\n"+result+"\n";
        try(FileOutputStream out=new FileOutputStream(f,true)){
            out.write(msg.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void shareLog() {
        String body=transcript.toString();
        try{
            Intent sharing=new Intent(Intent.ACTION_SEND);
            sharing.setType("text/plain");
            sharing.putExtra(Intent.EXTRA_SUBJECT,"VeloWin - log de contêiner");
            sharing.putExtra(Intent.EXTRA_TEXT,body);
            startActivity(Intent.createChooser(sharing,"Compartilhar diagnóstico"));
        }catch(Exception e){
            append("\n[Compartilhamento indisponível: "+e.getMessage()+"]\n");
        }
    }

    @Override public void onDestroy(){
        cancelled.set(true);
        worker.shutdownNow();
        super.onDestroy();
    }
}
