package com.winlator.cmod.velowin;

/**
 * App-owned decision layer: runtime selection is made on verifiable
 * capabilities, not guessed by platform CPU/GPU labels.
 *
 * Wine/Bionic x86_64 uses Box64; Wine/ARM64EC uses native ARM64 wine and
 * FEXCore for x86; glibc/PRoot is a DIFFERENT Linux Gamescope runtime
 * requiring an installed rootfs, never an implicit conversion of a Wineprefix.
 */
public final class VeloRuntimePlan {
    private VeloRuntimePlan(){}
    public enum Mode {
        BIONIC_BOX64("Wine Bionic + Box64"),
        BIONIC_ARM64EC_FEX("Wine ARM64EC + FEXCore"),
        GLIBC_PROOT_GAMESCOPE("Linux glibc / PRoot + Gamescope"),
        EXTERNAL_GLIBC("glibc externo (indisponível)");
        public final String label;
        Mode(String label){this.label=label;}
    }
    public static final class Capabilities {
        public final boolean imageFs, box64, fex, linuxRoot, proot, mali;
        public Capabilities(boolean imageFs,boolean box64,boolean fex,
                            boolean linuxRoot,boolean proot,boolean mali){
            this.imageFs=imageFs;this.box64=box64;this.fex=fex;
            this.linuxRoot=linuxRoot;this.proot=proot;this.mali=mali;
        }
    }
    public static final class Decision {
        public final Mode mode;
        public final boolean available;
        public final String reason;
        Decision(Mode mode,boolean available,String reason){
            this.mode=mode;this.available=available;this.reason=reason;
        }
    }
    public static Decision inspect(Mode mode,String wineArch,boolean linuxContainer,
                                   Capabilities caps){
        if(mode==null||caps==null)return new Decision(mode,false,"Capacidades ausentes");
        boolean arm64ec="arm64ec".equalsIgnoreCase(wineArch);
        switch(mode){
            case BIONIC_BOX64:
                return new Decision(mode,caps.imageFs&&caps.box64&&!arm64ec&&!linuxContainer,
                    !caps.imageFs?"ImageFS não instalado":
                    !caps.box64?"Box64 não encontrado":
                    arm64ec?"Prefixo ARM64EC exige FEX, não Box64":
                    linuxContainer?"Contêiner Linux requer PRoot":"Disponível");
            case BIONIC_ARM64EC_FEX:
                return new Decision(mode,caps.imageFs&&caps.fex&&arm64ec&&!linuxContainer,
                    !caps.imageFs?"ImageFS não instalado":
                    !caps.fex?"FEXCore não instalado":
                    !arm64ec?"Instale um prefixo Wine ARM64EC separado":
                    linuxContainer?"Contêiner Linux não usa FEX Wine direto":"Disponível");
            case GLIBC_PROOT_GAMESCOPE:
                return new Decision(mode,linuxContainer&&caps.linuxRoot&&caps.proot&&!caps.mali,
                    !linuxContainer?"Apenas contêineres Linux já instalados":
                    !caps.linuxRoot?"LinuxFS/Gamescope não instalado":
                    !caps.proot?"PRoot ou loader ausente":
                    caps.mali?"O LinuxFS atual requer driver Linux compatível; "
                        +"não força Turnip em Mali":"Disponível");
            case EXTERNAL_GLIBC:
                return new Decision(mode,false,
                    "Rootfs glibc alternativo não integrado ao launcher; "
                    +"não será marcado funcional sem driver X11/Wine confirmado.");
            default:
                return new Decision(mode,false,"Backend desconhecido");
        }
    }
}
