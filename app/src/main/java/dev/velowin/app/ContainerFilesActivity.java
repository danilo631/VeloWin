package dev.velowin.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import dev.velowin.app.engine.BionicWorkspace;
import dev.velowin.app.engine.ContainerFileBrowser;
import dev.velowin.app.engine.ContainerSession;
import dev.velowin.app.engine.WinePrefixManager;
import dev.velowin.app.library.Game;
import dev.velowin.app.library.GameStore;
import dev.velowin.app.runtime.BionicRelease;
import dev.velowin.app.runtime.BionicWineInstaller;

import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Original, shell-free Android file navigator for a VeloWin Bionic session.
 * Uses app-private roots derived from an existing GameStore ID; never a
 * filesystem path passed by an intent. It is NOT an X11 desktop or Wine GUI.
 */
public final class ContainerFilesActivity extends Activity {
    private static final int BG=0xff0b1018, PANEL=0xff182331;
    private static final int ACCENT=0xff88d7bd, MUTED=0xffa0b0bf;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final LinkedHashMap<String,Root> roots=new LinkedHashMap<>();
    private ContainerFileBrowser browser;
    private String selectedRoot="workspace", relative="";
    private int generation=0;
    private LinearLayout fileList,rootTabs;
    private TextView location,status;

    private static final class Root {
        final File file;
        final boolean writable;
        Root(File f,boolean write){file=f;writable=write;}
    }

