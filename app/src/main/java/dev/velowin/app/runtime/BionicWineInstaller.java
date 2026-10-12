package dev.velowin.app.runtime;

import android.content.Context;
import android.system.Os;
import com.github.luben.zstd.ZstdInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Inert installation of the *real* zstd-compressed GameNative Bionic WCP TAR.
 * Hard separation from glibc and no execution, Wineprefix setup or X server.
 */
public final class BionicWineInstaller {
    private static final int MAX_ENTRIES = 240000;
    private static final long MAX_UNPACKED = 8L * 1024 * 1024 * 1024;
    private static final long MAX_FILE = 2L * 1024 * 1024 * 1024;
    public interface Progress { void count(int entries); }
    private BionicWineInstaller() {}

    private static void validate(BionicRelease entry) throws IOException {
        if (entry == null || !BionicReleasePolicy.valid(entry.tag,entry.name,entry.uri,
                    "sha256:"+entry.digest,entry.size))
            throw new IOException("Manifesto de download Bionic inválido.");
    }

    public static File folder(Context context,BionicRelease entry) throws IOException {
        validate(entry);
        File base=new File(context.getFilesDir(),"bionic-wine-filesystems");
        return new File(base,entry.tag+"--"+entry.name.substring(0,entry.name.length()-4));
    }

    public static boolean installed(Context context,BionicRelease entry) {
        try{
            File root=folder(context,entry), manifest=new File(root,".velowin-bionic.json");
            if(!manifest.isFile()||manifest.length()>2048)return false;
            byte[] data=Files.readAllBytes(manifest.toPath());
            JSONObject stored=new JSONObject(new String(data,java.nio.charset.StandardCharsets.UTF_8));
            if(!entry.digest.equals(stored.optString("sha256"))
                    || !entry.tag.equals(stored.optString("tag"))
                    || !entry.name.equals(stored.optString("file")))return false;
            File wine=new File(root,"bin/wine");
            if(!wine.isFile()||!contains(root,wine))return false;
            return ElfVerifier.isMachine(wine,"ARM64EC".equals(entry.arch)?183:62);
        }catch(Exception ignored){return false;}
    }

