package dev.velowin.app.runtime;

import android.content.Context;
import org.json.JSONObject;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import dev.velowin.app.library.PeInspector;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;

/** Verified, app-private downloads. DXVK is staged, never injected in Wineprefix by download. */
public final class ComponentStore {
    private ComponentStore(){}
    public interface Progress { void report(String message); }
    private static final String[] DLLS={"d3d8","d3d9","d3d10core","d3d11","dxgi"};
    private static final long MAX_UNPACKED=300L*1024*1024;

    public static File base(Context context)throws IOException {
        File f=new File(context.getFilesDir(),"velowin-components");
        if(!f.isDirectory()&&!f.mkdirs())throw new IOException("Pasta de componentes indisponível.");
        return f;
    }
    private static File root(Context ctx,ComponentRelease release)throws IOException {
        if(!ComponentReleasePolicy.valid(release))throw new IOException("Manifesto inválido.");
        return new File(base(ctx),release.kind==ComponentRelease.Kind.DXVK?
            "dxvk/"+release.tag:"termux-x11/"+release.tag);
    }
    public static File apk(Context ctx,ComponentRelease release)throws IOException {
        if(release.kind!=ComponentRelease.Kind.TERMUX_X11_APK)
            throw new IOException("Não é APK Termux:X11.");
        return new File(root(ctx,release),"termux-x11-universal-debug.apk");
    }

    public static boolean installed(Context ctx,ComponentRelease release) {
        try{
            File folder=root(ctx,release);
            File marker=new File(folder,".velowin-component.json");
            if(!marker.isFile()||marker.length()>4096)return false;
            JSONObject receipt=new JSONObject(new String(Files.readAllBytes(marker.toPath()),
                StandardCharsets.UTF_8));
            if(!release.kind.name().equals(receipt.optString("kind"))
                || !release.tag.equals(receipt.optString("tag"))
                || !release.sha256.equals(receipt.optString("sha256")))return false;
            if(release.kind==ComponentRelease.Kind.TERMUX_X11_APK)
                return apk(ctx,release).isFile()
                    && apk(ctx,release).length()==release.size;
            // Validate actual PE DLL architecture, not just filenames.
            for(String name:new String[]{"d3d9.dll","dxgi.dll","d3d11.dll"}) {
                File f=new File(folder,"x64/"+name);
                try(InputStream in=new FileInputStream(f)){
                    if(!"x86_64".equals(PeInspector.inspect(in)))return false;
                }
            }
            return true;
        }catch(Exception ignored){return false;}
    }

    private static String hex(byte[] bytes) {
        StringBuilder result=new StringBuilder();
        for(byte b:bytes)result.append(String.format(Locale.ROOT,"%02x",b&255));
        return result.toString();
    }
    public static boolean verifyApk(Context ctx,ComponentRelease release)throws Exception {
        if(!installed(ctx,release) || release.kind!=ComponentRelease.Kind.TERMUX_X11_APK)
            return false;
        MessageDigest hash=MessageDigest.getInstance("SHA-256");
        try(InputStream in=new BufferedInputStream(new FileInputStream(apk(ctx,release)))){
            byte[] buf=new byte[65536];int n;
            while((n=in.read(buf))!=-1)hash.update(buf,0,n);
        }
        return release.sha256.equals(hex(hash.digest()));
    }

