package dev.velowin.app.engine;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Documented Box64 settings based on upstream docs/USAGE.md, not magic fps claims.
 * A preset only changes the environment of a manually requested diagnostic.
 */
public final class Box64Tuning {
    public enum Preset { STABILITY, BALANCED, PERFORMANCE }
    private Box64Tuning() {}

    public static Map<String,String> environment(Preset preset) {
        LinkedHashMap<String,String> vars = new LinkedHashMap<>();
        vars.put("BOX64_NOBANNER", "1");
        vars.put("BOX64_LOG", "1");
        switch (preset) {
            case STABILITY:
                vars.put("BOX64_DYNAREC_SAFEFLAGS", "2");
                vars.put("BOX64_DYNAREC_STRONGMEM", "1");
                vars.put("BOX64_DYNAREC_BIGBLOCK", "0");
                vars.put("BOX64_DYNAREC_CALLRET", "0");
                break;
            case BALANCED:
                vars.put("BOX64_DYNAREC_SAFEFLAGS", "1");
                vars.put("BOX64_DYNAREC_STRONGMEM", "0");
                vars.put("BOX64_DYNAREC_BIGBLOCK", "2");
                vars.put("BOX64_DYNAREC_CALLRET", "2");
                break;
            case PERFORMANCE:
                vars.put("BOX64_DYNAREC_SAFEFLAGS", "1");
                vars.put("BOX64_DYNAREC_STRONGMEM", "0");
                vars.put("BOX64_DYNAREC_BIGBLOCK", "3");
                vars.put("BOX64_DYNAREC_CALLRET", "2");
                break;
            default: throw new IllegalArgumentException("Preset desconhecido.");
        }
        return Collections.unmodifiableMap(vars);
    }

    public static Preset fromGame(String raw) {
        if ("STABILITY".equals(raw)) return Preset.STABILITY;
        if ("PERFORMANCE".equals(raw)) return Preset.PERFORMANCE;
        return Preset.BALANCED;
    }

    public static String explanation(Preset preset) {
        switch (preset) {
            case STABILITY: return "Mais flags/ordenação de memória e blocos menores: prioriza compatibilidade (pode reduzir FPS).";
            case PERFORMANCE: return "Blocos dynarec maiores, sujeito a falhas em programas com JIT ou automodificação.";
            default: return "Padrões próximos do Box64 upstream, ponto inicial recomendado.";
        }
    }
}
