package dev.velowin.app.engine;

import java.util.Locale;

/**
 * Restricted in-app console vocabulary. User strings are NEVER passed to a
 * shell or interpreted as command arguments. Only fixed, reviewed argv vectors
 * are constructed by ContainerSession.
 */
public final class ContainerCommand {
    public enum Action {
        HELP, STATUS, CLEAR, BOX64_VERSION, WINE_VERSION, PREFIX_PREPARE,
        PREFIX_STATUS, WINEBOOT, CMD_VERSION, RUN_EXE, STOP_WIN_SERVER, FILES
    }

    private ContainerCommand() {}

    public static Action parse(String raw) {
        if (raw == null || raw.length() > 128) return null;
        String normalized = raw.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        switch (normalized) {
            case "help": case "?": case "ajuda": return Action.HELP;
            case "status": case "info": return Action.STATUS;
            case "clear": case "limpar": return Action.CLEAR;
            case "box64 -v": case "box64 --version": return Action.BOX64_VERSION;
            case "wine --version": case "wine -v": return Action.WINE_VERSION;
            case "prefix prepare": case "preparar prefixo": return Action.PREFIX_PREPARE;
            case "prefix status": case "prefix": return Action.PREFIX_STATUS;
            case "wineboot -u": case "wineboot --init": return Action.WINEBOOT;
            case "wine cmd /c ver": case "cmd /c ver": return Action.CMD_VERSION;
            case "run exe": case "testar exe": return Action.RUN_EXE;
            case "wineserver -k": case "stop": return Action.STOP_WIN_SERVER;
            case "files": case "arquivos": return Action.FILES;
            default: return null;
        }
    }

    public static boolean requiresConsent(Action action) {
        return action == Action.WINEBOOT || action == Action.RUN_EXE;
    }

    public static String manual() {
        return "Comandos disponíveis:\n"
            + "help                — este menu\n"
            + "status              — componentes e ABI\n"
            + "files               — pastas privadas do contêiner\n"
            + "box64 -v            — executar tradutor Bionic\n"
            + "wine --version      — teste Wine + Box64\n"
            + "prefix prepare      — extrair template do prefixo do jogo\n"
            + "prefix status       — verificar prefixo\n"
            + "wineboot -u         — inicialização experimental Wine (confirmação)\n"
            + "wine cmd /c ver     — executar programa Win32 sem janela\n"
            + "run exe             — tentar EXE importado (confirmação)\n"
            + "wineserver -k       — solicitar encerramento do wineserver\n"
            + "clear               — limpar console\n\n"
            + "Não há shell Linux nem comandos arbitrários. Programas Windows "
            + "ainda podem falhar por falta de X11, áudio e outras bibliotecas.";
    }
}
