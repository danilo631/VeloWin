package com.winlator.cmod.velowin;

import com.winlator.cmod.runtime.container.Container;

/**
 * VeloWin's independent, reversible Mali-compatible container profile.
 *
 * Derived from community findings in ar37-rs/winlator_mali and Mobox's
 * fallback philosophy; no Turnip/Adreno driver is ever injected here.
 * This is a compatibility baseline, NOT a Vulkan capability assertion.
 * GPL-3.0-or-later, copyright VeloWin contributors.
 */
public final class MaliProfile {
    private static final String APPLIED="velowin.mali.applied";
    private static final String OLD_DRIVER="velowin.mali.driver";
    private static final String OLD_DX="velowin.mali.dx";
    private static final String OLD_ENV="velowin.mali.env";
    private static final String OLD_SCREEN="velowin.mali.screen";
    private static final String OLD_FRAMEGEN="velowin.mali.fg";
    private static final String OLD_LSFG="velowin.mali.lsfg";

    private MaliProfile() {}

    public static boolean isApplied(Container c) {
        return c!=null && "1".equals(c.getExtra(APPLIED,"0"));
    }
    public static void apply(Container c) {
        if(c==null)throw new IllegalArgumentException("Contêiner ausente");
        if(c.isGamescopeRuntime())throw new IllegalStateException(
            "Mali compatibilidade só pode ser aplicada ao Wine/X11.");
        if(isApplied(c))return;
        c.putExtra(OLD_DRIVER,c.getGraphicsDriver());
        c.putExtra(OLD_DX,c.getDXWrapper());
        c.putExtra(OLD_ENV,c.getEnvVars());
        c.putExtra(OLD_SCREEN,c.getScreenSize());
        c.putExtra(OLD_FRAMEGEN,c.getExtra("disFrameGen","0"));
        c.putExtra(OLD_LSFG,c.getExtra("frameGen","0"));

        // System Vulkan wrapper or WineD3D avoids incorrectly selecting
        // Turnip on Arm Mali. User can opt in to DXVK in advanced settings.
        c.setGraphicsDriver("wrapper");
        c.setDXWrapper("wined3d");
        c.setScreenSize("1280x720");
        c.setEnvVars(rewriteEnv(c.getEnvVars()));
        c.putExtra("disFrameGen","0"); // No speculative compute support.
        c.putExtra("frameGen","0"); // Old LSFG never allowed.
        c.setDisplayBackend(Container.DISPLAY_BACKEND_X11);
        c.putExtra(APPLIED,"1");
        c.saveData();
    }
    public static void restore(Container c) {
        if(!isApplied(c))return;
        c.setGraphicsDriver(c.getExtra(OLD_DRIVER,"wrapper"));
        c.setDXWrapper(c.getExtra(OLD_DX,"wined3d"));
        c.setEnvVars(c.getExtra(OLD_ENV,""));
        c.setScreenSize(c.getExtra(OLD_SCREEN,"1280x720"));
        c.putExtra("disFrameGen",c.getExtra(OLD_FRAMEGEN,"0"));
        // The old proprietary mode is retired; always remain disabled.
        c.putExtra("frameGen","0");
        for(String k:new String[]{OLD_DRIVER,OLD_DX,OLD_ENV,OLD_SCREEN,
            OLD_FRAMEGEN,OLD_LSFG,APPLIED})c.putExtra(k,null);
        c.saveData();
    }
    public static String rewriteEnv(String original) {
        String value=original==null?"":original.trim();
        // Never set guessed GL_VERSION_OVERRIDE / Vulkan version.
        // Retain every non-overridden env variable, preserve order.
        value=value.replaceAll("(?i)(?:^|\\s)mesa_glthread=(?:true|false|0|1)(?=\\s|$)"," ");
        value=value.trim();
        return value.isEmpty()?"mesa_glthread=false":value+" mesa_glthread=false";
    }
}
