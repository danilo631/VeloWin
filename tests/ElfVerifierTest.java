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
    public static void main(String[] args) throws Exception {
        File tmp = File.createTempFile("velowin-elf-", ".bin");
        try {
            try(FileOutputStream out=new FileOutputStream(tmp)){out.write(elf(62));}
            ok(ElfVerifier.isX86_64(tmp), "ELF64 x86_64");
            try(FileOutputStream out=new FileOutputStream(tmp)){out.write(elf(183));}
            ok(!ElfVerifier.isX86_64(tmp), "ARM64 rejected");
            try(FileOutputStream out=new FileOutputStream(tmp)){out.write(new byte[4]);}
            ok(!ElfVerifier.isX86_64(tmp), "truncated ELF");
        }finally{ tmp.delete(); }
        System.out.println("ElfVerifierTest: PASS");
    }
}
