package com.winlator.cmod.velowin;

import android.app.Activity;
import android.content.Intent;
import com.winlator.cmod.runtime.container.Container;
import com.winlator.cmod.runtime.container.ContainerManager;
import com.winlator.cmod.runtime.display.XServerDisplayActivity;
import com.winlator.cmod.runtime.display.environment.ImageFs;
import dev.velowin.core.VeloEnginePort;

/** The ONLY new v19 module depending on the pinned Wine/XServer launcher.
 * Replace this adapter when VeloWin's own native engine matures.
 */
public final class VeloEngineAdapter implements VeloEnginePort {
    private final Activity host;
    private final ContainerManager manager;
    public VeloEngineAdapter(Activity host,ContainerManager manager){
        this.host=host;this.manager=manager;
    }
    @Override public void launch(LaunchRequest request){
        if(!ImageFs.find(host).isUpToDate())
            throw new IllegalStateException("ImageFS incompleto; instale o runtime");
        Container selected=manager.getContainerById(request.containerId);
        if(selected==null)throw new IllegalStateException("Contêiner não encontrado");
        if(request.kind==LaunchKind.LINUX_SESSION&&!selected.isGamescopeRuntime())
            throw new IllegalArgumentException("Crie contêiner Linux separado");
        if(request.kind!=LaunchKind.LINUX_SESSION&&selected.isGamescopeRuntime())
            throw new IllegalArgumentException("Contêiner Linux não é Wine");
        Intent intent=new Intent(host,XServerDisplayActivity.class);
        intent.putExtra("container_id",request.containerId);
        if(request.kind==LaunchKind.WINDOWS_PROGRAM){
            // Existing launch API: boot_exe is resolved inside the guest;
            // never pass untrusted executable paths through a shell.
            if(request.executable.indexOf('\n')>=0 || request.executable.indexOf('\r')>=0
                ||request.executable.length()>2048)
                throw new IllegalArgumentException("Comando Windows inválido");
            intent.putExtra("boot_exe",request.executable);
        }
        VeloSessionJournal.begin(request.containerId);
        try {
            host.startActivity(intent);
        } catch (RuntimeException error) {
            VeloSessionJournal.markFailed(request.containerId,
                "Falha Activity: "+error.getClass().getSimpleName());
            throw error;
        }
    }
}
