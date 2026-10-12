package dev.velowin.app.engine;

/**
 * Typed, backwards-compatible reference stored in a per-game profile.
 * Never infer architecture from a loose release tag.
 */
public final class GameRuntimeChoice {
    public enum Kind { AUTO, BIONIC, GLIBC, INVALID }
    public final Kind kind;
    public final String tag;
    public final String arch;
    private GameRuntimeChoice(Kind kind,String tag,String arch) {
        this.kind=kind; this.tag=tag; this.arch=arch;
    }
    public static String bionic(String tag,String arch) {
        if(!tag.matches("proton-[0-9]+\\.[0-9]+-[0-9]+-20[0-9]{6}")
            || (!"x86_64".equals(arch) && !"ARM64EC".equals(arch)))
            throw new IllegalArgumentException("Versão/ABI Wine Bionic inválida.");
        return "BIONIC:"+tag+":"+arch;
    }
    public static GameRuntimeChoice fromStored(String stored) {
        if(stored==null || stored.isEmpty())
            return new GameRuntimeChoice(Kind.AUTO,"","");
        if(stored.startsWith("BIONIC:")) {
            String[] fields=stored.split(":",-1);
            if(fields.length!=3)return invalid();
            try{
                bionic(fields[1],fields[2]);
                return new GameRuntimeChoice(Kind.BIONIC,fields[1],fields[2]);
            }catch(IllegalArgumentException error){return invalid();}
        }
        if(stored.matches("proton-[0-9][A-Za-z0-9.-]*|wine-[0-9][A-Za-z0-9.-]*"))
            return new GameRuntimeChoice(Kind.GLIBC,stored,"");
        return invalid();
    }
    private static GameRuntimeChoice invalid(){
        return new GameRuntimeChoice(Kind.INVALID,"","");
    }
    public String label() {
        switch(kind) {
            case AUTO:return "Automático";
            case BIONIC:return "Bionic "+tag+" / "+arch;
            case GLIBC:return "Wine Linux/glibc "+tag;
            default:return "Perfil de runtime incompatível";
        }
    }
}
