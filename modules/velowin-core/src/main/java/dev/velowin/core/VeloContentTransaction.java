package dev.velowin.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;

/**
 * An independent replacement for ContentsManager's buggy mkdirs+renameTo
 * finalization (previously called both failure and success callbacks).
 * Stage and final target MUST reside on same filesystem for atomic rename.
 * Never delete a previous install or overwrite an occupied destination.
 */
public final class VeloContentTransaction {
    private VeloContentTransaction(){}
    public static void commit(File staging,File installed) throws IOException {
        if(staging==null||installed==null)throw new IOException("No install destination");
        Path source=staging.toPath().toAbsolutePath().normalize();
        Path target=installed.toPath().toAbsolutePath().normalize();
        if(source.equals(target))throw new IOException("Source equals destination");
        if(Files.isSymbolicLink(source)||!Files.isDirectory(source,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Staging missing or symbolic");
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Target already exists");
        Path parent=target.getParent();
        if(parent==null||Files.isSymbolicLink(parent))
            throw new IOException("Unsafe install parent");
        Files.createDirectories(parent);
        if(!Files.isDirectory(parent,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Install parent not a directory");
        if(Files.exists(target,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Concurrent destination appeared");
        try{
            Files.move(source,target,StandardCopyOption.ATOMIC_MOVE);
        }catch(AtomicMoveNotSupportedException error){
            // Work directories are in context private storage; same mount.
            Files.move(source,target);
        }
        if(!Files.isDirectory(target,LinkOption.NOFOLLOW_LINKS)
                || Files.exists(source,LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Finalized installation is inconsistent");
    }
}
