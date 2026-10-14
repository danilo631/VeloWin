import com.winlator.cmod.velowin.MaliProfile;
import com.winlator.cmod.runtime.container.Container;

public final class MaliProfileTest {
    private static void check(boolean yes,String why) {
        if(!yes)throw new AssertionError(why);
    }
    public static void main(String[] args) {
        Container c=new Container();
        c.putExtra("disFrameGen","1");
        MaliProfile.apply(c);
        check(MaliProfile.isApplied(c),"applied flag");
        check("wined3d".equals(c.getDXWrapper()),"WineD3D fallback");
        check("wrapper".equals(c.getGraphicsDriver()),"system wrapper");
        check("1280x720".equals(c.getScreenSize()),"conservative resolution");
        check("x11".equals(c.display()),"X11");
        check("0".equals(c.getExtra("disFrameGen","")),"DIS off until validated");
        check("0".equals(c.getExtra("frameGen","")),"proprietary framegen disabled");
        check("TEST=1 mesa_glthread=false".equals(c.getEnvVars()),"env preserves other settings");
        MaliProfile.apply(c);
        check(!c.getEnvVars().contains("false false"),"apply idempotent");
        MaliProfile.restore(c);
        check(!MaliProfile.isApplied(c),"restored");
        check("dxvk+vkd3d".equals(c.getDXWrapper()),"DX wrapper restored");
        check("1600x900".equals(c.getScreenSize()),"resolution restored");
        check("mesa_glthread=true TEST=1".equals(c.getEnvVars()),"env restored");
        check("1".equals(c.getExtra("disFrameGen","")),"DIS restored");
        check("0".equals(c.getExtra("frameGen","")),"deprecated LSFG cannot reactivate");
        check(MaliProfile.rewriteEnv("A=1 mesa_glthread=0 B=2")
            .equals("A=1 B=2 mesa_glthread=false"),"repeat and spacing");
        Container linux=new Container();
        linux.setGamescope(true);
        boolean guarded=false;
        try{MaliProfile.apply(linux);}
        catch(IllegalStateException expected){guarded=true;}
        check(guarded,"no invalid X11 conversion from GameScope");
        check(!MaliProfile.isApplied(linux),"no partial mutation");
        System.out.println("MaliProfileTest PASS");
    }
}
