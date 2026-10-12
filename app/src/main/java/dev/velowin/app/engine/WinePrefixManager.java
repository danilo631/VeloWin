package dev.velowin.app.engine;

import android.content.Context;
import android.system.Os;
import dev.velowin.app.library.Game;
import dev.velowin.app.runtime.ArchiveSafety;
import dev.velowin.app.runtime.BionicRelease;
import dev.velowin.app.runtime.BionicWineInstaller;
import dev.velowin.app.runtime.WinePackageInstaller;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Prepares a private Wineprefix TEMPLATE per game from a locally installed,
 * validated Bionic Wine WCP. No wineboot or game process is started.
 * All files and symlinks are treated as untrusted archive input.
 */
public final class WinePrefixManager {
    private WinePrefixManager() {}
    private static final long MAX_TOTAL=3L*1024*1024*1024;
    private static final long MAX_MEMBER=512L*1024*1024;
    private static final int MAX_ENTRIES=150000;
    private static final String MARKER=".velowin-prefix.json";

    public interface Progress { void entries(int count); }

    private static String validId(Game game) throws IOException {
        if(game==null || game.id==null)throw new IOException("Jogo não selecionado.");
        try {
            UUID id=UUID.fromString(game.id);
            if(!id.toString().equals(game.id.toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("UUID não canônico.");
        } catch(IllegalArgumentException failure) {
            throw new IOException("Identificador do jogo inválido.",failure);
        }
        return game.id.toLowerCase(Locale.ROOT);
    }

    public static File location(Context context,Game game) throws IOException {
        return new File(new File(BionicWorkspace.prepare(context),"prefixes"),validId(game));
    }

    private static File confined(File base,String member) throws IOException {
        File dest=new File(base,member);
        if(!dest.getCanonicalPath().startsWith(base.getCanonicalPath()+File.separator))
            throw new IOException("Caminho prefixPack fora da pasta isolada.");
        return dest;
    }

    public static boolean templateReady(Context context,Game game,BionicRelease release) {
        if(release==null)return false;
        try {
            File root=location(context,game);
            File m=new File(root,MARKER);
            if(!m.isFile() || m.length()>2048 || m.length()<4)return false;
            JSONObject meta=new JSONObject(new String(Files.readAllBytes(m.toPath()),
                StandardCharsets.UTF_8));
            if(!validId(game).equals(meta.optString("game"))
                || !release.digest.equals(meta.optString("wineSha256"))
                || !release.tag.equals(meta.optString("wineTag"))
                || !"TEMPLATE_ONLY".equals(meta.optString("state")))return false;
            String prefix=meta.optString("prefixRoot","");
            if(prefix.contains("/") || prefix.contains("\\") || prefix.equals("..")
                || prefix.equals("."))return false;
            File actual=prefix.isEmpty()?root:confined(root,prefix);
            return new File(actual,"drive_c").isDirectory()
                && new File(actual,"system.reg").isFile();
        } catch(Exception failure) {return false;}
    }

    public static String status(Context context,Game game,BionicRelease release) {
        if(templateReady(context,game,release))
            return "Template Wineprefix preparado. Wineboot ainda NÃO validado.";
        return "Template Wineprefix ausente para este jogo/Wine.";
    }

    public static File prepare(Context context,Game game,BionicRelease wine,
        AtomicBoolean cancelled,Progress progress) throws Exception {
        if(cancelled==null || progress==null)throw new IOException("Operação inválida.");
        if(wine==null || !BionicWineInstaller.installed(context,wine))
            throw new IOException("Instale um Wine Bionic verificado antes do prefixo.");
        if(!"x86_64".equals(wine.arch) && !"ARM64EC".equals(wine.arch))
            throw new IOException("Arquitetura Wine Bionic desconhecida.");
        File destination=location(context,game);
        if(templateReady(context,game,wine))return destination;
        if(destination.exists())
            throw new IOException("Prefixo anterior ou incompleto preservado. Remova-o explicitamente.");
        File wineRoot=BionicWineInstaller.folder(context,wine);
        File archive=confined(wineRoot,"prefixPack.txz");
        if(!archive.isFile() || archive.length()>512L*1024*1024 || archive.length()<1024)
            throw new IOException("Template prefixPack.txz ausente ou muito grande.");
        File parent=destination.getParentFile();
        if(!parent.isDirectory() && !parent.mkdirs())
            throw new IOException("Não foi possível criar diretório de prefixos.");
        if(parent.getUsableSpace()<256L*1024*1024)
            throw new IOException("Espaço insuficiente para preparar Wineprefix.");

        File stage=new File(parent,".prefix-stage-"+UUID.randomUUID());
        if(!stage.mkdir())throw new IOException("Falha ao criar diretório temporário.");
        long expanded=0;
        int count=0;
        Set<String> seen=new HashSet<>();
        Set<String> links=new HashSet<>();
        List<String[]> pending=new ArrayList<>();
        try {
            try(InputStream source=new BufferedInputStream(new FileInputStream(archive),65536);
                XZCompressorInputStream decoded=new XZCompressorInputStream(source);
                TarArchiveInputStream tar=new TarArchiveInputStream(decoded)) {
                byte[] buf=new byte[65536];
                TarArchiveEntry member;
                while((member=tar.getNextEntry())!=null) {
                    if(cancelled.get() || Thread.currentThread().isInterrupted())
                        throw new IOException("Preparação cancelada.");
                    if(++count>MAX_ENTRIES || !tar.canReadEntryData(member))
                        throw new IOException("Template excede limite de entradas.");
                    String name=ArchiveSafety.member(member.getName());
                    if(name==null)throw new IOException("Caminho inválido no template Wine.");
                    if(name.isEmpty() && member.isDirectory())continue;
                    if(name.isEmpty() || !seen.add(name) ||
                        ArchiveSafety.conflicts(name,links))
                        throw new IOException("Entrada duplicada ou incompatível em prefixPack.");
                    long len=member.getSize();
                    if(len<0 || len>MAX_MEMBER || expanded>MAX_TOTAL-len)
                        throw new IOException("Template Wine grande demais.");
                    expanded+=len;
                    File target=confined(stage,name);
                    if(member.isSymbolicLink()) {
                        PrefixLinkPolicy.Decision rule=
                            PrefixLinkPolicy.decide(name,member.getLinkName());
                        if(rule==PrefixLinkPolicy.Decision.REJECT)
                            throw new IOException("Link absoluto ou fora da pasta: "+name);
                        // Wine Z: -> / would reveal host Android. Omit this drive.
                        if(rule==PrefixLinkPolicy.Decision.SKIP_HOST_ROOT_Z)continue;
                        pending.add(new String[]{name,member.getLinkName()});
                        links.add(name);
                        continue;
                    }
                    if(!member.isDirectory() && !member.isFile())
                        throw new IOException("Dispositivo/hardlink/FIFO não permitido no prefixo.");
                    if(member.isDirectory()) {
                        if(target.isFile() || (target.exists()&&!target.isDirectory()))
                            throw new IOException("Diretório conflita com arquivo.");
                        if(!target.isDirectory() && !target.mkdirs())
                            throw new IOException("Falha ao criar pasta do prefixo.");
                    } else {
                        File up=target.getParentFile();
                        if(!up.isDirectory() && !up.mkdirs())
                            throw new IOException("Falha ao criar pais do prefixo.");
                        if(Files.exists(target.toPath(),LinkOption.NOFOLLOW_LINKS))
                            throw new IOException("Arquivo do prefixo duplicado.");
                        if(stage.getUsableSpace()<len+64L*1024*1024)
                            throw new IOException("Sem espaço para template Wine.");
                        try(FileOutputStream out=new FileOutputStream(target)) {
                            long written=0;
                            while(written<len) {
                                if(cancelled.get() || Thread.currentThread().isInterrupted())
                                    throw new IOException("Preparação interrompida.");
                                int n=tar.read(buf,0,(int)Math.min(buf.length,len-written));
                                if(n<=0)throw new IOException("Template TAR truncado.");
                                out.write(buf,0,n);
                                written+=n;
                            }
                        }
                        Os.chmod(target.getAbsolutePath(),0600);
                    }
                    if(count%300==0)progress.entries(count);
                }
            }
            // Late symlinks can never be followed while writing entries.
            for(String[] link:pending) {
                File target=confined(stage,link[0]);
                if(Files.exists(target.toPath(),LinkOption.NOFOLLOW_LINKS))
                    throw new IOException("Symlink conflita com arquivo.");
                File up=target.getParentFile();
                if(!up.isDirectory()&&!up.mkdirs())throw new IOException("Pai do link ausente.");
                Os.symlink(link[1],target.getAbsolutePath());
            }
            String prefixRoot="";
            if(!new File(stage,"system.reg").isFile() || !new File(stage,"drive_c").isDirectory()){
                File[] roots=stage.listFiles();
                if(roots==null || roots.length!=1 || !roots[0].isDirectory()
                    || Files.isSymbolicLink(roots[0].toPath()))
                    throw new IOException("Template não contém um Wineprefix reconhecível.");
                prefixRoot=roots[0].getName();
            }
            File actual=prefixRoot.isEmpty()?stage:confined(stage,prefixRoot);
            if(!new File(actual,"system.reg").isFile()
                || !new File(actual,"drive_c").isDirectory())
                throw new IOException("Faltam system.reg e drive_c no template.");
            JSONObject receipt=new JSONObject();
            receipt.put("game",validId(game));
            receipt.put("wineTag",wine.tag);
            receipt.put("wineSha256",wine.digest);
            receipt.put("prefixRoot",prefixRoot);
            receipt.put("state","TEMPLATE_ONLY");
            receipt.put("hostRootDriveExcluded",true);
            try(FileOutputStream out=new FileOutputStream(new File(stage,MARKER))) {
                out.write(receipt.toString().getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            if(cancelled.get())throw new IOException("Cancelado antes de concluir.");
            try {
                Files.move(stage.toPath(),destination.toPath(),StandardCopyOption.ATOMIC_MOVE);
            }catch(AtomicMoveNotSupportedException unsupported) {
                if(!stage.renameTo(destination))throw new IOException("Falha ao finalizar prefixo.");
            }
            return destination;
        }catch(Exception failure) {
            WinePackageInstaller.deleteTree(stage);
            throw failure;
        }
    }

    /** Deletes only a canonical UUID game prefix under our own private workspace. */
    public static void remove(Context context,Game game) throws IOException {
        File destination=location(context,game);
        if(!destination.exists())return;
        File parent=destination.getParentFile().getCanonicalFile();
        if(!destination.getCanonicalFile().getParentFile().equals(parent))
            throw new IOException("Prefixo fora do local permitido.");
        WinePackageInstaller.deleteTree(destination);
    }

    public static int cleanupUnfinished(Context context) throws IOException {
        File base=new File(BionicWorkspace.prepare(context),"prefixes");
        File[] all=base.listFiles();
        if(all==null)throw new IOException("Não foi possível verificar prefixos.");
        int removed=0;
        for(File f:all) {
            if(!f.getName().matches("\\.prefix-stage-[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}"))
                continue;
            if(Files.isSymbolicLink(f.toPath()) || !f.isDirectory())continue;
            if(!f.getCanonicalFile().getParentFile().equals(base.getCanonicalFile()))
                throw new IOException("Pasta temporária fora do workspace.");
            WinePackageInstaller.deleteTree(f);
            removed++;
        }
        return removed;
    }
}