    public static File install(Context context,BionicRelease entry,
                               AtomicBoolean cancel,Progress progress) throws Exception {
        validate(entry);
        File file=BionicArchiveStore.verifiedFile(context,entry);
        if(!file.isFile() || file.length()!=entry.size)
            throw new IOException("WCP ausente/incompleto.");
        if(installed(context,entry))return folder(context,entry);
        if(cancel.get())throw new IOException("Operação cancelada.");
        if(!entry.digest.equals(checksum(file,cancel)))
            throw new IOException("Checksum WCP alterado; faça download novamente.");

        try(InputStream in=new FileInputStream(file)){
            byte[] header=new byte[4];
            if(in.read(header)!=4 || (header[0]&255)!=0x28 || (header[1]&255)!=0xb5
                    || (header[2]&255)!=0x2f || (header[3]&255)!=0xfd)
                throw new IOException("Este WCP não usa Zstandard TAR; formato recusado.");
        }

        File destination=folder(context,entry);
        if(destination.exists())throw new IOException(
            "Já existe preparação incompleta. Remova-a antes de reinstalar.");
        File base=destination.getParentFile();
        if(!base.isDirectory()&&!base.mkdirs())
            throw new IOException("Impossível preparar armazenamento Bionic.");
        File stage=new File(base,".staging-"+UUID.randomUUID());
        if(!stage.mkdir())throw new IOException("Falha ao criar pasta de trabalho.");

        Set<String> seen=new HashSet<>();
        Set<String> links=new HashSet<>();
        List<String[]> symbolicLinks=new ArrayList<>();
        long total=0;
        int count=0;
        try{
            try(InputStream source=new BufferedInputStream(new FileInputStream(file),65536);
                ZstdInputStream decoded=new ZstdInputStream(source);
                TarArchiveInputStream tar=new TarArchiveInputStream(decoded)){
                TarArchiveEntry entryTar;
                byte[] buf=new byte[65536];
                while((entryTar=tar.getNextEntry())!=null){
                    if(cancel.get()||Thread.currentThread().isInterrupted())
                        throw new IOException("Instalação cancelada.");
                    if(++count>MAX_ENTRIES||!tar.canReadEntryData(entryTar))
                        throw new IOException("Entrada TAR inválida/quantidade excessiva.");

                    String name=ArchiveSafety.member(entryTar.getName());
                    if(name==null)throw new IOException("Traversal TAR bloqueado.");
                    if(name.isEmpty()){
                        if(entryTar.isDirectory())continue;
                        throw new IOException("Arquivo WCP sem nome.");
                    }
                    if(ArchiveSafety.conflicts(name,links) || !seen.add(name))
                        throw new IOException("Arquivo TAR duplicado ou sob symlink: "+name);

                    long length=entryTar.getSize();
                    if(length<0||length>MAX_FILE||total>MAX_UNPACKED-length)
                        throw new IOException("WCP excedeu limite descompactado.");
                    total+=length;

                    File target=new File(stage,name);
                    if(!contains(stage,target))
                        throw new IOException("Arquivo fora do diretório Bionic.");
                    if(entryTar.isSymbolicLink()){
                        if(ArchiveSafety.symlinkTarget(name,entryTar.getLinkName())==null)
                            throw new IOException("Link simbólico sai da pasta Wine.");
                        links.add(name);
                        symbolicLinks.add(new String[]{name,entryTar.getLinkName()});
                        continue;
                    }
                    if(!entryTar.isFile()&&!entryTar.isDirectory())
                        throw new IOException("Tipo TAR especial recusado.");

                    if(entryTar.isDirectory()){
                        if(target.exists()&&!target.isDirectory())
                            throw new IOException("Diretório sobre arquivo.");
                        if(!target.isDirectory()&&!target.mkdirs())
                            throw new IOException("Falha ao criar diretório.");
                    }else{
                        File parent=target.getParentFile();
                        if(!parent.isDirectory()&&!parent.mkdirs())
                            throw new IOException("Falha ao criar pasta de arquivo.");
                        if(Files.exists(target.toPath(),LinkOption.NOFOLLOW_LINKS))
                            throw new IOException("Arquivo sobreposto.");
                        if(stage.getUsableSpace()<length+64L*1024*1024)
                            throw new IOException("Sem armazenamento para WCP.");
                        try(FileOutputStream out=new FileOutputStream(target)){
                            long written=0;
                            while(written<length){
                                if(cancel.get()||Thread.currentThread().isInterrupted())
                                    throw new IOException("Extração cancelada.");
                                int n=tar.read(buf,0,(int)Math.min(buf.length,length-written));
                                if(n<0)throw new IOException("WCP truncado.");
                                out.write(buf,0,n);
                                written+=n;
                            }
                        }
                        Os.chmod(target.getAbsolutePath(),
                            (entryTar.getMode() & 0111)!=0 ? 0700 : 0600);
                    }
                    if(count%250==0)progress.count(count);
                }
            }

            // Links are created only AFTER regular files; no TAR entry is ever written through them.
            for(String[] link:symbolicLinks){
                File target=new File(stage,link[0]);
                if(Files.exists(target.toPath(),LinkOption.NOFOLLOW_LINKS))
                    throw new IOException("Symlink sobrescreve arquivo/diretório.");
                if(!contains(stage,target))
                    throw new IOException("Symlink escapando da pasta.");
                File parent=target.getParentFile();
                if(!parent.isDirectory()&&!parent.mkdirs())
                    throw new IOException("Pai de symlink indisponível.");
                Os.symlink(link[1],target.getAbsolutePath());
            }

            File profile=new File(stage,"profile.json");
            if(!profile.isFile()||profile.length()>64*1024 || !contains(stage,profile))
                throw new IOException("Manifesto profile.json inválido.");
            JSONObject document=new JSONObject(
                new String(Files.readAllBytes(profile.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8));
            JSONObject wineProfile=document.optJSONObject("wine");
            if(!"Proton".equals(document.optString("type"))
                    || wineProfile==null || !"bin".equals(wineProfile.optString("binPath"))
                    || !"lib".equals(wineProfile.optString("libPath"))
                    || !"prefixPack.txz".equals(wineProfile.optString("prefixPack")))
                throw new IOException("O pacote não possui layout Wine Bionic reconhecido.");

            File executable=new File(stage,"bin/wine");
            if(!executable.isFile()||!contains(stage,executable))
                throw new IOException("WCP não possui bin/wine confiável.");
            int machine="ARM64EC".equals(entry.arch)?183:62;
            if(!ElfVerifier.isMachine(executable,machine))
                throw new IOException("Executável Wine não corresponde à arquitetura "+entry.arch);
            // PrefixPack is a compressed template; NEVER automatically run wineboot here.
            File prefixPack=new File(stage,"prefixPack.txz");
            if(!prefixPack.isFile()||!contains(stage,prefixPack))
                throw new IOException("WCP sem template prefixPack.txz.");

            JSONObject marker=new JSONObject();
            marker.put("tag",entry.tag);
            marker.put("file",entry.name);
            marker.put("sha256",entry.digest);
            marker.put("abi","ANDROID_BIONIC");
            marker.put("architecture",entry.arch);
            try(FileOutputStream out=new FileOutputStream(new File(stage,".velowin-bionic.json"))){
                out.write(marker.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            if(cancel.get())throw new IOException("Operação cancelada.");
            try{
                Files.move(stage.toPath(),destination.toPath(),StandardCopyOption.ATOMIC_MOVE);
            }catch(AtomicMoveNotSupportedException error){
                if(!stage.renameTo(destination))
                    throw new IOException("Falha ao finalizar Wine Bionic.");
            }
            return destination;
        }catch(Exception failure){
            WinePackageInstaller.deleteTree(stage);
            throw failure;
        }
    }

    /** Remove only abandoned, UUID-named staging folders, never completed installations. */
    public static int cleanupUnfinished(Context ctx) throws IOException {
        File base=new File(ctx.getFilesDir(),"bionic-wine-filesystems");
        if(!base.isDirectory())return 0;
        File[] children=base.listFiles();
        if(children==null)throw new IOException("Falha ao ler pastas Bionic.");
        int removed=0;
        for(File folder:children){
            if(!folder.getName().matches("\\.staging-[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")
                || !folder.isDirectory() || Files.isSymbolicLink(folder.toPath()))continue;
            if(!folder.getCanonicalFile().getParentFile().equals(base.getCanonicalFile()))
                throw new IOException("Destino da recuperação fora do Bionic.");
            WinePackageInstaller.deleteTree(folder);
            removed++;
        }
        return removed;
    }

    public static void uninstall(Context ctx,BionicRelease release) throws IOException {
        File installed=folder(ctx,release);
        if(!installed.exists())return;
        File parent=new File(ctx.getFilesDir(),"bionic-wine-filesystems");
        if(!installed.getCanonicalFile().getParentFile().equals(parent.getCanonicalFile()))
            throw new IOException("Pasta Wine fora do local seguro.");
        WinePackageInstaller.deleteTree(installed);
    }

    private static String checksum(File f,AtomicBoolean cancel) throws Exception {
        MessageDigest sha=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new BufferedInputStream(new FileInputStream(f))){
            byte[] b=new byte[65536];
            int n;
            while((n=in.read(b))!=-1){
                if(cancel.get())throw new IOException("Verificação cancelada.");
                sha.update(b,0,n);
            }
        }
        StringBuilder out=new StringBuilder();
        for(byte b:sha.digest())out.append(String.format(Locale.ROOT,"%02x",b&255));
        return out.toString();
    }
    private static boolean contains(File root,File target) {
        try{return target.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator);}
        catch(IOException error){return false;}
    }
}
