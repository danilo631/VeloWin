import dev.velowin.core.VeloSessionLifecycle;

public final class VeloSessionLifecycleTest {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    public static void main(String[] a){
        VeloSessionLifecycle s=new VeloSessionLifecycle();
        check(!s.move(VeloSessionLifecycle.State.RUNNING,1,"no window"),"cannot fake running");
        check(s.move(VeloSessionLifecycle.State.PREPARING,2,"launch"),"launch");
        check(s.move(VeloSessionLifecycle.State.STARTING_XSERVER,3,"X"),"XServer");
        check(s.move(VeloSessionLifecycle.State.WAITING_FOR_WINDOW,4,"wait"),"wait");
        check(s.move(VeloSessionLifecycle.State.WAITING_FOR_WINDOW,35000,"timeout"),"alert");
        check(s.state()!=VeloSessionLifecycle.State.RUNNING,"mouse not a window");
        check(s.move(VeloSessionLifecycle.State.STARTING_WINE,35001,"Wine"),"wine");
        check(s.move(VeloSessionLifecycle.State.RUNNING,35002,"mapped"),"mapped");
        check(!s.move(VeloSessionLifecycle.State.WAITING_FOR_WINDOW,35003,"stale"),"no regression");
        check(s.move(VeloSessionLifecycle.State.STOPPED,35004,"exit"),"shutdown");
        check(!s.move(VeloSessionLifecycle.State.RUNNING,35005,"late"),"terminal");
        System.out.println("VeloSessionLifecycleTest PASS");
    }
}
