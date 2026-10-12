package dev.velowin.app.runtime;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/** Minimal ELF64 LE x86_64 validator for unexecuted Wine binaries. */
public final class ElfVerifier {
    private ElfVerifier() {}

    public static boolean isX86_64(File file) {
        try { return reason(file) == null; }
        catch (IOException e) { return false; }
    }

    public static String reason(File binary) throws IOException {
        if (binary == null || !binary.isFile()) return "bin/wine ausente.";
        try (RandomAccessFile in = new RandomAccessFile(binary, "r")) {
            if (in.length() < 64) return "Executável ELF truncado.";
            byte[] header = new byte[64];
            in.readFully(header);
            if (header[0] != 0x7f || header[1] != 'E' || header[2] != 'L'
                    || header[3] != 'F' || header[4] != 2 || header[5] != 1)
                return "bin/wine não é ELF64 little-endian.";
            int machine = shortLe(header, 18);
            if (machine != 62) return "Arquitetura ELF " + machine + "; esperado x86_64.";
            int type = shortLe(header, 16);
            if (type != 2 && type != 3) return "Tipo ELF não executável.";
            long phoff = longLe(header, 32);
            int phsize = shortLe(header, 54);
            int phcount = shortLe(header, 56);
            if (phoff == 0 && phcount == 0) return null;
            if (phsize < 56 || phcount > 256 || phoff < 64
                    || phoff > in.length() || (long)phsize * phcount > in.length()-phoff)
                return "Cabeçalho de programas ELF inválido.";

            byte[] ph = new byte[56];
            for (int i = 0; i < phcount; i++) {
                in.seek(phoff + (long)phsize*i);
                in.readFully(ph);
                if (intLe(ph, 0) != 3) continue; // PT_INTERP
                long offset = longLe(ph, 8);
                long size = longLe(ph, 32);
                if (size < 2 || size > 512 || offset < 0 || offset > in.length() - size)
                    return "Caminho PT_INTERP inválido.";
                byte[] path = new byte[(int)size];
                in.seek(offset);
                in.readFully(path);
                int n = 0;
                while (n < path.length && path[n] != 0) n++;
                String linker = new String(path, 0, n, StandardCharsets.UTF_8);
                if (linker.contains("linker64") || linker.startsWith("/system/")
                        || linker.startsWith("/apex/"))
                    return "Wine Bionic detectado; não instalar no pipeline glibc.";
                if (!linker.startsWith("/lib") && !linker.startsWith("/usr/lib"))
                    return "Interpretador ELF inesperado: " + linker;
                return null;
            }
            return null;
        }
    }

    private static int shortLe(byte[] bytes, int offset) {
        return (bytes[offset] & 255) | ((bytes[offset+1] & 255) << 8);
    }
    private static long intLe(byte[] bytes, int offset) {
        return ((long)shortLe(bytes, offset) & 65535)
            | (((long)shortLe(bytes, offset+2) & 65535) << 16);
    }
    private static long longLe(byte[] bytes, int offset) {
        return intLe(bytes, offset) | (intLe(bytes, offset+4) << 32);
    }
}
