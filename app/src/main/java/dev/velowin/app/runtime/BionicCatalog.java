package dev.velowin.app.runtime;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import javax.net.ssl.HttpsURLConnection;

public final class BionicCatalog {
    private BionicCatalog() {}
    public static ArrayList<BionicRelease> search() throws Exception {
        HttpsURLConnection http = RuntimeCatalog.open(BionicReleasePolicy.API);
        try {
            if (http.getResponseCode() != HttpURLConnection.HTTP_OK)
                throw new IOException("Catálogo GameNative HTTP " + http.getResponseCode());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = http.getInputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n=in.read(buf))!=-1) {
                    if (out.size()+n>2*1024*1024) throw new IOException("Catálogo muito grande.");
                    out.write(buf,0,n);
                }
            }
            JSONArray releases = new JSONArray(out.toString("UTF-8"));
            ArrayList<BionicRelease> list = new ArrayList<>();
            int x64=0,arm=0;
            for(int i=0; i<releases.length(); i++) {
                JSONObject release=releases.optJSONObject(i);
                if(release==null||release.optBoolean("draft")||release.optBoolean("prerelease"))continue;
                String tag=release.optString("tag_name");
                JSONArray assets=release.optJSONArray("assets");
                if(assets==null)continue;
                for(int j=0;j<assets.length();j++){
                    JSONObject asset=assets.optJSONObject(j);
                    if(asset==null)continue;
                    String name=asset.optString("name"),uri=asset.optString("browser_download_url");
                    String digest=asset.optString("digest"),arch=name.contains("arm64ec")?"ARM64EC":"x86_64";
                    long size=asset.optLong("size");
                    if(!BionicReleasePolicy.valid(tag,name,uri,digest,size))continue;
                    if("ARM64EC".equals(arch) && arm++>=2)continue;
                    if("x86_64".equals(arch) && x64++>=3)continue;
                    list.add(new BionicRelease(tag,name,uri,
                        BionicReleasePolicy.hash(digest),size,arch));
                }
            }
            if(list.isEmpty())throw new IOException("Nenhum pacote Bionic com SHA-256 válido.");
            return list;
        } finally {http.disconnect();}
    }
}
