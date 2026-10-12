package dev.velowin.app.engine;

import android.content.Context;
import android.net.Uri;
import android.system.Os;
import dev.velowin.app.library.Game;
import dev.velowin.app.library.PeInspector;
import dev.velowin.app.runtime.BionicArchiveStore;
import dev.velowin.app.runtime.BionicRelease;
import dev.velowin.app.runtime.BionicWineInstaller;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Rootless per-game Android/Bionic test container. Uses the verified Wine WCP
 * and the Box64 executable already bundled as an Android native library.
 *
 * This is not a kernel sandbox, a Linux rootfs, a display server or a supported
 * Wine gaming environment. All process entrypoints are explicit and opt-in.
 */
public final class ContainerSession {
    public interface Feedback { void update(String text); }

    private final Context ctx;
    private final Game game;

    public ContainerSession(Context context, Game game) {
        ctx=context.getApplicationContext();
        this.game=game==null?null:game.copy();
    }

    public BionicRelease selectedWine() {
        GameRuntimeChoice choice=GameRuntimeChoice.fromStored(
            game==null?"":game.runtimeTag);
        if(choice.kind==GameRuntimeChoice.Kind.GLIBC
                || choice.kind==GameRuntimeChoice.Kind.INVALID)return null;
        for(BionicRelease e:BionicArchiveStore.saved(ctx)) {
            if(!"x86_64".equals(e.arch) || !BionicWineInstaller.installed(ctx,e))
                continue; // FEX/ARM64EC is NOT a Box64-compatible Wine.
            if(choice.kind==GameRuntimeChoice.Kind.BIONIC
                && (!e.tag.equals(choice.tag) || !e.arch.equals(choice.arch)))
                continue;
            return e;
        }
        return null;
    }

    public String status() {
        StringBuilder out=new StringBuilder("Contêiner Android/Bionic · VeloWin\n");
        out.append("Tradutor Box64: ").append(Box64Smoke.bundled(ctx)?"presente":"ausente")
            .append("\nWine Bionic x86_64: ");
        BionicRelease e=selectedWine();
        out.append(e==null?"não selecionado/instalado":e.tag);
        out.append("\nWine ARM64EC/FEX: pipeline distinto, indisponível neste console");
        if(game==null) {
            out.append("\nModo: diagnóstico sem jogo.");
        }else{
            out.append("\nJogo: ").append(game.title)
                .append("\nArquitetura PE: ").append(game.architecture)
                .append("\nBackend do perfil: ").append(game.backend)
                .append("\nWineprefix: ");
            if(e!=null)out.append(WinePrefixManager.status(ctx,game,e));
            else out.append("indisponível");
        }
        out.append("\nPonte Wine/X11: não integrada")
            .append("\nÁudio Win32: não integrado")
            .append("\nControles Win32: não integrados")
            .append("\nDirect3D/DXVK: não integrado")
            .append("\n\nAqui você pode testar processos reais de console; "
                +"um código 0 só valida o comando executado.");
        return out.toString();
    }

    public String files() throws Exception {
        File base=BionicWorkspace.prepare(ctx);
        StringBuilder b=new StringBuilder("Workspace: ").append(base.getAbsolutePath())
            .append("\nHOME: ").append(new File(base,"home").getAbsolutePath())
            .append("\nTMPDIR: ").append(new File(base,"tmp").getAbsolutePath())
            .append("\nLogs: ").append(new File(base,"logs").getAbsolutePath());
        if(game!=null) {
            b.append("\nPrefixo: ").append(WinePrefixManager.location(ctx,game).getAbsolutePath())
                .append("\nExecutável: content:// (SAF), ainda não copiado");
        }
        b.append("\nEssas pastas não oferecem isolamento de permissões de kernel.");
        return b.toString();
    }

