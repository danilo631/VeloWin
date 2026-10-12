import dev.velowin.app.runtime.ElfVerifier;
import java.io.File;
import java.io.FileOutputStream;

public class ElfVerifierTest {
    static void ok(boolean b, String name) { if(!b) throw new AssertionError(name); }
    static byte[] elf(int machine) {
        byte[] e = new byte[80];
        e[0]=0x7f;e[1]='E';e[2]='L';e[3]='F';e[4]=2;e[5]=1;
        e[16]=3;
        e[18]=(byte)machine;e[19]=(byte)(machine >>> 8);
        return e;
    }
    static byte[] dynamic(int machine, String interpreter) throws Exception {
        byte[] image = new byte[256];
        byte[] base = elf(machine);
        System.arraycopy(base, 0, image, 0, base.length);
        image[32] = 64; // ELF64 phoff
        image[54] = 56; // phentsize
        image[56] = 1;  // phnum
        image[64] = 3;  // PT_INTERP
        image[64+8] = (byte) 160; // offset 160
        byte[] name = (interpreter + "\0").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        image[64+32] = (byte) name.length;
        System.arraycopy(name,0,image,160,name.length);
        return image;
    }

    public static void main(String[] args) throws Exception {
        File tmp = File.createTempFile("velowin-elf-", ".bin");
        try {
            try(FileOutputStream out=new FileOutputStream(tmp)){out.write(elf(62));}
            ok(ElfVerifier.isX86_64(tmp), "ELF64 x86_64");
            try(FileOutputStream out=new FileOutputStream(tmp)){out.write(elf(183));}
            ok(!ElfVerifier.isX86_64(tmp), "ARM64 rejected");
            try(FileOutputStream out=new FileOutputStream(tmp)){
                out.write(dynamic(62, "/system/bin/linker64"));
            }
            ok(ElfVerifier.isBionicMachine(tmp,62), "Bionic x86-64 interpreter");
            ok(!ElfVerifier.isBionicMachine(tmp,183), "wrong Bionic machine");
            try(FileOutputStream out=new FileOutputStream(tmp)){
                out.write(dynamic(62, "/lib64/ld-linux-x86-64.so.2"));
            }
            ok(!ElfVerifier.isBionicMachine(tmp,62), "glibc interpreter blocked");
            try(FileOutputStream out=new FileOutputStream(tmp)){out.write(new byte[4]);}
            ok(!ElfVerifier.isX86_64(tmp), "truncated ELF");
        }finally{ tmp.delete(); }
        System.out.println("ElfVerifierTest: PASS");
    }
}
