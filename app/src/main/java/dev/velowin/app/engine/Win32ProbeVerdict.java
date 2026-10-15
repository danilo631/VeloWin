package dev.velowin.app.engine;

/**
 * A structured result is required: stdout text mentioning a success string is
 * never enough if the actual process failed, timed out or was cancelled.
 */
public final class Win32ProbeVerdict {
    public static final String MARKER="VELO_WIN32_SMOKE_OK";
    private Win32ProbeVerdict() {}
    public static boolean passed(int exitCode,boolean timeout,boolean cancelled,String stdout) {
        if(exitCode!=0 || timeout || cancelled || stdout==null)return false;
        for(String line:stdout.split("\\r?\\n",-1))
            if(MARKER.equals(line.trim()))return true;
        return false;
    }
    public static String summary(int exitCode,boolean timeout,boolean cancelled,String stdout) {
        if(passed(exitCode,timeout,cancelled,stdout))
            return "WIN32 CONFIRMADO: o programa de teste retornou código 0 e marcador próprio.";
        if(timeout)return "WIN32 NÃO VALIDADO: processo excedeu o limite de tempo.";
        if(cancelled)return "WIN32 NÃO VALIDADO: teste cancelado.";
        return "WIN32 NÃO VALIDADO: código "+exitCode+" ou marcador ausente.";
    }
}
