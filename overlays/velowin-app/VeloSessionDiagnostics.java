package com.winlator.cmod.velowin;

import android.content.Context;
import com.winlator.cmod.runtime.container.Container;
import com.winlator.cmod.runtime.container.ContainerManager;
import com.winlator.cmod.runtime.display.environment.ImageFs;
import java.io.File;
import java.nio.file.Files;

/** Read-only checks for an actual Bionic Wine session. Never marks graphical
 * readiness from pointer visibility, which exists without any Wine window.
 */
public final class VeloSessionDiagnostics {
    private VeloSessionDiagnostics(){}
    public static String collect(Context context,Container c,ContainerManager manager){
        if(c==null)return "Contêiner não encontrado";
        StringBuilder s=new StringBuilder("VeloWin SessionCheck v18\n");
        try{
            File containerRoot=c.getRootDir();
            File rootfs=ImageFs.find(context).getRootDir();
            s.append("Contêiner #").append(c.id).append(": ").append(c.getName()).append("\n");
            s.append("ImageFS: ").append(ImageFs.find(context).isUpToDate()?"instalado":"incompleto").append("\n");
            s.append("Arquitetura: ").append(c.getExtra("wineprefixArch","x86_64")).append("\n");
            s.append("Wine: ").append(c.getWineVersion()).append("\n");
            s.append("Backend: ").append(c.getDisplayBackend()).append("\n");
            s.append("Driver: ").append(c.getGraphicsDriver()).append("\n");
            s.append("DirectX: ").append(c.getDXWrapper()).append("\n");
            s.append("Emulador: ").append(c.getEmulator()).append("\n");
            s.append("Tamanho: ").append(c.getScreenSize()).append("\n");
            boolean prefix=new File(containerRoot,".wine/system.reg").isFile();
            s.append("Prefixo Wine: ").append(prefix?"presente":"ausente/incompleto").append("\n");
            File windows=new File(containerRoot,".wine/drive_c/windows");
            boolean wfm=new File(windows,"wfm.exe").isFile();
            boolean handler=new File(windows,"winhandler.exe").isFile();
            s.append("wfm.exe: ").append(wfm?"encontrado":"AUSENTE").append("\n");
            s.append("winhandler.exe: ").append(handler?"encontrado":"AUSENTE").append("\n");
            boolean box=new File(rootfs,"usr/bin/box64").isFile()
                ||new File(rootfs,"usr/local/bin/box64").isFile();
            s.append("Binário Box64: ").append(box?"encontrado":"ausente").append("\n");
            boolean symlink=Files.isSymbolicLink(
                new File(rootfs,"home/xuser").toPath());
            s.append("Home ativa é symlink: ").append(symlink).append("\n");
            s.append("Modo recuperacao Winefile: ")
                .append(VeloBootTarget.isRecovery(c.getExtra(VeloBootTarget.MODE_KEY,"0"))
                    ?"ativado":"desativado").append("\n");
            s.append("VeloFlow: ")
                .append(c.getExtra("disFrameGen","0").equals("1")?"ativo":"desativado")
                .append("\n");
            if(!prefix) s.append("\nAlerta: prefixo não está pronto; verifique a instalação do Wine.\n");
            if(!handler||!wfm)s.append("\nAlerta: arquivos de desktop ausentes. "
                +"No próximo boot o motor tentará repará-los; se falhar, "
                +"usará Winefile/explorer nativo.\n");
            if(!box&&!"arm64ec".equalsIgnoreCase(c.getExtra("wineprefixArch","")))
                s.append("\nAlerta: Box64 ausente. Instale o runtime de tradução x86_64.\n");
            if("dxvk+vkd3d".equalsIgnoreCase(c.getDXWrapper()))
                s.append("\nDica Mali: para tela preta teste Modo seguro "
                    +"WineD3D/X11 antes de DXVK.\n");
            s.append("\nCursor aparecendo não confirma janela Wine. "
                +"Recolha logcat da tag VeloWinSession e ContainerLaunch.\n");
        }catch(Exception e){
            s.append("Diagnóstico parcial: ").append(e.getClass().getSimpleName());
        }
        return s.toString();
    }
}
