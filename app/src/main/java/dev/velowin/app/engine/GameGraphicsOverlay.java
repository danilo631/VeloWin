package dev.velowin.app.engine;

import android.content.Context;
import dev.velowin.app.library.Game;
import dev.velowin.app.runtime.ComponentRegistry;
import dev.velowin.app.runtime.ComponentRelease;
import dev.velowin.app.runtime.ComponentStore;
import dev.velowin.app.runtime.WinePackageInstaller;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Deploy vetted x86_64 Windows DLLs into ONE imported game's private directory.
 * No privileged driver changes and no Wineprefix replacement.
 */
public final class GameGraphicsOverlay {
    private GameGraphicsOverlay(){}
    private static final String MARKER=".velowin-graphics.json";

    private static File gameDir(Context ctx,Game game)throws IOException {
        if(game==null || game.id==null || !"x86_64".equals(game.architecture))
            throw new IOException("Somente executáveis Windows x86_64 são suportados.");
        UUID id;
        try {id=UUID.fromString(game.id);}
        catch(Exception ex){throw new IOException("UUID do jogo inválido.",ex);}
        if(!id.toString().equals(game.id.toLowerCase(Locale.ROOT)))
            throw new IOException("UUID não canônico.");
        File dir=new File(new File(BionicWorkspace.prepare(ctx),"games"),id.toString());
        if(Files.isSymbolicLink(dir.toPath()) || (!dir.isDirectory()&&!dir.mkdirs()))
            throw new IOException("Pasta do jogo indisponível.");
        return dir.getCanonicalFile();
    }
    private static String[] dlls(ComponentRelease.Kind kind) {
        return kind==ComponentRelease.Kind.DXVK
            ?new String[]{"d3d9.dll","d3d10core.dll","d3d11.dll","dxgi.dll"}
            :new String[]{"d3d12.dll","d3d12core.dll"};
    }
    private static String hash(File file)throws Exception {
        MessageDigest md=MessageDigest.getInstance("SHA-256");
        try(FileInputStream in=new FileInputStream(file)){
            byte[] b=new byte[65536];int n;
            while((n=in.read(b))!=-1)md.update(b,0,n);
        }
        StringBuilder out=new StringBuilder();
        for(byte b:md.digest())out.append(String.format(Locale.ROOT,"%02x",b&255));
        return out.toString();
    }
    private static File source(Context ctx,ComponentRelease e,String name)throws IOException {
        // Verify the exact stored upstream release on every deployment.
        ComponentRelease verified=ComponentRegistry.get(ctx,e.kind);
        if(verified==null || !verified.sha256.equals(e.sha256) || !verified.tag.equals(e.tag))
            throw new IOException("Release gráfico não autenticado.");
        File root=ComponentStore.folder(ctx,e);
        File dll=new File(root,"x64/"+name).getCanonicalFile();
        if(!dll.getPath().startsWith(root.getCanonicalPath()+File.separator)
            || !dll.isFile() || Files.isSymbolicLink(dll.toPath()))
            throw new IOException("DLL x64 indisponível: "+name);
        return dll;
    }

    public static String status(Context ctx,Game game) {
        try {
            File dir=gameDir(ctx,game),marker=new File(dir,MARKER);
            if(!marker.isFile())return "Nenhuma camada Direct3D injetada para este jogo.";
            JSONObject receipt=new JSONObject(new String(Files.readAllBytes(marker.toPath()),
                StandardCharsets.UTF_8));
            JSONArray items=receipt.getJSONArray("files");
            if(items.length()<1 || items.length()>6)return "Registro de DLL inválido.";
            for(int i=0;i<items.length();i++){
                JSONObject item=items.getJSONObject(i);
                String name=item.getString("name");
                if(!name.matches("[a-z0-9]{3,18}\\.dll"))
                    return "Registro contém DLL inválida.";
                File dll=new File(dir,name);
                if(!dll.isFile()||Files.isSymbolicLink(dll.toPath())
                    || !hash(dll).equals(item.getString("sha256")))
                    return "DLL ausente ou modificada: "+name;
            }
            return "Injeção local verificada: "+receipt.getString("kind")
                +" "+receipt.getString("tag")+" ("+items.length()+" DLLs). "
                +"Renderização Vulkan ainda depende de X11 e GPU.";
        }catch(Exception e){return "Injeção Direct3D não validada: "+e.getMessage();}
    }

