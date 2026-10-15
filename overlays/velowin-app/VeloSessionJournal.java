package com.winlator.cmod.velowin;

import android.os.SystemClock;
import java.util.concurrent.ConcurrentHashMap;
import dev.velowin.core.VeloSessionLifecycle;

/** Process-local observed session state. No persisted false RUNNING after process death. */
public final class VeloSessionJournal {
    private static final ConcurrentHashMap<Integer,VeloSessionLifecycle> ACTIVE=
        new ConcurrentHashMap<>();
    private VeloSessionJournal(){}
    public static void begin(int id){
        if(id<=0)return;
        VeloSessionLifecycle s=new VeloSessionLifecycle();
        s.move(VeloSessionLifecycle.State.PREPARING,
            SystemClock.elapsedRealtime(),"Solicitação de início");
        ACTIVE.put(id,s);
    }
    private static void mark(int id,VeloSessionLifecycle.State state,String details){
        if(id<=0)return;
        VeloSessionLifecycle session=ACTIVE.computeIfAbsent(id,key->{
            VeloSessionLifecycle s=new VeloSessionLifecycle();
            s.move(VeloSessionLifecycle.State.PREPARING,
                SystemClock.elapsedRealtime(),"Início observado no motor");
            return s;
        });
        session.move(state,SystemClock.elapsedRealtime(),details);
    }
    public static void markXServer(int id){
        mark(id,VeloSessionLifecycle.State.STARTING_XSERVER,"Preparando XServer");
    }
    public static void markWine(int id){
        mark(id,VeloSessionLifecycle.State.STARTING_WINE,"Preparação Wine concluída");
    }
    public static void markWaiting(int id){
        mark(id,VeloSessionLifecycle.State.WAITING_FOR_WINDOW,"Esperando janela guest");
    }
    public static void markWindow(int id){
        mark(id,VeloSessionLifecycle.State.RUNNING,"Primeira janela guest observada");
    }
    public static void warnNoWindow(int id){
        mark(id,VeloSessionLifecycle.State.WAITING_FOR_WINDOW,"35s sem janela guest");
    }
    public static void markStopped(int id){
        mark(id,VeloSessionLifecycle.State.STOPPED,"Activity encerrada");
    }
    public static void markFailed(int id,String why){
        mark(id,VeloSessionLifecycle.State.FAILED,why);
    }
    public static String describe(int id){
        VeloSessionLifecycle state=ACTIVE.get(id);
        return state==null?"sem sessão observada neste processo":
            state.state().name()+" • "+state.detail();
    }
}
