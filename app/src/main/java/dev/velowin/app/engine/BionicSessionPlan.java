package dev.velowin.app.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable, non-executable Bionic session specification. It cannot launch a
 * game until native display/audio/input bridges are independently verified.
 */
public final class BionicSessionPlan {
    public enum Translator { BOX64, FEX }
    public enum Graphics { NATIVE_VULKAN, DXVK, VKD3D, OPENGL }
    public final Translator translator;
    public final Graphics graphics;
    public final String wineAbi;
    public final String gameAbi;
    public final boolean vulkan13;
    public final List<String> blockers;

    private BionicSessionPlan(Translator translator, Graphics graphics, String wineAbi,
            String gameAbi, boolean vulkan13, List<String> blockers) {
        this.translator=translator;
        this.graphics=graphics;
        this.wineAbi=wineAbi;
        this.gameAbi=gameAbi;
        this.vulkan13=vulkan13;
        this.blockers=Collections.unmodifiableList(new ArrayList<>(blockers));
    }

    public static BionicSessionPlan evaluate(Translator cpu, Graphics gpu,
            String wineAbi, String gameAbi, boolean vulkan, boolean vulkan13,
            boolean translatorReady, boolean wineReady, boolean displayReady,
            boolean audioReady, boolean inputReady, boolean prefixReady,
            boolean dxvkReady, boolean vkd3dReady) {
        ArrayList<String> blockers=new ArrayList<>();
        if (!"ARM64EC".equals(wineAbi) && !"X86_64".equals(wineAbi))
            blockers.add("ABI Wine Bionic desconhecida");
        if ("ARM64EC".equals(wineAbi) && cpu!=Translator.FEX)
            blockers.add("Wine ARM64EC exige FEX nesta configuração");
        if ("X86_64".equals(wineAbi) && cpu!=Translator.BOX64)
            blockers.add("Wine x86_64 Bionic exige Box64 nesta configuração");
        if (!"x86".equals(gameAbi) && !"x86_64".equals(gameAbi))
            blockers.add("Executável Windows não é x86/x86_64");
        if (!translatorReady) blockers.add("Tradutor CPU não validado");
        if (!wineReady) blockers.add("Wine Bionic não validado");
        if (!prefixReady) blockers.add("Prefixo Wine não inicializado");
        if (!displayReady) blockers.add("Ponte de janelas X11/Android indisponível");
        if (!audioReady) blockers.add("Ponte de áudio Wine/Android indisponível");
        if (!inputReady) blockers.add("Ponte de controles Wine/Android indisponível");
        if (gpu==Graphics.NATIVE_VULKAN || gpu==Graphics.DXVK || gpu==Graphics.VKD3D) {
            if (!vulkan) blockers.add("Driver Vulkan não confirmado");
        }
        if (gpu==Graphics.DXVK && !dxvkReady)
            blockers.add("DXVK não validado");
        if (gpu==Graphics.VKD3D) {
            if (!vulkan13) blockers.add("Vulkan 1.3 não confirmado para VKD3D");
            if (!vkd3dReady) blockers.add("VKD3D-Proton não validado");
        }
        return new BionicSessionPlan(cpu,gpu,wineAbi,gameAbi,vulkan13,blockers);
    }

    public boolean readyForNativeLaunch() { return blockers.isEmpty(); }

    public String diagnostic() {
        return readyForNativeLaunch() ? "Pré-requisitos declarados presentes; exige smoke test real."
            : "Sessão Bionic bloqueada: " + String.join("; ",blockers);
    }
}
