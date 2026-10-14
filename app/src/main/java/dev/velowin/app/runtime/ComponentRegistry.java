package dev.velowin.app.runtime;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Record verified archives only. The checksum/layout are revalidated before use. */
public final class ComponentRegistry {
    private ComponentRegistry(){}
    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("velowin-components-v1",Context.MODE_PRIVATE);
    }
    public static void put(Context ctx,ComponentRelease e)throws Exception {
        if(!ComponentReleasePolicy.valid(e)||!ComponentStore.installed(ctx,e))
            throw new java.io.IOException("Componente não verificado.");
        JSONObject data=new JSONObject();
        data.put("kind",e.kind.name());data.put("tag",e.tag);
        data.put("name",e.name);data.put("url",e.url);
        data.put("sha",e.sha256);data.put("size",e.size);
        if(!prefs(ctx).edit().putString(e.kind.name(),data.toString()).commit())
            throw new java.io.IOException("Não foi possível registrar componente.");
    }
    public static ComponentRelease get(Context ctx,ComponentRelease.Kind kind){
        try{
            JSONObject data=new JSONObject(prefs(ctx).getString(kind.name(),""));
            ComponentRelease e=new ComponentRelease(kind,data.getString("tag"),
                data.getString("name"),data.getString("url"),data.getString("sha"),
                data.getLong("size"));
            return ComponentReleasePolicy.valid(e)&&ComponentStore.installed(ctx,e)?e:null;
        }catch(Exception ignored){return null;}
    }
    public static void forget(Context ctx,ComponentRelease.Kind kind){
        prefs(ctx).edit().remove(kind.name()).apply();
    }
}
