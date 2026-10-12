package dev.velowin.app.library;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Small, bounded PE header reader. Identifies architecture before a game is added
 * without copying the whole executable or loading/starting untrusted code.
 */
public final class PeInspector {
    private PeInspector() {}
    public static final int LIMIT = 1024 * 1024;

    public static String inspect(InputStream input) throws IOException {
        if (input == null) throw new IOException("Não foi possível ler o executável.");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        // Reading just the first MB bounds memory for arbitrary selected files.
        while (output.size() < LIMIT
                && (read = input.read(buffer, 0, Math.min(buffer.length, LIMIT-output.size()))) != -1) {
            output.write(buffer, 0, read);
            if (output.size() >= 64) {
                byte[] b = output.toByteArray();
                long location = uint32(b, 0x3c);
                if (location >= 64 && location <= LIMIT-24 && output.size() >= location+24)
                    return decode(b, (int)location);
                if (location > LIMIT-24) throw new IOException("Cabeçalho PE fora dos limites.");
            }
        }
        return parse(output.toByteArray());
    }

    public static String parse(byte[] data) throws IOException {
        if (data == null || data.length < 64
                || data[0] != 'M' || data[1] != 'Z') throw new IOException("Cabeçalho MZ ausente.");
        long offset = uint32(data, 0x3c);
        if (offset < 64 || offset > LIMIT - 24 || offset+24 > data.length)
            throw new IOException("Cabeçalho PE incompleto.");
        return decode(data, (int)offset);
    }

    private static String decode(byte[] data, int offset) throws IOException {
        if (data.length < 64 || data[0] != 'M' || data[1] != 'Z'
                || data[offset] != 'P' || data[offset+1] != 'E'
                || data[offset+2] != 0 || data[offset+3] != 0)
            throw new IOException("Não é um executável PE Windows válido.");
        int machine = (data[offset+4] & 255) | ((data[offset+5] & 255) << 8);
        if (machine == 0x8664) return "x86_64";
        if (machine == 0x014c) return "x86";
        if (machine == 0xaa64) return "arm64";
        throw new IOException("Arquitetura PE não suportada: " + Integer.toHexString(machine));
    }

    private static long uint32(byte[] data, int offset) {
        return ((long)data[offset] & 255L)
            | (((long)data[offset+1] & 255L) << 8)
            | (((long)data[offset+2] & 255L) << 16)
            | (((long)data[offset+3] & 255L) << 24);
    }
}
