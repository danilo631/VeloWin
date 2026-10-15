package dev.velowin.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * VeloWin-owned ImageFS update garbage collection. Unlike the old blanket
 * clearRootDir, preserves Wine user HOME and installed-Wine packages, rejects
 * symlinked protected directories and never follows symlinks when deleting.
 *
 * This is only the filesystem preparation phase. The pinned engine still
 * supplies Zstd/TAR extraction, setup and full X11/Wine launch.
 */
public final class VeloImageFsMaintenance {
    private VeloImageFsMaintenance(){}
    public static void prepare(File root) throws IOException {
        if (root==null)throw new IOException("ImageFS root absent");
        Path base=root.toPath().toAbsolutePath().normalize();
        if (Files.isSymbolicLink(base))throw new IOException("ImageFS root is a symlink");
        Files.createDirectories(base);
        if (!Files.isDirectory(base,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("ImageFS root is not a directory");
        Path home=base.resolve("home"), opt=base.resolve("opt");
        // Verify protected storage before deleting anything at all.
        checkProtected(home);
        checkProtected(opt);
        checkProtected(opt.resolve("installed-wine"));
        try (DirectoryStream<Path> entries=Files.newDirectoryStream(base)) {
            for(Path entry:entries){
                if (entry.getFileName().toString().equals("home"))continue;
                if (entry.getFileName().toString().equals("opt")){
                    if(!Files.isDirectory(entry,LinkOption.NOFOLLOW_LINKS)){
                        if(Files.exists(entry,LinkOption.NOFOLLOW_LINKS))delete(entry);
                        Files.createDirectory(entry);
                    }
                    try(DirectoryStream<Path> children=Files.newDirectoryStream(entry)){
                        for(Path child:children)
                            if(!child.getFileName().toString().equals("installed-wine"))
                                delete(child);
                    }
                    continue;
                }
                delete(entry);
            }
        }
    }
    private static void checkProtected(Path p) throws IOException{
        if (Files.isSymbolicLink(p))throw new IOException("Protected ImageFS path is symlink: "+p);
        if (Files.exists(p,LinkOption.NOFOLLOW_LINKS)
                && !Files.isDirectory(p,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Protected ImageFS path not a directory: "+p);
    }
    static void delete(Path p) throws IOException {
        if(!Files.exists(p,LinkOption.NOFOLLOW_LINKS))return;
        Files.walkFileTree(p,new SimpleFileVisitor<Path>(){
            @Override public FileVisitResult visitFile(Path file,BasicFileAttributes attr)
                    throws IOException {
                Files.delete(file);return FileVisitResult.CONTINUE;
            }
            @Override public FileVisitResult postVisitDirectory(Path dir,IOException error)
                    throws IOException{
                if(error!=null)throw error;
                Files.delete(dir);return FileVisitResult.CONTINUE;
            }
        });
    }
}
