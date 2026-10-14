package dev.velowin.app.runtime;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;

/** Official GitHub release metadata; no unverified links from arbitrary pages. */
public final class ComponentCatalog {
    private ComponentCatalog(){}
    public static ComponentRelease latest(ComponentRelease.Kind kind)throws Exception {
        String url=kind==ComponentRelease.Kind.DXVK
            ?"https://api.github.com/repos/doitsujin/dxvk/releases/latest"
            :kind==ComponentRelease.Kind.VKD3D_PROTON
            ?"https://api.github.com/repos/HansKristian-Work/vkd3d-proton/releases/latest"
            :"https://api.github.com/repos/termux/termux-x11/releases/tags/nightly";
        HttpsURLConnection conn=RuntimeCatalog.open(url);
        try{
            if(conn.getResponseCode()!=HttpURLConnection.HTTP_OK)
                throw new IOException("Catálogo GitHub HTTP "+conn.getResponseCode());
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=conn.getInputStream()){
                byte[] block=new byte[8192];
                int n;
                while((n=in.read(block))!=-1){
                    if(out.size()+n>1024*1024)
                        throw new IOException("Manifesto GitHub excessivo.");
                    out.write(block,0,n);
                }
            }
            JSONObject release=new JSONObject(out.toString("UTF-8"));
            if(release.optBoolean("draft")|| (kind!=ComponentRelease.Kind.TERMUX_X11_APK
                && release.optBoolean("prerelease")))
                throw new IOException("Release não é estável.");
            String tag=release.optString("tag_name");
            JSONArray assets=release.optJSONArray("assets");
            if(assets==null)throw new IOException("Release sem assets.");
            for(int i=0;i<assets.length();i++){
                JSONObject a=assets.optJSONObject(i);
                if(a==null)continue;
                String name=a.optString("name"),fileUrl=a.optString("browser_download_url");
                String sha=ComponentReleasePolicy.digest(a.optString("digest"));
                ComponentRelease entry=new ComponentRelease(kind,tag,name,fileUrl,
                    sha,a.optLong("size"));
                if(ComponentReleasePolicy.valid(entry))return entry;
            }
            throw new IOException("Release sem pacote oficial com SHA-256 válido.");
        }finally{conn.disconnect();}
    }
}
