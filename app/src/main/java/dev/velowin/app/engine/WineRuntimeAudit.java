package dev.velowin.app.engine;

import dev.velowin.app.runtime.ElfVerifier;
import java.io.File;
import java.io.IOException;

/** Read-only audit of the selected GameNative x86_64/Bionic installation. */
public final class WineRuntimeAudit {
    private WineRuntimeAudit() {}

    private static File confined(File root,String path) throws IOException {
        File base=root.getCanonicalFile();
        File f=new File(base,path).getCanonicalFile();
        if(!f.getPath().startsWith(base.getPath()+File.separator))
            throw new IOException("Symlink escapa do WCP: "+path);
        return f;
    }

    public static boolean loaderReady(File root) {
        try {
            return ElfVerifier.isBionicMachine(confined(root,"bin/wine"),62);
        } catch(IOException failure){ return false; }
    }

    public static String report(File root) {
        StringBuilder b=new StringBuilder("AUDITORIA WINE BIONIC (somente arquivos)\n");
        try {
            File wine=confined(root,"bin/wine");
            File server=confined(root,"bin/wineserver");
            File unix=confined(root,"lib/wine/x86_64-unix");
            File windows=confined(root,"lib/wine/x86_64-windows");
            File prefix=confined(root,"prefixPack.txz");
            boolean wineOk=ElfVerifier.isBionicMachine(wine,62);
            boolean serverOk=ElfVerifier.isBionicMachine(server,62);
            b.append("bin/wine: ").append(wineOk?"ELF x86_64/Bionic":"AUSENTE/ELF ou linker incompatível").append('\n');
            b.append("bin/wineserver: ").append(serverOk?"ELF x86_64/Bionic":"AUSENTE/ELF ou linker incompatível").append('\n');
            b.append("Módulos Unix: ").append(unix.isDirectory()?"presentes":"ausentes").append('\n');
            b.append("Módulos Windows: ").append(windows.isDirectory()?"presentes":"ausentes").append('\n');
            b.append("prefixPack.txz: ").append(prefix.isFile()?"presente":"ausente").append('\n');
            b.append("Wine ELF pronto para tentativa: ").append(wineOk?"SIM":"NÃO").append('\n');
            if(!serverOk)b.append("AVISO: wineserver ausente/incompatível; CMD/wineboot podem falhar.\n");
            if(!unix.isDirectory()||!windows.isDirectory())
                b.append("AVISO: módulos Wine incompletos; verifique a extração WCP.\n");
        } catch(IOException failure) {
            b.append("FALHA DE SEGURANÇA/LAYOUT: ").append(failure.getMessage()).append('\n');
        }
        b.append("Isto não executa Wine, CMD ou jogo e não prova compatibilidade.\n");
        return b.toString();
    }
}
