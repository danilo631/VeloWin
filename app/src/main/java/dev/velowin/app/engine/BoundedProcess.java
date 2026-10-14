package dev.velowin.app.engine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * No shell, bounded diagnostics. Dedicated stream pump prevents child pipe
 * blocking waitFor; timeout and cancellation forcibly stop the direct child.
 * This does NOT kill grandchildren: use only for short -v/--version probes.
 */
public final class BoundedProcess {
    public static final class Result {
        public final int code;
        public final boolean timeout;
        public final boolean cancelled;
        public final String output;
        public final long elapsedMs;
        Result(int code, boolean timeout, boolean cancelled, String output, long elapsedMs) {
            this.code=code;this.timeout=timeout;this.cancelled=cancelled;
            this.output=output;this.elapsedMs=elapsedMs;
        }
        public String summary() {
            return (timeout ? "Tempo limite" : cancelled ? "Cancelado" : "Código " + code)
                + " / " + elapsedMs + " ms\n" + output;
        }
    }

    private BoundedProcess() {}

    public static Result run(List<String> command, File directory,
                             Map<String,String> additionalEnvironment,
                             long timeoutMs, AtomicBoolean cancel) throws Exception {
        if (command == null || command.isEmpty() || command.size()>8
                || timeoutMs < 500 || timeoutMs > 30000 || cancel == null)
            throw new IllegalArgumentException("Diagnóstico inválido.");
        for (String arg: command) {
            if (arg == null || arg.isEmpty() || arg.indexOf('\0') >= 0 || arg.length()>4096)
                throw new IllegalArgumentException("Argumento de processo inválido.");
        }
        if (directory == null || !directory.isDirectory())
            throw new IOException("Diretório de diagnóstico inexistente.");
        if (cancel.get()) throw new IOException("Operação cancelada.");
        long started=System.nanoTime();
        ProcessBuilder pb=new ProcessBuilder(new ArrayList<>(command));
        pb.directory(directory);
        pb.redirectErrorStream(true);
        Map<String,String> environment=pb.environment();
        environment.remove("LD_PRELOAD");
        environment.remove("LD_AUDIT");
        for (Map.Entry<String,String> e: additionalEnvironment.entrySet()) {
            if (!e.getKey().matches("[A-Z][A-Z0-9_]{0,63}")
                || e.getValue()==null || e.getValue().length()>4096
                || e.getValue().indexOf('\0')>=0)
                throw new IllegalArgumentException("Variável de ambiente inválida.");
            environment.put(e.getKey(), e.getValue());
        }

        Process p=pb.start();
        // Keep both the beginning (setup errors) and end (Wine's final error or
        // Win32 success marker). Draining never stops at the capture limit.
        final int CAP=8192;
        ByteArrayOutputStream capture=new ByteArrayOutputStream(4096);
        byte[] tail=new byte[CAP];
        int[] tailLength={0};
        long[] totalBytes={0};
        AtomicBoolean streamDone=new AtomicBoolean(false);
        Thread drainer=new Thread(()->{
            try(InputStream in=p.getInputStream()){
                byte[] block=new byte[1024];
                int n;
                while((n=in.read(block))>=0) {
                    synchronized(capture) {
                        totalBytes[0]+=n;
                        if(capture.size()<CAP)
                            capture.write(block,0,Math.min(n,CAP-capture.size()));
                        int keep=Math.min(n,CAP);
                        int remove=Math.max(0,tailLength[0]+keep-CAP);
                        if(remove>0) {
                            System.arraycopy(tail,remove,tail,0,tailLength[0]-remove);
                            tailLength[0]-=remove;
                        }
                        System.arraycopy(block,n-keep,tail,tailLength[0],keep);
                        tailLength[0]+=keep;
                    }
                    // Intentionally drain even when head and tail are full.
                }
            }catch(IOException ignored){}finally{streamDone.set(true);}
        },"velowin-probe-output");
        drainer.setDaemon(true);
        drainer.start();
        boolean timedOut=false, wasCancelled=false;
        try {
            while(p.isAlive()){
                if(cancel.get()||Thread.currentThread().isInterrupted()){
                    wasCancelled=true;break;
                }
                long elapsed=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started);
                if(elapsed>=timeoutMs){timedOut=true;break;}
                p.waitFor(Math.min(100,Math.max(1,timeoutMs-elapsed)),TimeUnit.MILLISECONDS);
            }
            if(timedOut||wasCancelled){
                p.destroyForcibly();
                p.waitFor(2,TimeUnit.SECONDS);
            }
            drainer.join(1000);
            long duration=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started);
            String output;
            synchronized(capture){
                output=capture.toString("UTF-8");
                if(totalBytes[0]>capture.size()) {
                    // If the streams overlap, omit only the already captured bytes.
                    int overlap=(int)Math.max(0,
                        capture.size()+tailLength[0]-totalBytes[0]);
                    if(totalBytes[0]>capture.size()+tailLength[0])
                        output+="\n[...saída intermediária omitida...]\n";
                    output+=new String(tail,overlap,tailLength[0]-overlap,
                        java.nio.charset.StandardCharsets.UTF_8);
                }
            }
            if(!streamDone.get()) output+="\n[saída truncada/incompleta]";
            return new Result((timedOut||wasCancelled||p.isAlive())?-1:p.exitValue(),
                timedOut,wasCancelled,output,duration);
        }finally {
            if(p.isAlive()){
                p.destroyForcibly();
                p.waitFor(1,TimeUnit.SECONDS);
            }
        }
    }
}
