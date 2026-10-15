package dev.velowin.app.engine;

/** Explicit, testable isolation between Android Bionic and Linux glibc runtimes. */
public final class RuntimeStrategy {
    public enum Host { BIONIC, GLIBC }
    public enum WineArch { X86_64, ARM64EC }
    public enum Translator { BOX64, FEX, ARM64EC_NATIVE }
    public static final class Selection {
        public final Host host;
        public final WineArch wine;
        public final Translator translator;
        public final String explanation;
        Selection(Host h, WineArch w, Translator t, String e) {
            host=h; wine=w; translator=t; explanation=e;
        }
    }
    private RuntimeStrategy() {}
    public static Selection recommend(String pe, boolean experimentalArm64ec) {
        if ("arm64".equals(pe) && experimentalArm64ec)
            return new Selection(Host.BIONIC, WineArch.ARM64EC, Translator.ARM64EC_NATIVE,
                "ARM64EC experimental: requer Wine e dependências próprias.");
        return new Selection(Host.BIONIC, WineArch.X86_64, Translator.BOX64,
            "Android/Bionic Box64 + Wine x86-64 é a rota principal; precisa de backend completo.");
    }
    public static boolean compatible(Host archive, Host host) { return archive == host; }
}
