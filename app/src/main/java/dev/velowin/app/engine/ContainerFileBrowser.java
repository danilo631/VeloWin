package dev.velowin.app.engine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Safe, shell-free navigator for one app-private directory.
 *
 * No external storage access, no JavaScript, no executing files. Symbolic links
 * are excluded deliberately, including Wine's Z: host-root mapping. All
 * operations are bounded to the root set by the Android activity, never by an
 * intent or an untrusted game profile.
 */
public final class ContainerFileBrowser {
    public static final int MAX_ENTRIES=300;
    public static final int MAX_PREVIEW=16*1024;
    private static final int MAX_READ_FILE=512*1024;
    private final File root;
    private final boolean writable;

    public static final class Entry {
        public final String name;
        public final String relative;
        public final boolean directory;
        public final long length;
        public final long modified;
        Entry(String name,String relative,boolean directory,long length,long modified){
            this.name=name;this.relative=relative;this.directory=directory;
            this.length=length;this.modified=modified;
        }
    }

    public ContainerFileBrowser(File root,boolean writable) throws IOException {
        if(root==null || !root.isDirectory() || Files.isSymbolicLink(root.toPath()))
            throw new IOException("Raiz privada do contêiner indisponível.");
        this.root=root.getCanonicalFile();
        this.writable=writable;
    }

    private static boolean safeName(String name) {
        return name!=null && !name.isEmpty() && !name.equals(".") && !name.equals("..")
            && name.length()<=255 && name.indexOf('/')<0 && name.indexOf('\\')<0
            && name.indexOf('\0')<0;
    }

    /** Empty string means the configured root. Absolute paths and dot segments are denied. */
    public File resolve(String relative) throws IOException {
        if(relative==null || relative.length()>2048 || relative.startsWith("/")
            || relative.indexOf('\\')>=0 || relative.indexOf('\0')>=0)
            throw new IOException("Caminho inválido.");
        File current=root;
        if(!relative.isEmpty()) {
            for(String name:relative.split("/",-1)) {
                if(!safeName(name))throw new IOException("Segmento de caminho inválido.");
                current=new File(current,name);
                if(Files.isSymbolicLink(current.toPath()))
                    throw new IOException("Link simbólico não navegável.");
            }
        }
        File canonical=current.getCanonicalFile();
        if(!canonical.equals(root) && !canonical.getPath().startsWith(
                root.getPath()+File.separator))
            throw new IOException("Caminho fora do contêiner.");
        return canonical;
    }

    public String parent(String relative) throws IOException {
        resolve(relative);
        if(relative.isEmpty())return "";
        int slash=relative.lastIndexOf('/');
        return slash<0?"":relative.substring(0,slash);
    }

    public List<Entry> list(String relative) throws IOException {
        File folder=resolve(relative);
        if(!folder.isDirectory())throw new IOException("Pasta não encontrada.");
        File[] files=folder.listFiles();
        if(files==null)throw new IOException("Não foi possível listar a pasta.");
        Arrays.sort(files,Comparator.comparing((File f)->!f.isDirectory())
            .thenComparing(File::getName,String.CASE_INSENSITIVE_ORDER));
        ArrayList<Entry> entries=new ArrayList<>();
        for(File file:files) {
            if(entries.size()==MAX_ENTRIES)break;
            String name=file.getName();
            if(!safeName(name) || Files.isSymbolicLink(file.toPath()))continue;
            String next=relative.isEmpty()?name:relative+"/"+name;
            File safe;
            try{safe=resolve(next);}catch(IOException ignored){continue;}
            if(!Files.isRegularFile(safe.toPath(),LinkOption.NOFOLLOW_LINKS)
                && !Files.isDirectory(safe.toPath(),LinkOption.NOFOLLOW_LINKS))continue;
            entries.add(new Entry(name,next,safe.isDirectory(),
                safe.isFile()?safe.length():0L,safe.lastModified()));
        }
        return entries;
    }

    public String inspect(String relative) throws IOException {
        File file=resolve(relative);
        if(!file.isFile() || !Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Arquivo regular não encontrado.");
        return "Arquivo: "+file.getName()+"\nTamanho: "+file.length()
            +" bytes\nÚltima alteração (epoch ms): "+file.lastModified()
            +"\nSomente inspeção. Nenhum executável será iniciado.";
    }

    public String preview(String relative) throws IOException {
        File file=resolve(relative);
        if(!file.isFile() || !Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS)
            || file.length()>MAX_READ_FILE)
            throw new IOException("Prévia indisponível (não é texto pequeno).");
        String name=file.getName().toLowerCase(Locale.ROOT);
        if(!(name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".log")
            || name.endsWith(".json") || name.endsWith(".reg")
            || name.endsWith(".ini") || name.endsWith(".cfg")
            || name.endsWith(".conf")))
            throw new IOException("Extensão não permitida para prévia de texto.");
        byte[] all=Files.readAllBytes(file.toPath());
        int n=Math.min(all.length,MAX_PREVIEW);
        for(int i=0;i<n;i++)if(all[i]==0)throw new IOException("Arquivo binário.");
        return new String(all,0,n,StandardCharsets.UTF_8)
            +(all.length>n?"\n\n[prévia limitada a 16 KiB]":"");
    }

    public void makeFolder(String relative,String name) throws IOException {
        if(!writable)throw new IOException("Área somente leitura.");
        if(!safeName(name) || !name.matches("[A-Za-z0-9À-ÿ_ .-]{1,64}")
            || name.equals(".") || name.equals(".."))
            throw new IOException("Nome de pasta inválido.");
        File parent=resolve(relative);
        if(!parent.isDirectory())throw new IOException("Pasta de destino inexistente.");
        File dest=resolve(relative.isEmpty()?name:relative+"/"+name);
        if(!dest.mkdir())throw new IOException("Não foi possível criar pasta (já existe?).");
    }
}