    public static File downloadAndPrepare(Context ctx,ComponentRelease release,
            AtomicBoolean cancelled,Progress progress)throws Exception {
        if(!ComponentReleasePolicy.valid(release)||cancelled==null||progress==null)
            throw new IOException("Pacote ou operação inválidos.");
        if(installed(ctx,release))return root(ctx,release);
        File target=root(ctx,release);
        if(target.exists())throw new IOException("Instalação incompleta preservada; remova-a primeiro.");
        File parent=target.getParentFile();
        if(!parent.isDirectory()&&!parent.mkdirs())
            throw new IOException("Não foi possível criar diretório de pacotes.");
        if(parent.getUsableSpace()<release.size+400L*1024*1024)
            throw new IOException("Espaço interno insuficiente para o pacote.");
        File staging=new File(parent,".stage-"+UUID.randomUUID());
        if(!staging.mkdir())throw new IOException("Falha preparando staging.");
        File archive=new File(staging,".download");
        try {
            progress.report("Baixando "+release.name+"...");
            HttpsURLConnection connection=RuntimeCatalog.open(release.url);
            try{
                int hops=0;
                while(connection.getResponseCode()/100==3 && hops++<5){
                    String next=connection.getHeaderField("Location");
                    if(!ReleasePolicy.trustedRedirect(next))
                        throw new IOException("Redirecionamento fora da CDN GitHub.");
                    connection.disconnect();
                    connection=RuntimeCatalog.open(next);
                }
                if(connection.getResponseCode()!=HttpURLConnection.HTTP_OK)
                    throw new IOException("Download GitHub HTTP "+connection.getResponseCode());
                long advertised=connection.getContentLengthLong();
                if(advertised>=0&&advertised!=release.size)
                    throw new IOException("Tamanho remoto diferente do manifesto.");
                MessageDigest digest=MessageDigest.getInstance("SHA-256");
                long total=0,checkpoint=0;
                try(InputStream in=connection.getInputStream();
                    FileOutputStream out=new FileOutputStream(archive)){
                    byte[] buf=new byte[65536];
                    int n;
                    while((n=in.read(buf))!=-1){
                        if(cancelled.get()||Thread.currentThread().isInterrupted())
                            throw new IOException("Download cancelado.");
                        if(total>release.size-n)throw new IOException("Download acima do limite.");
                        total+=n;digest.update(buf,0,n);out.write(buf,0,n);
                        if(total-checkpoint>=1024*1024){
                            checkpoint=total;
                            progress.report("Download: "+total/1048576+" de "+
                                release.size/1048576+" MiB");
                        }
                    }
                    out.getFD().sync();
                }
                if(total!=release.size || !release.sha256.equals(hex(digest.digest())))
                    throw new IOException("SHA-256 ou tamanho incorretos. Pacote rejeitado.");
            }finally{connection.disconnect();}
            if(cancelled.get())throw new IOException("Cancelado.");
            if(release.kind==ComponentRelease.Kind.DXVK) {
                progress.report("Extraindo DLLs Windows x86/x64...");
                unpackDxvk(archive,staging,cancelled);
                if(!archive.delete())throw new IOException("Falha ao liberar arquivo TAR.");
            } else {
                Files.move(archive.toPath(),new File(staging,release.name).toPath());
            }
            JSONObject receipt=new JSONObject();
            receipt.put("kind",release.kind.name());
            receipt.put("tag",release.tag);
            receipt.put("sha256",release.sha256);
            receipt.put("name",release.name);
            receipt.put("size",release.size);
            try(FileOutputStream out=new FileOutputStream(
                new File(staging,".velowin-component.json"))){
                out.write(receipt.toString().getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            if(cancelled.get())throw new IOException("Operação cancelada.");
            Files.move(staging.toPath(),target.toPath());
            progress.report("Pacote verificado e armazenado.");
            return target;
        }catch(Exception failure) {
            WinePackageInstaller.deleteTree(staging);
            throw failure;
        }
    }
    /** Only extract the five expected Direct3D DLL basenames in x32 and x64. */
    private static void unpackDxvk(File archive,File folder,
            AtomicBoolean cancelled)throws Exception {
        Set<String> seen=new HashSet<>();
        long bytes=0;
        int entries=0;
        try(InputStream source=new BufferedInputStream(new FileInputStream(archive));
            GZIPInputStream gzip=new GZIPInputStream(source);
            TarArchiveInputStream tar=new TarArchiveInputStream(gzip)){
            TarArchiveEntry member;
            byte[] buf=new byte[65536];
            while((member=tar.getNextEntry())!=null){
                if(cancelled.get())throw new IOException("Extração cancelada.");
                if(++entries>2000 || !tar.canReadEntryData(member))
                    throw new IOException("TAR DXVK inválido ou muito grande.");
                String path=ArchiveSafety.member(member.getName());
                if(path==null)throw new IOException("Caminho TAR inválido.");
                if(member.isSymbolicLink()||member.isLink())
                    throw new IOException("Links TAR não aceitos em DLLs.");
                if(!member.isFile()&&!member.isDirectory())
                    throw new IOException("Tipo TAR não regular.");
                long len=member.getSize();
                if(len<0||len>64L*1024*1024||bytes>MAX_UNPACKED-len)
                    throw new IOException("TAR DXVK excedeu tamanho máximo.");
                bytes+=len;
                if(!member.isFile())continue;
                String[] parts=path.split("/");
                if(parts.length<2)continue;
                String arch=parts[parts.length-2],file=parts[parts.length-1];
                if(!("x32".equals(arch)||"x64".equals(arch)))continue;
                boolean valid=false;
                for(String dll:DLLS)if((dll+".dll").equals(file)){valid=true;break;}
                if(!valid)continue;
                String key=arch+"/"+file;
                if(!seen.add(key))throw new IOException("DLL duplicada no TAR.");
                File destination=new File(folder,key);
                File parent=destination.getParentFile();
                if(!parent.isDirectory()&&!parent.mkdirs())
                    throw new IOException("Pasta de DLL indisponível.");
                try(FileOutputStream output=new FileOutputStream(destination)){
                    long copied=0;
                    while(copied<len){
                        if(cancelled.get())throw new IOException("Extração cancelada.");
                        int n=tar.read(buf,0,(int)Math.min(buf.length,len-copied));
                        if(n<=0)throw new IOException("DLL truncada.");
                        output.write(buf,0,n);copied+=n;
                    }
                }
                try(InputStream in=new FileInputStream(destination)){
                    String machine=PeInspector.inspect(in);
                    if(!("x64".equals(arch)?"x86_64":"x86").equals(machine))
                        throw new IOException("DLL com arquitetura PE incorreta: "+key);
                }
            }
        }
        for(String dll:new String[]{"d3d9","d3d11","dxgi"})
            if(!seen.contains("x64/"+dll+".dll"))
                throw new IOException("Pacote DXVK sem DLL x64 obrigatória: "+dll);
    }

    /** Only the exact manifest-authenticated directory may be removed. */
    public static void remove(Context ctx,ComponentRelease release)throws IOException {
        File target=root(ctx,release);
        if(!target.exists())return;
        if(Files.isSymbolicLink(target.toPath())
            || !target.getCanonicalFile().getParentFile().equals(
                target.getParentFile().getCanonicalFile()))
            throw new IOException("Caminho de pacote não autorizado.");
        WinePackageInstaller.deleteTree(target);
    }
}
