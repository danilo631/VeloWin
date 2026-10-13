package dev.velowin.app.engine;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Strict local TCP bridge configuration. The bridge must be started by the user
 * in a separate X11/PulseAudio host; VeloWin does not contain an X11 server.
 * Never accept remote hosts or arbitrary ports from a game profile.
 */
public final class BridgeProfile {
    public final int display;
    public final boolean pulseAudio;
    private BridgeProfile(int display, boolean pulseAudio) {
        this.display=display;
        this.pulseAudio=pulseAudio;
    }

    public static BridgeProfile disabled() { return new BridgeProfile(-1,false); }
    public static BridgeProfile of(int display,boolean pulseAudio) {
        if(display < -1 || display > 9)
            throw new IllegalArgumentException("Display X11 precisa estar entre 0 e 9.");
        return new BridgeProfile(display,pulseAudio);
    }

    public boolean x11Enabled() { return display>=0; }
    public int x11Port() { return x11Enabled()?6000+display:-1; }
    public int pulsePort() { return pulseAudio?4713:-1; }

    public Map<String,String> environment() {
        LinkedHashMap<String,String> result=new LinkedHashMap<>();
        if(x11Enabled())result.put("DISPLAY","127.0.0.1:"+display);
        if(pulseAudio)result.put("PULSE_SERVER","tcp:127.0.0.1:4713");
        return result;
    }

    public String summary() {
        return "X11: "+(x11Enabled()?"127.0.0.1:"+display+" (TCP "+x11Port()+")":"desativado")
            +"\nPulseAudio: "+(pulseAudio?"127.0.0.1:4713":"desativado");
    }
}