    /**
     * Only installs files when no pre-existing same-name DLL or overlay exists.
     * A failed copy cleans its own files, preserving pre-existing game assets.
     */
    public static String deploy(Context ctx,Game game,ComponentRelease.Kind kind)throws Exception {
        if(kind!=ComponentRelease.Kind.DXVK && kind!=ComponentRelease.Kind.VKD3D_PROTON)
            throw new IOException("Componente não é Direct3D.");
        ComponentRelease packageRelease=ComponentRegistry.get(ctx,kind);
        if(packageRelease==null)throw new IOException("Baixe e valide este componente primeiro.");
        File dir=gameDir(ctx,game);
        File marker=new File(dir,MARKER);
        if(Files.exists(marker.toPath(),LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Já existe uma camada gráfica. Remova-a antes de trocar.");
        ArrayList<File> sources=new ArrayList<>();
        ArrayList<File> targets=new ArrayList<>();
        ArrayList<String> names=new ArrayList<>();
        for(String name:dlls(kind)){
            File original;
            try{original=source(ctx,packageRelease,name);}
            catch(IOException missing){
                if("d3d12core.dll".equals(name))continue; // upstream optional in some releases
                throw missing;
            }
            File target=new File(dir,name);
            if(Files.exists(target.toPath(),LinkOption.NOFOLLOW_LINKS))
                throw new IOException("DLL do jogo já existe; não sobrescrever: "+name);
            sources.add(original);targets.add(target);names.add(name);
        }
        if(sources.isEmpty())throw new IOException("Pacote sem DLLs aplicáveis.");
        File stage=new File(dir,".velowin-graphics-stage-"+UUID.randomUUID());
        if(!stage.mkdir())throw new IOException("Falha no staging gráfico.");
        List<File> installed=new ArrayList<>();
        try {
            JSONArray manifestFiles=new JSONArray();
            for(int i=0;i<sources.size();i++){
                File destination=new File(stage,names.get(i));
                Files.copy(sources.get(i).toPath(),destination.toPath());
                JSONObject record=new JSONObject();
                record.put("name",names.get(i));record.put("sha256",hash(destination));
                manifestFiles.put(record);
            }
            for(int i=0;i<targets.size();i++){
                // No REPLACE_EXISTING: reject racing imports and preserve original.
                Files.move(new File(stage,names.get(i)).toPath(),targets.get(i).toPath());
                installed.add(targets.get(i));
            }
            JSONObject receipt=new JSONObject();
            receipt.put("kind",kind.name());receipt.put("tag",packageRelease.tag);
            receipt.put("releaseSha256",packageRelease.sha256);
            receipt.put("files",manifestFiles);
            File stagedMarker=new File(stage,MARKER);
            try(FileOutputStream out=new FileOutputStream(stagedMarker)){
                out.write(receipt.toString().getBytes(StandardCharsets.UTF_8));
                out.getFD().sync();
            }
            Files.move(stagedMarker.toPath(),marker.toPath());
            return "DLLs "+kind+" aplicadas na pasta privada do jogo. "+status(ctx,game);
        }catch(Exception fail){
            for(File installedFile:installed)installedFile.delete();
            throw fail;
        }finally{
            // Remove any incomplete staging DLLs without touching game-owned files.
            WinePackageInstaller.deleteTree(stage);
        }
    }

    public static String overrides(Context ctx,Game game,String profile)throws IOException {
        if(!"DXVK".equals(profile)&&!"VKD3D".equals(profile))return "";
        File dir=gameDir(ctx,game),marker=new File(dir,MARKER);
        if(!marker.isFile())throw new IOException("Perfil requer DLLs não instaladas.");
        try {
            JSONObject json=new JSONObject(new String(Files.readAllBytes(marker.toPath()),
                StandardCharsets.UTF_8));
            String kind="DXVK".equals(profile)?"DXVK":"VKD3D_PROTON";
            if(!kind.equals(json.getString("kind")))
                throw new IOException("Camada selecionada difere das DLLs instaladas.");
            JSONArray files=json.getJSONArray("files");
            StringBuilder override=new StringBuilder();
            for(int i=0;i<files.length();i++){
                JSONObject entry=files.getJSONObject(i);
                String filename=entry.getString("name");
                if(!filename.matches("[a-z0-9]{3,18}\\.dll"))
                    throw new IOException("Registro de DLL não confiável.");
                File dll=new File(dir,filename);
                if(!dll.isFile()||Files.isSymbolicLink(dll.toPath())
                    || !hash(dll).equals(entry.getString("sha256")))
                    throw new IOException("DLL modificada: "+filename);
                if(override.length()!=0)override.append(";");
                override.append(filename.substring(0,filename.length()-4)).append("=n,b");
            }
            if(files.length()==0)throw new IOException("Camada gráfica vazia.");
            return override.toString();
        }catch(IOException fail){throw fail;}
        catch(Exception other){throw new IOException("Não foi possível validar DLLs.",other);}
    }

    public static void remove(Context ctx,Game game)throws Exception {
        File dir=gameDir(ctx,game),marker=new File(dir,MARKER);
        if(!marker.isFile())return;
        JSONObject json=new JSONObject(new String(Files.readAllBytes(marker.toPath()),
            StandardCharsets.UTF_8));
        JSONArray files=json.getJSONArray("files");
        if(files.length()>6)throw new IOException("Manifesto Direct3D inválido.");
        for(int i=0;i<files.length();i++){
            JSONObject entry=files.getJSONObject(i);
            String name=entry.getString("name");
            if(!name.matches("[a-z0-9]{3,18}\\.dll"))
                throw new IOException("DLL registrada inválida.");
            File dll=new File(dir,name);
            if(Files.exists(dll.toPath(),LinkOption.NOFOLLOW_LINKS)){
                if(Files.isSymbolicLink(dll.toPath())||!dll.isFile()
                    || !hash(dll).equals(entry.getString("sha256")))
                    throw new IOException("DLL alterada; não será excluída: "+name);
            }
        }
        for(int i=0;i<files.length();i++){
            String name=files.getJSONObject(i).getString("name");
            File dll=new File(dir,name);
            if(dll.exists()&&!dll.delete())throw new IOException("Falha removendo "+name);
        }
        if(!marker.delete())throw new IOException("Falha removendo manifesto gráfico.");
    }
}