    public String execute(ContainerCommand.Action action, AtomicBoolean cancelled,
                          Feedback feedback) throws Exception {
        if(action==null)throw new IOException("Comando desconhecido. Digite help.");
        if(cancelled.get())throw new IOException("Teste cancelado.");
        switch(action) {
            case HELP: return ContainerCommand.manual();
            case STATUS: return status();
            case FILES: return files();
            case PREFIX_STATUS:
                if(game==null)return "Escolha um jogo para criar um prefixo individual.";
                BionicRelease checked=selectedWine();
                return checked==null?"Wine Bionic não instalado. Abra Runtimes."
                    :WinePrefixManager.status(ctx,game,checked);
            case BOX64_VERSION:
                if(!Box64Smoke.bundled(ctx))
                    return "Box64 ausente. Instale o APK VeloWin que contém Box64 Bionic.";
                return probe(Arrays.asList(box64().getAbsolutePath(),"-v"),null,
                    8000,cancelled,"Box64 -v");
            case PREFIX_PREPARE:
                if(game==null)return "Abra o contêiner de um jogo da Biblioteca.";
                BionicRelease packageWine=needWine();
                if(WinePrefixManager.templateReady(ctx,game,packageWine))
                    return "Template Wineprefix já preparado. Wineboot ainda não validado.";
                feedback.update("Extraindo prefixPack.txz no diretório privado do jogo...");
                WinePrefixManager.prepare(ctx,game,packageWine,cancelled,
                    n->feedback.update("Extraindo template: "+n+" entradas TAR"));
                return "Template Wineprefix preparado. Agora teste wine cmd /c ver "
                    +"ou wineboot -u. Isso não confirma um servidor gráfico.";
            case WINE_VERSION:
                BionicRelease v=needWine();
                return probe(wineCommand(v,"--version"),environment(v,false),
                    12000,cancelled,"Wine --version");
            case CMD_VERSION:
                BionicRelease c=needWine();
                needPrefix(c);
                return probe(wineCommand(c,"cmd","/c","ver"),environment(c,true),
                    22000,cancelled,"Wine cmd /c ver");
            case WINEBOOT:
                BionicRelease w=needWine();
                needPrefix(w);
                // May spawn wineserver descendants; the UI warns about that risk.
                return probe(wineCommand(w,"wineboot","-u"),environment(w,true),
                    30000,cancelled,"Wineboot -u (experimental)")
                    +"\nPode ser necessário encerrar wineserver -k. "
                    +"Não há servidor X11 embutido.";
            case RUN_EXE:
                BionicRelease run=needWine();
                needPrefix(run);
                File exe=stageGame(cancelled,feedback);
                return probe(wineCommand(run,exe.getAbsolutePath()),environment(run,true),
                    30000,cancelled,"EXE Windows importado (experimental)")
                    +"\nArquivos auxiliares do jogo não foram importados. "
                    +"Sem X11, programas com janela podem falhar.";
            case STOP_WIN_SERVER:
                BionicRelease server=needWine();
                needPrefix(server);
                File wineserver=confinedWineBinary(server,"bin/wineserver");
                if(!wineserver.isFile())return "Pacote Wine não contém bin/wineserver.";
                return probe(Arrays.asList(box64().getAbsolutePath(),
                    wineserver.getAbsolutePath(),"-k"),environment(server,true),
                    10000,cancelled,"Wineserver -k");
            case CLEAR: return "";
            default: throw new IOException("Ação não reconhecida.");
        }
    }

    private BionicRelease needWine() throws IOException {
        if(!Box64Smoke.bundled(ctx))
            throw new IOException("Box64 não encontrado no APK. Não é possível rodar Wine.");
        if(game!=null && "FEX".equals(game.backend))
            throw new IOException("Este contêiner usa Box64. Selecione BOX64 no perfil do jogo.");
        BionicRelease release=selectedWine();
        if(release==null)
            throw new IOException("Prepare e selecione um Wine Bionic x86_64 em Runtimes. "
                +"Wine glibc ou ARM64EC não é compatível com este console Box64.");
        return release;
    }

    private File box64() throws IOException {
        File binary=new File(ctx.getApplicationInfo().nativeLibraryDir,"libvelobox64.so");
        if(!Box64Smoke.bundled(ctx) || !binary.isFile())
            throw new IOException("Box64 não incluído.");
        return binary.getCanonicalFile();
    }

    private File confinedWineBinary(BionicRelease release,String path) throws IOException {
        File root=BionicWineInstaller.folder(ctx,release).getCanonicalFile();
        File binary=new File(root,path).getCanonicalFile();
        if(!binary.getPath().startsWith(root.getPath()+File.separator))
            throw new IOException("Binário Wine aponta para fora do WCP.");
        return binary;
    }

    private List<String> wineCommand(BionicRelease release,String... args) throws Exception {
        File wine=confinedWineBinary(release,"bin/wine");
        if(!wine.isFile())throw new IOException("Wine Bionic sem bin/wine.");
        ArrayList<String> result=new ArrayList<>();
        result.add(box64().getAbsolutePath());
        result.add(wine.getAbsolutePath());
        result.addAll(Arrays.asList(args));
        return result;
    }

    private File needPrefix(BionicRelease release) throws Exception {
        if(game==null)throw new IOException("Abra o contêiner pelo menu de um jogo.");
        if(!WinePrefixManager.templateReady(ctx,game,release))
            throw new IOException("Prepare o template do prefixo primeiro: prefix prepare.");
        File folder=WinePrefixManager.location(ctx,game).getCanonicalFile();
        File receipt=new File(folder,".velowin-prefix.json");
        if(!receipt.isFile() || receipt.length()>2048)
            throw new IOException("Manifesto de Wineprefix ausente ou inválido.");
        JSONObject json=new JSONObject(new String(Files.readAllBytes(receipt.toPath()),
            StandardCharsets.UTF_8));
        String root=json.optString("prefixRoot","");
        if(!root.isEmpty() && (!root.matches("[.A-Za-z0-9_-]{1,100}")
            || "..".equals(root) || ".".equals(root)))
            throw new IOException("Nome de prefixo inválido.");
        File prefix=(root.isEmpty()?folder:new File(folder,root)).getCanonicalFile();
        if(!prefix.equals(folder) && !prefix.getPath().startsWith(folder.getPath()+File.separator))
            throw new IOException("Prefixo fora do contêiner.");
        if(!new File(prefix,"drive_c").isDirectory())
            throw new IOException("Prefixo sem unidade C.");
        return prefix;
    }

