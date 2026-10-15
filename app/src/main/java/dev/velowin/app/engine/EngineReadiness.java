package dev.velowin.app.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure Java readiness reasons; never conflate a working Android media pipeline
 * or Vulkan loader with a complete Windows runtime.
 */
public final class EngineReadiness {
    public final boolean isArm64;
    public final boolean box64Executable;
    public final boolean bionicWineInstalled;
    public final boolean prefixCreated;
    public final boolean displayBridge;
    public final boolean audioBridge;
    public final boolean inputBridge;
    public final boolean gpuVulkan;
    public final boolean dxvkAvailable;

    public EngineReadiness(boolean isArm64, boolean box64Executable, boolean bionicWineInstalled,
        boolean prefixCreated, boolean displayBridge, boolean audioBridge, boolean inputBridge,
        boolean gpuVulkan, boolean dxvkAvailable) {
        this.isArm64=isArm64;
        this.box64Executable=box64Executable;
        this.bionicWineInstalled=bionicWineInstalled;
        this.prefixCreated=prefixCreated;
        this.displayBridge=displayBridge;
        this.audioBridge=audioBridge;
        this.inputBridge=inputBridge;
        this.gpuVulkan=gpuVulkan;
        this.dxvkAvailable=dxvkAvailable;
    }
    public boolean canLaunchGame() {
        return isArm64 && box64Executable && bionicWineInstalled && prefixCreated &&
            displayBridge && audioBridge && inputBridge && gpuVulkan && dxvkAvailable;
    }
    public List<String> missing() {
        ArrayList<String> out=new ArrayList<>();
        if(!isArm64)out.add("CPU ARM64");
        if(!box64Executable)out.add("Box64 Bionic validado");
        if(!bionicWineInstalled)out.add("Wine Bionic instalado e validado");
        if(!prefixCreated)out.add("Wineprefix inicializado");
        if(!displayBridge)out.add("Janela Windows / X11 / Surface");
        if(!audioBridge)out.add("Ponte áudio Wine → Android");
        if(!inputBridge)out.add("Ponte teclado/gamepad → Wine");
        if(!gpuVulkan)out.add("Dispositivo Vulkan funcional");
        if(!dxvkAvailable)out.add("DXVK e dependências vulkan/Wine");
        return out;
    }
    public String report() {
        if(canLaunchGame())return "Todos os pré-requisitos declarados foram atendidos. Ainda é necessário teste de execução.";
        return "Ainda faltam componentes de execução:\n• "+String.join("\n• ",missing());
    }
}