    private int dp(int n){return Math.round(getResources().getDisplayMetrics().density*n);}
    private TextView text(String label,int size,int color,boolean bold) {
        TextView view=new TextView(this);
        view.setText(label);view.setTextSize(size);view.setTextColor(color);
        if(bold)view.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return view;
    }
    private GradientDrawable background(int color) {
        GradientDrawable shape=new GradientDrawable();
        shape.setColor(color);shape.setCornerRadius(dp(11));
        return shape;
    }
    private Button button(String caption,Runnable action,boolean highlight) {
        Button btn=new Button(this);
        btn.setAllCaps(false);btn.setTextSize(12);btn.setText(caption);
        btn.setTextColor(highlight?BG:Color.WHITE);
        btn.setBackground(background(highlight?ACCENT:PANEL));
        btn.setPadding(dp(10),dp(9),dp(10),dp(9));
        btn.setOnClickListener(v->action.run());
        return btn;
    }
    private void addRow(LinearLayout target,String label,Runnable action){
        Button btn=button(label,action,false);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);
        params.bottomMargin=dp(5);
        target.addView(btn,params);
    }

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        try{createRoots();}
        catch(Exception error){
            new AlertDialog.Builder(this).setTitle("Contêiner indisponível")
                .setMessage(error.getMessage()).setPositiveButton("Voltar",
                    (d,w)->finish()).setCancelable(false).show();
            return;
        }
        if(savedState!=null) {
            selectedRoot=savedState.getString("root","workspace");
            relative=savedState.getString("relative","");
        }
        if(!roots.containsKey(selectedRoot)){selectedRoot="workspace";relative="";}

        LinearLayout page=new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(14),dp(12),dp(14),dp(10));
        page.setBackgroundColor(BG);
        page.addView(text("V / ARQUIVOS DO CONTÊINER",18,Color.WHITE,true));
        TextView description=text("Navegador Android · armazenamento privado · sem shell",
            12,MUTED,false);
        LinearLayout.LayoutParams descParams=new LinearLayout.LayoutParams(-1,-2);
        descParams.topMargin=dp(5);descParams.bottomMargin=dp(10);
        page.addView(description,descParams);

        HorizontalScrollView horizontal=new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(false);
        rootTabs=new LinearLayout(this);
        rootTabs.setOrientation(LinearLayout.HORIZONTAL);
        horizontal.addView(rootTabs);
        page.addView(horizontal);

        location=text("",13,ACCENT,true);
        location.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin=dp(13);lp.bottomMargin=dp(9);
        page.addView(location,lp);
        LinearLayout nav=new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(nav);
        for(Object[] item:new Object[][]{
            {"← Voltar",(Runnable)this::goUp},
            {"Atualizar",(Runnable)this::refresh},
            {"+ Pasta",(Runnable)this::askNewFolder}}){
            Button b=button((String)item[0],(Runnable)item[1],false);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1f);
            p.rightMargin=dp(5);nav.addView(b,p);
        }

        ScrollView scroll=new ScrollView(this);
        fileList=new LinearLayout(this);
        fileList.setOrientation(LinearLayout.VERTICAL);
        fileList.setPadding(dp(8),dp(8),dp(8),dp(8));
        fileList.setBackground(background(PANEL));
        scroll.addView(fileList);
        LinearLayout.LayoutParams files=new LinearLayout.LayoutParams(-1,0,1f);
        files.topMargin=dp(12);files.bottomMargin=dp(8);
        page.addView(scroll,files);
        status=text("",11,MUTED,false);
        page.addView(status);
        Button console=button("Voltar ao console VeloWin",this::finish,true);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);
        cp.topMargin=dp(8);page.addView(console,cp);
        setContentView(page);
        renderRootTabs();refresh();
    }

    private void createRoots() throws Exception {
        String gameId=getIntent().getStringExtra("game_id");
        Game game=null;
        if(gameId!=null) {
            for(Game item:new GameStore(this).all())
                if(gameId.equals(item.id)){game=item;break;}
            if(game==null)throw new IOException("Jogo não encontrado na biblioteca.");
        }
        ContainerSession session=new ContainerSession(this,game);
        String id=session.sessionId();
        // GameStore is persistent user data, not a trusted filesystem path.
        try {
            UUID uuid=UUID.fromString(id);
            if(!uuid.toString().equals(id.toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("UUID não canônico.");
        }catch(IllegalArgumentException error){
            throw new IOException("Identificador do contêiner inválido.",error);
        }
        File workspace=BionicWorkspace.prepare(this);
        roots.put("workspace",new Root(workspace,true));

        File ownGame=new File(new File(workspace,"games"),id);
        if(!ownGame.isDirectory() && !ownGame.mkdirs())
            throw new IOException("Não foi possível criar a pasta privada do jogo.");
        roots.put("game",new Root(ownGame,true));

        File logs=new File(workspace,"logs");
        roots.put("logs",new Root(logs,false));
        File prefix=new File(new File(workspace,"prefixes"),id);
        if(prefix.isDirectory()) {
            roots.put("prefix",new Root(prefix,false));
            File cDrive=locateCDrive(prefix);
            if(cDrive!=null)roots.put("cdrive",new Root(cDrive,false));
        }
        BionicRelease wine=session.selectedWine();
        if(wine!=null) {
            File packageRoot=BionicWineInstaller.folder(this,wine);
            if(packageRoot.isDirectory())roots.put("wine",new Root(packageRoot,false));
        }
    }

    private File locateCDrive(File prefix) {
        try {
            File meta=new File(prefix,".velowin-prefix.json");
            if(!meta.isFile() || meta.length()>2048)return null;
            JSONObject json=new JSONObject(new String(Files.readAllBytes(meta.toPath()),
                StandardCharsets.UTF_8));
            String sub=json.optString("prefixRoot","");
            if(!sub.isEmpty() && (!sub.matches("[.A-Za-z0-9_-]{1,100}")
                || sub.equals(".") || sub.equals("..")))return null;
            File actual=sub.isEmpty()?prefix:new File(prefix,sub);
            File cdrive=new File(actual,"drive_c").getCanonicalFile();
            if(!cdrive.getPath().startsWith(prefix.getCanonicalPath()+File.separator)
                || !cdrive.isDirectory())return null;
            return cdrive;
        }catch(Exception ignored){return null;}
    }

    private String rootLabel(String id){
        switch(id){
            case "workspace":return "Workspace";
            case "game":return "Jogo";
            case "logs":return "Logs";
            case "prefix":return "Prefixo";
            case "cdrive":return "C: Wine";
            case "wine":return "Wine Bionic";
            default:return id;
        }
    }
    private void renderRootTabs() {
        rootTabs.removeAllViews();
        for(String id:roots.keySet()){
            Button tab=button(rootLabel(id),()->selectRoot(id),id.equals(selectedRoot));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);
            p.rightMargin=dp(6);rootTabs.addView(tab,p);
        }
    }
    private void selectRoot(String id){
        selectedRoot=id;relative="";
        renderRootTabs();refresh();
    }

    private void refresh(){
        Root target=roots.get(selectedRoot);
        if(target==null)return;
        final String path=relative,rootId=selectedRoot;
        final int request=++generation;
        location.setText(rootLabel(rootId)+" /"+path);
        status.setText("Carregando arquivos...");
        fileList.removeAllViews();
        io.execute(()->{
            List<ContainerFileBrowser.Entry> entries=null;
            String failure=null;
            ContainerFileBrowser current=null;
            try{
                current=new ContainerFileBrowser(target.file,target.writable);
                entries=current.list(path);
            }catch(Exception error){failure=error.getMessage();}
            final ContainerFileBrowser loaded=current;
            final List<ContainerFileBrowser.Entry> finalEntries=entries;
            final String errorMessage=failure;
            runOnUiThread(()->{
                if(isFinishing()||isDestroyed()||request!=generation)return;
                browser=loaded;
                if(errorMessage!=null){
                    status.setText("Erro: "+errorMessage);
                    return;
                }
                if(finalEntries==null)return;
                if(finalEntries.isEmpty()) {
                    fileList.addView(text("Pasta vazia. Os arquivos aparecerão aqui.",
                        13,MUTED,false));
                }else{
                    for(ContainerFileBrowser.Entry entry:finalEntries){
                        String label=(entry.directory?"📁 ":"📄 ")+entry.name;
                        addRow(fileList,label,()->openEntry(entry));
                    }
                }
                status.setText(finalEntries.size()+
                    (finalEntries.size()>=ContainerFileBrowser.MAX_ENTRIES?
                        "+ entradas (lista limitada a 300)":" entradas")+
                    " · somente pastas locais · links simbólicos ocultos");
            });
        });
    }

    private void openEntry(ContainerFileBrowser.Entry entry) {
        if(entry.directory) {
            relative=entry.relative;refresh();return;
        }
        final ContainerFileBrowser used=browser;
        io.execute(()->{
            String message;
            try{
                message=used.inspect(entry.relative);
                try{ message+="\n\n"+used.preview(entry.relative);}
                catch(IOException previewUnavailable) {
                    message+="\n\nPrévia: "+previewUnavailable.getMessage();
                }
            }catch(Exception error){message="Não foi possível inspecionar: "+error.getMessage();}
            final String result=message;
            runOnUiThread(()->{
                if(!isFinishing()&&!isDestroyed())new AlertDialog.Builder(this)
                    .setTitle(entry.name).setMessage(result)
                    .setPositiveButton("Fechar",null).show();
            });
        });
    }

    private void goUp(){
        if(relative.isEmpty()){finish();return;}
        try {relative=new ContainerFileBrowser(
                roots.get(selectedRoot).file,roots.get(selectedRoot).writable).parent(relative);
            refresh();
        }catch(IOException error){relative="";refresh();}
    }

    private void askNewFolder() {
        Root target=roots.get(selectedRoot);
        if(target==null || !target.writable){
            new AlertDialog.Builder(this).setMessage(
                "Esta área é somente leitura. Use Workspace ou Jogo para criar pastas.")
                .setPositiveButton("Entendi",null).show();
            return;
        }
        EditText field=new EditText(this);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        field.setHint("Nome da pasta");
        new AlertDialog.Builder(this).setTitle("Nova pasta privada")
            .setView(field)
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Criar",(d,w)->{
                String path=relative,name=field.getText().toString().trim();
                io.execute(()->{
                    String error=null;
                    try{new ContainerFileBrowser(target.file,true).makeFolder(path,name);}
                    catch(Exception e){error=e.getMessage();}
                    String result=error;
                    runOnUiThread(()->{
                        if(isFinishing()||isDestroyed())return;
                        if(result!=null)new AlertDialog.Builder(this)
                            .setMessage(result).setPositiveButton("OK",null).show();
                        refresh();
                    });
                });
            }).show();
    }

    @Override public void onBackPressed() {goUp();}
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("root",selectedRoot);
        out.putString("relative",relative);
    }
    @Override protected void onDestroy() {
        ++generation;
        io.shutdownNow();
        super.onDestroy();
    }
}