    private Map<String,String> environment(BionicRelease release,boolean withPrefix)
        throws Exception {
        File work=BionicWorkspace.prepare(ctx);
        File wineRoot=BionicWineInstaller.folder(ctx,release).getCanonicalFile();
        Box64Tuning.Preset preset=game==null?Box64Tuning.Preset.BALANCED
            :Box64Tuning.fromGame(game.boxPreset);
        Map<String,String> env=new LinkedHashMap<>(Box64Tuning.environment(preset));
        env.put("HOME",new File(work,"home").getAbsolutePath());
        env.put("TMPDIR",new File(work,"tmp").getAbsolutePath());
        env.put("XDG_CACHE_HOME",new File(work,"cache").getAbsolutePath());
        env.put("WINEDEBUG","warn+all");
        env.put("WINEESYNC","0");
        env.put("WINEFSYNC","0");
        env.put("BOX64_LD_LIBRARY_PATH",
            new File(wineRoot,"lib/wine/x86_64-unix").getAbsolutePath()+":"
            +new File(wineRoot,"lib").getAbsolutePath());
        if(withPrefix)env.put("WINEPREFIX",needPrefix(release).getAbsolutePath());
        return env;
    }

    private String probe(List<String> argv,Map<String,String> env,long timeout,
        AtomicBoolean cancelled,String description) throws Exception {
        File dir=BionicWorkspace.prepare(ctx);
        BoundedProcess.Result result=BoundedProcess.run(argv,dir,
            env==null?Box64Tuning.environment(Box64Tuning.Preset.BALANCED):env,
            timeout,cancelled);
        String report=description+"\n"+result.summary()
            +"\nDiagnóstico: "+FailureClassifier.explain(result.output,result.code,result.timeout)
            +"\nEste resultado não comprova execução de jogos Windows.";
        if(result.timeout || result.cancelled)
            report+="\nAviso: processos Wine filhos podem permanecer; use wineserver -k.";
        return report;
    }

    /** Copy one SAF-selected EXE only; no neighboring DLLs/assets are imported. */
    private File stageGame(AtomicBoolean cancelled,Feedback feedback) throws Exception {
        if(game==null || game.uri==null || !game.uri.startsWith("content://"))
            throw new IOException("Nenhum .exe importado pelo seletor Android.");
        if(!"x86".equals(game.architecture) && !"x86_64".equals(game.architecture))
            throw new IOException("Somente programas Windows PE x86/x86_64 são testáveis.");
        Uri uri=Uri.parse(game.uri);
        if(!"content".equals(uri.getScheme()))throw new IOException("URI não autorizada.");
        try(InputStream source=ctx.getContentResolver().openInputStream(uri)) {
            if(source==null)throw new IOException("O Android não abriu o .exe.");
            String arch=PeInspector.inspect(source);
            if(!arch.equals(game.architecture))
                throw new IOException("A arquitetura do .exe mudou desde a importação.");
        }
        File workspace=BionicWorkspace.prepare(ctx);
        File games=new File(workspace,"games");
        if(!games.isDirectory()&&!games.mkdirs())throw new IOException("Sem diretório de jogos.");
        UUID id=UUID.fromString(game.id);
        File folder=new File(games,id.toString());
        if(!folder.isDirectory() && !folder.mkdirs())throw new IOException("Falha na pasta do jogo.");
        File target=new File(folder,"velowin-test.exe");
        File partial=new File(folder,".velowin-test.part");
        if(partial.exists()&&!partial.delete())throw new IOException("Cópia anterior bloqueada.");
        final long max=384L*1024*1024;
        long count=0;
        try(InputStream in=ctx.getContentResolver().openInputStream(uri);
            FileOutputStream out=new FileOutputStream(partial)) {
            if(in==null)throw new IOException("EXE inacessível.");
            byte[] buf=new byte[65536];
            int n;
            while((n=in.read(buf))!=-1){
                if(cancelled.get() || Thread.currentThread().isInterrupted())
                    throw new IOException("Cópia cancelada.");
                count+=n;
                if(count>max)throw new IOException("EXE acima de 384 MiB. "
                    +"Importe programas de teste menores.");
                if(folder.getUsableSpace()<64L*1024*1024)
                    throw new IOException("Armazenamento insuficiente.");
                out.write(buf,0,n);
                if(count%(8L*1024*1024)<n)
                    feedback.update("Copiando EXE: "+(count/1048576)+" MiB");
            }
            out.getFD().sync();
        }catch(Exception error) {
            partial.delete();
            throw error;
        }
        if(count<64){partial.delete();throw new IOException("Arquivo EXE vazio ou truncado.");}
        if(target.exists()&&!target.delete()){partial.delete();throw new IOException("Arquivo anterior protegido.");}
        if(!partial.renameTo(target)){partial.delete();throw new IOException("Falha ao finalizar cópia.");}
        return target;
    }
}
