package dev.velowin.app.engine;

import android.content.Context;
import dev.velowin.app.library.Game;
import dev.velowin.app.runtime.RuntimeStore;
import dev.velowin.app.runtime.WinePackageInstaller;

/**
 * Fail-closed execution gate. No x86 translator/rootfs/graphics backend is bundled
 * in VeloWin 0.2, so a filesystem prepared from Wine is NOT a runnable engine.
 */
public final class EngineGate {
    private EngineGate() {}

    public static String reason(Context context, Game game) {
        if (game == null) return "Adicione um jogo para configurar os componentes.";
        int prepared = 0;
        try {
            for (RuntimeStore.Installed wine : new RuntimeStore(context).all()) {
                if (WinePackageInstaller.installed(context, wine)) prepared++;
            }
        }
        catch (RuntimeException ignored) {}
        String status = prepared == 0
            ? "Nenhum Wine preparado. Abra Runtimes para baixar e preparar uma versão."
            : prepared + " pacote(s) Wine preparados, mas ainda sem motor ARM64.";
        return "Motor Windows ainda não integrado. " + status + "\n\nFalta integrar e validar o tradutor x86 (Box64/FEX), "
            + "rootfs Linux/glibc, Wineprefix, display/input/áudio e Vulkan/DXVK. "
            + "O VeloWin impede iniciar binários Windows sem esses módulos.";
    }

    public static boolean isReady(Context context) {
        return false;
    }
}
