package com.winlator.cmod.velowin;

/**
 * Independent VeloWin boot policy, based on Winlator's explorer + file manager
 * desktop behavior. Does not fork XServer or spawn a second Wine instance.
 */
public final class VeloBootTarget {
    private VeloBootTarget() {}
    public static final String MODE_KEY="velowin.boot.filemanager";
    public static final String WFM="\"wfm.exe\"";
    public static final String WINEFILE="\"winefile.exe\"";
    // Wine's built-in explorer directly when even winhandler.exe is absent.
    public static final String EXPLORER="explorer.exe";

    public static String select(boolean wfmReady, boolean handlerReady,
                                boolean forceWineFile) {
        if(!handlerReady)return EXPLORER;
        if(forceWineFile || !wfmReady)return WINEFILE;
        return WFM;
    }
    public static boolean isRecovery(String value) {
        return "1".equals(value);
    }
}
