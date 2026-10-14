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
    private final boolean diagnosticMode;

    public ContainerSession(Context context, Game selectedGame) {
        ctx=context.getApplicationContext();
        diagnosticMode=selectedGame==null;
        if(diagnosticMode) {
            // Persistent private test container. Not added to GameStore.
            Game demo=new Game("Contêiner de diagnóstico", "");
            demo.id=UUID.nameUUIDFromBytes(
                "VeloWin/Bionic/demo-container/v1".getBytes(StandardCharsets.UTF_8))
                .toString();
            demo.architecture="x86_64";
            demo.backend="BOX64";
            this.game=demo;
        }else{
            this.game=selectedGame.copy();
        }
    }

    public String sessionId() { return game.id; }

    public BridgeProfile bridges() { return LocalBridgeManager.get(ctx,game.id); }

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
        if(diagnosticMode) {
            out.append("\nModo: contêiner de diagnóstico independente da Biblioteca")
                .append("\nWineprefix: ");
            if(e!=null)out.append(WinePrefixManager.status(ctx,game,e));
            else out.append("indisponível");
        }else{
            out.append("\nJogo: ").append(game.title)
                .append("\nArquitetura PE: ").append(game.architecture)
                .append("\nBackend do perfil: ").append(game.backend)
                .append("\nWineprefix: ");
            if(e!=null)out.append(WinePrefixManager.status(ctx,game,e));
            else out.append("indisponível");
        }
        out.append("\nPonte local opt-in:\n").append(bridges().summary())
            .append("\nPonte Wine/X11 nativa: não integrada")
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
        b.append("\nPrefixo: ").append(WinePrefixManager.location(ctx,game).getAbsolutePath());
        if(!diagnosticMode)
            b.append("\nExecutável: content:// (SAF), ainda não copiado");
        else b.append("\nExecutável importado: nenhum (modo diagnóstico)");
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
            case BRIDGE_PROBE: return LocalBridgeManager.probe(bridges());
            case DIAGNOSE: return runGuided(cancelled,feedback);
            case FILES: return files();
            case PREFIX_STATUS:
                // O contêiner de diagnóstico também tem seu próprio Wineprefix.
                BionicRelease checked=selectedWine();
                return checked==null?"Wine Bionic não instalado. Abra Runtimes."
                    :WinePrefixManager.status(ctx,game,checked);
            case BOX64_VERSION:
                if(!Box64Smoke.bundled(ctx))
                    return "Box64 ausente. Instale o APK VeloWin que contém Box64 Bionic.";
                return probe(Arrays.asList(box64().getAbsolutePath(),"-v"),null,
                    8000,cancelled,"Box64 -v");
            case PREFIX_PREPARE:
                // O modo de diagnóstico usa seu prefixo privado estável.
                BionicRelease packageWine=needWine();
                if(WinePrefixManager.templateReady(ctx,game,packageWine))
                    return "Template Wineprefix já preparado. Wineboot ainda não validado.";
                feedback.update("Extraindo prefixPack.txz no diretório privado do jogo...");
                WinePrefixManager.prepare(ctx,game,packageWine,cancelled,
                    n->feedback.update("Extraindo template: "+n+" entradas TAR"));
                return "Template Wineprefix preparado. Agora teste wine cmd /c ver "
                    +"ou wineboot -u. Isso não confirma um servidor gráfico.";
            case WINE_PREFLIGHT:
                BionicRelease auditWine=needWine();
                return WineRuntimeAudit.report(BionicWineInstaller.folder(ctx,auditWine));
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
            case X11_WINDOW_PROBE:
                if(!bridges().x11Enabled())
                    throw new IOException("Selecione uma ponte X11 local antes de abrir janelas.");
                if(LocalBridgeManager.inspectX11(bridges().x11Port())
                    .indexOf("aceitou handshake")<0)
                    throw new IOException("Servidor X11 não confirmou o protocolo. "
                        +"Verifique XServer local, porta e autenticação.");
                BionicRelease guiWine=needWine();
                needPrefix(guiWine);
                return probe(wineCommand(guiWine,"notepad"),
                    environment(guiWine,true),30000,cancelled,
                    "Wine notepad / X11 externo (janela não verificada por Surface)")
                    +"\nEste teste exige servidor X11 real já iniciado. "
                    +"Uma resposta TCP não confirma que a janela apareceu.";
            case WIN32_SMOKE:
                BionicRelease smokeWine=needWine();
                needPrefix(smokeWine);
                File executable=ownSmokeBinary(cancelled);
                BoundedProcess.Result smokeResult=runProbe(
                    wineCommand(smokeWine,executable.getAbsolutePath()),
                    environment(smokeWine,true),25000,cancelled);
                boolean passed=Win32ProbeVerdict.passed(smokeResult.code,
                    smokeResult.timeout,smokeResult.cancelled,smokeResult.output);
                return Win32ProbeVerdict.summary(smokeResult.code,smokeResult.timeout,
                    smokeResult.cancelled,smokeResult.output)+"\n"
                    +probeReport(smokeResult,"VeloWin Win32 Smoke (x86_64)")
                    +"\nIsto não comprova renderização de jogos.";
            case RUN_EXE:
                if(diagnosticMode)
                    throw new IOException("Para executar seu EXE, entre pelo jogo na Biblioteca.");
                BionicRelease run=needWine();
                needPrefix(run);
                File exe=stageGame(cancelled,feedback);
                return probe(wineCommand(run,exe.getAbsolutePath()),gameEnvironment(run),
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
        // Keep the Wine child-process PATH and loader settings consistent with
        // standalone Wine --version. No Linux/glibc PATH is inherited.
        return WineLaunchEnvironment.build(wineRoot,work,
            withPrefix?needPrefix(release):null,preset,bridges().environment(),true);
    }

    private Map<String,String> gameEnvironment(BionicRelease release)throws Exception {
        Map<String,String> env=environment(release,true);
        String graphics=game.renderApi==null?"WINED3D":game.renderApi;
        if("DXVK".equals(graphics)||"VKD3D".equals(graphics)) {
            // Fail closed: a menu preference alone does not enable Direct3D.
            env.put("WINEDLLOVERRIDES",GameGraphicsOverlay.overrides(ctx,game,graphics));
        } else if("WINED3D".equals(graphics)) {
            String overlayStatus=GameGraphicsOverlay.status(ctx,game);
            if(overlayStatus.startsWith("Injeção local verificada"))
                throw new IOException("Há DLLs DXVK/VKD3D no jogo. Remova a camada "
                    +"em Configurações do jogo antes de escolher WineD3D.");
        }
        return env;
    }

    private BoundedProcess.Result runProbe(List<String> argv,Map<String,String> env,
        long timeout,AtomicBoolean cancelled) throws Exception {
        File work=BionicWorkspace.prepare(ctx);
        return BoundedProcess.run(argv,work,
            env==null?Box64Tuning.environment(Box64Tuning.Preset.BALANCED):env,
            timeout,cancelled);
    }

    private String probeReport(BoundedProcess.Result result,String description) {
        String report=description+"\n"+result.summary()
            +"\nDiagnóstico: "+FailureClassifier.explain(result.output,result.code,result.timeout)
            +"\nEste resultado não comprova execução de jogos Windows.";
        if(result.timeout || result.cancelled)
            report+="\nAviso: processos Wine filhos podem permanecer; use wineserver -k.";
        return report;
    }

    private String probe(List<String> argv,Map<String,String> env,long timeout,
        AtomicBoolean cancelled,String description) throws Exception {
        return probeReport(runProbe(argv,env,timeout,cancelled),description);
    }

    /** A transparent manual wizard: never starts wineboot or imports an arbitrary EXE. */
    private String runGuided(AtomicBoolean cancelled,Feedback feedback) throws Exception {
        StringBuilder out=new StringBuilder("DIAGNÓSTICO GUIADO VELOWIN\n");
        out.append("Pontes locais:\n").append(LocalBridgeManager.probe(bridges()))
            .append("\n\n");
        feedback.update("Etapa 1/5: inicializar Box64");
        if(!Box64Smoke.bundled(ctx))
            return out.append("BLOQUEIO: Box64 Bionic ausente no APK.").toString();
        BoundedProcess.Result cpu=runProbe(Arrays.asList(box64().getAbsolutePath(),"-v"),
            null,8000,cancelled);
        out.append("1. Box64: ").append(cpu.summary()).append("\n");
        if(!cpuGood(cpu))return out.append("PARADO: tradutor Box64 não iniciou.\n")
            .append(FailureClassifier.explain(cpu.output,cpu.code,cpu.timeout)).toString();
        if(cancelled.get())return out.append("Cancelado.").toString();
        feedback.update("Etapa 2/5: verificar Wine Bionic");
        BionicRelease wine;
        try{ wine=needWine(); }
        catch(Exception error){return out.append("2. Wine: NÃO DISPONÍVEL — ")
            .append(error.getMessage()).toString();}
        out.append("2a. Pré-diagnóstico do pacote Wine:\n")
            .append(WineRuntimeAudit.report(BionicWineInstaller.folder(ctx,wine)));
        if(!WineRuntimeAudit.loaderReady(BionicWineInstaller.folder(ctx,wine)))
            return out.append("PARADO: ELF Wine x86_64/Bionic inválido.").toString();
        BoundedProcess.Result version=runProbe(wineCommand(wine,"--version"),
            environment(wine,false),12000,cancelled);
        out.append("2. Wine --version: ").append(version.summary()).append("\n");
        if(!cpuGood(version))return out.append("PARADO: Wine Bionic falhou.\n")
            .append(FailureClassifier.explain(version.output,version.code,version.timeout)).toString();
        feedback.update("Etapa 3/5: preparar prefixo Windows isolado");
        if(!WinePrefixManager.templateReady(ctx,game,wine))
            WinePrefixManager.prepare(ctx,game,wine,cancelled,
                count->feedback.update("Extraindo prefixo: "+count+" entradas"));
        out.append("3. Template Wineprefix: PREPARADO; wineboot não foi executado.\n");
        if(cancelled.get())return out.append("Cancelado.").toString();
        feedback.update("Etapa 4/5: executar Wine cmd /c ver");
        BoundedProcess.Result cmd=runProbe(wineCommand(wine,"cmd","/c","ver"),
            environment(wine,true),22000,cancelled);
        out.append("4. Wine CMD: ").append(cmd.summary()).append("\n");
        if(!cpuGood(cmd))return out.append("PARADO: programa Wine CMD falhou.\n")
            .append(FailureClassifier.explain(cmd.output,cmd.code,cmd.timeout)).toString();
        feedback.update("Etapa 5/5: testar executável Win32 próprio");
        File own=ownSmokeBinary(cancelled);
        BoundedProcess.Result windows=runProbe(wineCommand(wine,own.getAbsolutePath()),
            environment(wine,true),25000,cancelled);
        out.append("5. Win32 Smoke: ").append(windows.summary()).append("\n")
            .append(Win32ProbeVerdict.summary(windows.code,windows.timeout,
                windows.cancelled,windows.output)).append("\n");
        if(Win32ProbeVerdict.passed(windows.code,windows.timeout,
            windows.cancelled,windows.output))
            out.append("A pilha executou o PE de console VeloWin, mas não "
                +"há prova de janelas, áudio, DXVK ou jogos.");
        else out.append("Programa de teste Windows não foi validado. ")
            .append(FailureClassifier.explain(windows.output,windows.code,windows.timeout));
        return out.toString();
    }

    private static boolean cpuGood(BoundedProcess.Result result) {
        return result.code==0 && !result.timeout && !result.cancelled;
    }

    /**
     * A minimal Windows PE built from our own checked-in source code during CI.
     * Not a Windows game. Has a unique stdout success marker.
     */
    private File ownSmokeBinary(AtomicBoolean cancelled) throws Exception {
        File work=BionicWorkspace.prepare(ctx);
        File dir=new File(work,"smoke");
        if(!dir.isDirectory() && !dir.mkdirs())
            throw new IOException("Falha ao preparar área de teste Win32.");
        File destination=new File(dir,"velowin-smoke.exe");
        File temp=new File(dir,".velowin-smoke.part");
        if(temp.exists() && !temp.delete())
            throw new IOException("Teste anterior ainda em preparação.");
        int total=0;
        try(InputStream in=ctx.getAssets().open("velowin-smoke.exe");
            FileOutputStream out=new FileOutputStream(temp)) {
            byte[] block=new byte[16384];
            int read;
            while((read=in.read(block))!=-1) {
                if(cancelled.get()||Thread.currentThread().isInterrupted())
                    throw new IOException("Cópia do teste cancelada.");
                total+=read;
                if(total>4*1024*1024)
                    throw new IOException("Executável de teste inesperadamente grande.");
                out.write(block,0,read);
            }
            out.getFD().sync();
        }catch(Exception failure){temp.delete();throw failure;}
        try(InputStream validate=new FileInputStream(temp)) {
            if(!"x86_64".equals(PeInspector.inspect(validate)))
                throw new IOException("O executável do teste não é PE x86_64.");
        }catch(Exception failure){temp.delete();throw failure;}
        if(destination.exists() && !destination.delete())
            throw new IOException("Não foi possível substituir o teste Win32 antigo.");
        if(!temp.renameTo(destination)) {
            temp.delete();
            throw new IOException("Não foi possível disponibilizar teste Win32.");
        }
        return destination;
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
