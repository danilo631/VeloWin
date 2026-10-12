package dev.velowin.app.engine;

import android.content.Context;
import dev.velowin.app.library.Game;
import dev.velowin.app.runtime.RuntimeStore;

/**
 * Feature gate for the future native game engine. Never execute arbitrary Wine
 * binaries in the Android process or report a successful launch without an engine.
 */
public final class EngineGate {
    private EngineGate() {}

    public static String reason(Context context, Game game) {
        if (game == null) return "Nenhum jogo selecionado.";
        // Only a verified native engine with ABI/rootfs/Vulkan preflights will lift this gate.
        return "Motor Windows ainda não integrado. O VeloWin 0.1 organiza jogos "
            + "e gerencia downloads verificados, mas precisa de Wine, tradutor x86 "
            + "(Box64 ou FEX), rootfs e integração gráfica para executar jogos.";
    }

    public static boolean isReady(Context context) {
        return false;
    }
}
