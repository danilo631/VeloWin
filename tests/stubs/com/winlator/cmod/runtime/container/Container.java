package com.winlator.cmod.runtime.container;

import java.util.HashMap;
import java.util.Map;

/** Pure JVM stub to test only VeloWin policy, never shipped in Android. */
public class Container {
    private final Map<String,String> extras=new HashMap<>();
    private String driver="wrapper",dx="dxvk+vkd3d",env="mesa_glthread=true TEST=1";
    private String screen="1600x900",display="x11";
    private boolean gamescope=false,saved=false;
    public String getGraphicsDriver(){return driver;}
    public void setGraphicsDriver(String v){driver=v;}
    public String getDXWrapper(){return dx;}
    public void setDXWrapper(String v){dx=v;}
    public String getEnvVars(){return env;}
    public void setEnvVars(String v){env=v;}
    public String getScreenSize(){return screen;}
    public void setScreenSize(String v){screen=v;}
    public String getExtra(String k,String fallback){return extras.getOrDefault(k,fallback);}
    public void putExtra(String k,Object v){if(v==null)extras.remove(k);else extras.put(k,v.toString());}
    public boolean isGamescopeRuntime(){return gamescope;}
    public void setGamescope(boolean value){gamescope=value;}
    public void setDisplayBackend(String v){display=v;}
    public String display(){return display;}
    public void saveData(){saved=true;}
    public boolean wasSaved(){return saved;}
    public static final String DISPLAY_BACKEND_X11="x11";
}
