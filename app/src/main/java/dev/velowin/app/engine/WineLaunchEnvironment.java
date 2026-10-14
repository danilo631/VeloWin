package dev.velowin.app.engine;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Host-side environment for the x86_64 Bionic Wine / Box64 pipeline.
 * No glibc rootfs, shell commands, or external PATH entries are introduced.
 */
public final class WineLaunchEnvironment {
    private WineLaunchEnvironment() {}

    private static File within(File root, String relative) throws IOException {
        File canonicalRoot=root.getCanonicalFile();
        File target=new File(canonicalRoot,relative).getCanonicalFile();
        if(!target.getPath().startsWith(canonicalRoot.getPath()+File.separator))
            throw new IOException("Wine symlink fora do pacote: "+relative);
        return target;
    }

    public static Map<String,String> build(File wineRoot, File workspace, File prefix,
            Box64Tuning.Preset preset, Map<String,String> bridge, boolean diagnosticLogs)
            throws IOException {
        if(wineRoot==null || workspace==null || preset==null || bridge==null)
            throw new IOException("Ambiente Wine incompleto.");
        File bin=within(wineRoot,"bin");
        File wine=within(wineRoot,"bin/wine");
        if(!wine.isFile() || !ElfVerifierBridge.isBionicX64(wine))
            throw new IOException("WCP não contém bin/wine Bionic x86_64 válido.");
        File server=within(wineRoot,"bin/wineserver");

        LinkedHashMap<String,String> env=new LinkedHashMap<>(Box64Tuning.environment(preset));
        env.put("HOME",new File(workspace,"home").getAbsolutePath());
        env.put("TMPDIR",new File(workspace,"tmp").getAbsolutePath());
        env.put("XDG_CACHE_HOME",new File(workspace,"cache").getAbsolutePath());
        // Android's system PATH is explicit. Do not inherit Termux/glibc prefixes.
        env.put("PATH",bin.getAbsolutePath()+":/system/bin:/system/xbin");
        env.put("WINELOADER",wine.getAbsolutePath());
        // A WCP may omit this link; the audit reports it instead of inventing one.
        if(server.isFile())env.put("WINESERVER",server.getAbsolutePath());
        env.put("BOX64_LD_LIBRARY_PATH",
            within(wineRoot,"lib/wine/x86_64-unix").getAbsolutePath()+":"
            +within(wineRoot,"lib").getAbsolutePath());
        env.put("LC_ALL","C.UTF-8");
        env.put("WINEDEBUG",diagnosticLogs?"warn+all":"-all");
        env.put("WINEESYNC","0");
        env.put("WINEFSYNC","0");
        if(prefix!=null)env.put("WINEPREFIX",prefix.getCanonicalPath());
        // BridgeProfile is a strict loopback-only source; never interpolate user shell strings.
        env.putAll(bridge);
        return env;
    }

    /** Keep the ELF verification in one place; do not assume an x86_64 glibc ELF runs. */
    private static final class ElfVerifierBridge {
        static boolean isBionicX64(File wine) {
            return dev.velowin.app.runtime.ElfVerifier.isBionicMachine(wine,62);
        }
    }
}
