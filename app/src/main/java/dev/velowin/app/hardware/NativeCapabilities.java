package dev.velowin.app.hardware;

/** Embedded Android NDK Vulkan driver/device probe, not a Windows renderer. */
public final class NativeCapabilities {
    private static final boolean LOADED;
    static {
        boolean success = false;
        try { System.loadLibrary("velowin_probe"); success = true; }
        catch (UnsatisfiedLinkError | SecurityException ignored) {}
        LOADED = success;
    }
    private NativeCapabilities() {}
    private static native String probeVulkan();
    public static String inspectVulkan() {
        if (!LOADED) return "Módulo nativo Vulkan não disponível.";
        try { return probeVulkan(); }
        catch (RuntimeException | LinkageError failure) {
            return "Falha Vulkan JNI: " + failure.getClass().getSimpleName();
        }
    }
    public static boolean available() { return LOADED; }
}
