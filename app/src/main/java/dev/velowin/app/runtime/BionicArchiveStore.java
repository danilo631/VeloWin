package dev.velowin.app.runtime;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.net.ssl.HttpsURLConnection;

/**
 * Independent Bionic package pipeline. WCPs are downloaded and reverified, not
 * unpacked or executed: Android/WCP layout needs a dedicated validated installer.
 */
public final class BionicArchiveStore {
    public interface Progress { void percent(int value); }
    private BionicArchiveStore() {}
    public static File directory(Context context) throws IOException {
        File dir = new File(context.getFilesDir(),"bionic-wcp");
        if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Falha no armazenamento Bionic.");
        return dir;
    }
    public static File verifiedFile(Context context,BionicRelease e) throws IOException {
        return new File(directory(context),e.tag+"--"+e.name);
    }
    public static File download(Context context,BionicRelease e,
                                AtomicBoolean cancellation,Progress progress) throws Exception {
        if(e==null||!BionicReleasePolicy.valid(e.tag,e.name,e.uri,
                "sha256:"+e.digest,e.size))
            throw new IOException("Metadados Wine Bionic inválidos.");
        File root=directory(context);
        if(root.getUsableSpace()<e.size+64L*1024*1024)
            throw new IOException("Espaço insuficiente.");
        File goal=verifiedFile(context,e);
        File stage=new File(root,e.tag+"--"+e.name+".part");
        if(stage.exists()&&!stage.delete())throw new IOException("Download parcial inacessível.");
        MessageDigest sha=MessageDigest.getInstance("SHA-256");
        long received=0;
        try{
            HttpsURLConnection conn=RuntimeCatalog.open(e.uri);
            try {
                int hops=0;
                while(conn.getResponseCode()/100==3 && hops++<5) {
                    String next=conn.getHeaderField("Location");
                    if(!ReleasePolicy.trustedRedirect(next))
                        throw new IOException("Redirecionamento CDN inseguro.");
                    conn.disconnect();
                    conn=RuntimeCatalog.open(next);
                }
                if(conn.getResponseCode()!=HttpURLConnection.HTTP_OK)
                    throw new IOException("GameNative HTTP "+conn.getResponseCode());
                long advertised=conn.getContentLengthLong();
                if(advertised>=0 && advertised!=e.size)
                    throw new IOException("Tamanho remoto inesperado.");
                try(InputStream in=conn.getInputStream();FileOutputStream out=new FileOutputStream(stage)) {
                    byte[] buf=new byte[65536];
                    int n,prev=-1;
                    while((n=in.read(buf))!=-1){
                        if(cancellation.get()||Thread.currentThread().isInterrupted())
                            throw new IOException("Download cancelado.");
                        received+=n;
                        if(received>e.size)throw new IOException("Arquivo maior que release.");
                        sha.update(buf,0,n);
                        out.write(buf,0,n);
                        int pct=(int)(100*received/e.size);
                        if(pct!=prev){prev=pct;progress.percent(pct);}
                    }
                    out.getFD().sync();
                }
            } finally {conn.disconnect();}
            if(received!=e.size)throw new IOException("Arquivo incompleto.");
            StringBuilder hash=new StringBuilder();
            for(byte x:sha.digest())hash.append(String.format(Locale.ROOT,"%02x",x&255));
            if(!e.digest.equals(hash.toString()))throw new IOException("SHA-256 WCP não confere.");
            if(goal.exists()&&!goal.delete())throw new IOException("Pacote antigo não substituído.");
            if(!stage.renameTo(goal))throw new IOException("Falha ao finalizar WCP.");
            return goal;
        }catch(Exception fail){
            stage.delete();
            throw fail;
        }
    }
    public static boolean exists(Context context,BionicRelease e) {
        try{File f=verifiedFile(context,e);return f.isFile()&&f.length()==e.size;}
        catch(IOException ex){return false;}
    }
}
