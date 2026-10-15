package dev.velowin.app.engine;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/** Android-private directory layout. NOT a Linux glibc rootfs or running Wine prefix. */
public final class BionicWorkspace {
    private BionicWorkspace() {}
    public static File prepare(Context context) throws IOException {
        File root=new File(context.getFilesDir(),"bionic-workspace-v1");
        if(!root.isDirectory()&&!root.mkdirs())throw new IOException("Falha ao criar workspace.");
        for(String path:new String[]{"home","tmp","logs","cache","prefixes","mounts"}){
            File folder=new File(root,path);
            if(!folder.isDirectory()&&!folder.mkdirs())
                throw new IOException("Não foi possível criar "+path);
        }
        File marker=new File(root,".nomedia");
        if(!marker.exists())try(FileOutputStream f=new FileOutputStream(marker)){
            f.write("VeloWin Android/Bionic private workspace\n".getBytes(StandardCharsets.UTF_8));
        }
        return root;
    }
    public static String status(Context ctx) {
        File home=new File(ctx.getFilesDir(),"bionic-workspace-v1");
        if(!home.isDirectory())return "Workspace ainda não preparado.";
        return "Workspace privado preparado; não é um rootfs Linux nem um Wineprefix inicializado.";
    }
}
