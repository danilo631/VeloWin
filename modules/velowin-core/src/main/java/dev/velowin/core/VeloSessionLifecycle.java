package dev.velowin.core;

/** Events observed during an actual XServer/Wine session, not estimated health. */
public final class VeloSessionLifecycle {
    public enum State { CREATED, PREPARING, STARTING_XSERVER, STARTING_WINE,
        WAITING_FOR_WINDOW, RUNNING, FAILED, STOPPING, STOPPED }
    private State state=State.CREATED;
    private long changedAt;
    private String detail="";
    public synchronized State state(){return state;}
    public synchronized String detail(){return detail;}
    public synchronized long changedAt(){return changedAt;}

    public synchronized boolean move(State next,long now,String reason) {
        if(next==null || now<0 || now<changedAt)
            throw new IllegalArgumentException("bad lifecycle event");
        if(state==State.FAILED || state==State.STOPPED)return false;
        if(next==state){
            changedAt=now;detail=reason==null?"":reason;
            return true;
        }
        boolean startup=state==State.PREPARING || state==State.STARTING_XSERVER ||
            state==State.STARTING_WINE || state==State.WAITING_FOR_WINDOW;
        boolean accepted;
        switch(next){
            case PREPARING: accepted=state==State.CREATED;break;
            case STARTING_XSERVER:
            case STARTING_WINE:
            case WAITING_FOR_WINDOW: accepted=startup;break;
            case RUNNING: accepted=startup;break;
            case FAILED:
            case STOPPING:
            case STOPPED: accepted=state!=State.CREATED;break;
            default: accepted=false;
        }
        if(!accepted)return false;
        state=next; changedAt=now;detail=reason==null?"":reason;
        return true;
    }
}
