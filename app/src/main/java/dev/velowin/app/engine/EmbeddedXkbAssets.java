package dev.velowin.app.engine;

import android.content.Context;
import android.content.res.AssetManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.HashSet;
import java.util.Set;

/**
 * Installs the keyboard maps required by the pinned upstream Xorg engine.
 * Assets are bundled from the build image's licensed xkb-data package.
 * Installation is versioned and scoped to this application's private UID.
 */
public final class EmbeddedXkbAssets {
    private EmbeddedXkbAssets() {}
    private static final String ROOT="xkb-v1";
    private static final long MAX_TOTAL=32L*1024*1024;
    private static final int MAX_FILES=5000;

    public static File ensure(Context ctx)throws IOException {
        if(ctx==null)throw new IOException("Contexto XKB ausente.");
        File root=new File(ctx.getFilesDir(),ROOT);
        File marker=new File(root,".velowin-complete");
        if(marker.isFile()&&new File(root,"rules/evdev").isFile()
            &&new File(root,"symbols/us").isFile())
            return root.getCanonicalFile();
        if(root.exists())throw new IOException(
            "Instalação XKB incompleta; dados preservados para diagnóstico.");
        if(!root.mkdirs())throw new IOException("Falha criando pasta XKB.");
        State state=new State();
        try{
            for(String sub:new String[]{"rules","keycodes","types","compat","symbols","geometry"}) {
                copyFolder(ctx.getAssets(),"xkb/"+sub,new File(root,sub),state);
            }
            if(!new File(root,"rules/evdev").isFile()
                || !new File(root,"symbols/us").isFile())
                throw new IOException("XKB sem regras evdev e layout us.");
            if(!marker.createNewFile())throw new IOException("Manifesto XKB ausente.");
            return root.getCanonicalFile();
        }catch(IOException e){
            // Never start Xorg against partial keyboard data.
            throw new IOException("Preparação XKB falhou: "+e.getMessage(),e);
        }
    }
    private static final class State {int files;long bytes;}
    private static void copyFolder(AssetManager assets,String path,File folder,State state)
            throws IOException {
        if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("Pasta XKB inválida.");
        String[] names=assets.list(path);
        if(names==null||names.length==0)throw new IOException("Asset XKB ausente: "+path);
        Set<String> seen=new HashSet<>();
        for(String name:names){
            if(name==null||!name.matches("[A-Za-z0-9_+@.,-]{1,90}")
                ||!seen.add(name))throw new IOException("Nome XKB não permitido.");
            String asset=path+"/"+name;
            File destination=new File(folder,name);
            if(Files.isSymbolicLink(destination.toPath()))
                throw new IOException("Link XKB inesperado.");
            String[] children=assets.list(asset);
            if(children!=null&&children.length>0) {
                copyFolder(assets,asset,destination,state);
            }else{
                if(++state.files>MAX_FILES)throw new IOException("XKB excede arquivos.");
                try(InputStream in=assets.open(asset);
                    FileOutputStream out=new FileOutputStream(destination)){
                    byte[] buf=new byte[16384];
                    int n;
                    while((n=in.read(buf))!=-1){
                        if(state.bytes>MAX_TOTAL-n)throw new IOException("XKB excede tamanho.");
                        state.bytes+=n;out.write(buf,0,n);
                    }
                }
            }
        }
    }
}
