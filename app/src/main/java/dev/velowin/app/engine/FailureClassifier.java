package dev.velowin.app.engine;

import java.util.Locale;

/** Small explainable troubleshooting rules; never treat a text match as engine readiness. */
public final class FailureClassifier {
    private FailureClassifier() {}
    public static String explain(String rawOutput, int exitCode, boolean timedOut) {
        if(timedOut) return "O processo excedeu o tempo limite. "
            +"Verifique dependências e logs; não aumente o tempo indefinidamente.";
        String text=rawOutput==null?"":rawOutput.toLowerCase(Locale.ROOT);
        if(text.contains("cannot link executable") || text.contains("linker64")
            || text.contains("c_library") || text.contains("error: cannot locate symbol"))
            return "Falha do linker Bionic: biblioteca/símbolo Android ausente ou ABI incompatível.";
        if(text.contains("wrong elf class") || text.contains("exec format error")
            || text.contains("invalid elf") || text.contains("bad elf"))
            return "Arquitetura ELF incorreta. Confirme Wine x86-64 Bionic com Box64 ARM64.";
        if(text.contains("library") && (text.contains("not found")||text.contains("needed by"))
            || text.contains("cannot open shared object file"))
            return "Biblioteca compartilhada ausente. Revise o pacote Wine e dependências do runtime Bionic.";
        if(text.contains("cannot open display") || text.contains("failed to open display")
            || text.contains("x11drv") || text.contains("no x server"))
            return "Ponte gráfica Wine/X11 não disponível. O laboratório OpenGL Android não é um X server.";
        if(text.contains("vulkan") && (text.contains("missing") || text.contains("not found")
            || text.contains("failed")))
            return "Vulkan/driver insuficiente. Teste o diagnóstico Vulkan nativo e as extensões.";
        if(text.contains("permission denied") || text.contains("operation not permitted"))
            return "Operação bloqueada por permissões do Android/SELinux; verifique o local e a ABI do executável.";
        if(exitCode!=0)
            return "O autoteste falhou com código "+exitCode+". Copie a saída para investigar a primeira dependência ausente.";
        return "O autoteste concluiu sem erro aparente; isso não comprova Wineprefix, janelas, áudio ou jogos.";
    }
}
