package dev.velowin.app.runtime;

import java.util.Locale;

/** Strict URL/filename/digest policy, independent of Android for CI tests. */
public final class ComponentReleasePolicy {
    private ComponentReleasePolicy(){}
    public static String canonical(ComponentRelease.Kind kind,String tag,String name){
        if(kind==null||tag==null||name==null)return null;
        if(kind==ComponentRelease.Kind.DXVK) {
            if(!tag.matches("v[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}"))
                return null;
            String version=tag.substring(1);
            if(!name.equals("dxvk-"+version+".tar.gz"))return null;
            return "https://github.com/doitsujin/dxvk/releases/download/"+tag+"/"+name;
        }
        if(kind==ComponentRelease.Kind.VKD3D_PROTON) {
            if(!tag.matches("v[0-9]{1,3}\\.[0-9]{1,3}(\\.[0-9]{1,3})?"))
                return null;
            String version=tag.substring(1);
            if(!name.equals("vkd3d-proton-"+version+".tar.zst"))return null;
            return "https://github.com/HansKristian-Work/vkd3d-proton/releases/download/"
                +tag+"/"+name;
        }
        if(kind==ComponentRelease.Kind.TERMUX_X11_APK) {
            if(!"nightly".equals(tag)||!"termux-x11-universal-debug.apk".equals(name))
                return null;
            return "https://github.com/termux/termux-x11/releases/download/nightly/"+name;
        }
        return null;
    }
    public static boolean valid(ComponentRelease e) {
        if(e==null)return false;
        String expected=canonical(e.kind,e.tag,e.name);
        return expected!=null && expected.equals(e.url)
            && e.sha256!=null && e.sha256.matches("[a-f0-9]{64}")
            && e.size>1024 && e.size<=128L*1024*1024;
    }
    public static String digest(String supplied){
        return supplied!=null&&supplied.matches("sha256:[0-9a-fA-F]{64}")
            ?supplied.substring(7).toLowerCase(Locale.ROOT):null;
    }
}
