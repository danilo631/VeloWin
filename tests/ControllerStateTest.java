import dev.velowin.app.engine.ControllerState;
import static dev.velowin.app.engine.ControllerState.Source.*;
import static dev.velowin.app.engine.ControllerState.Button.*;

public final class ControllerStateTest {
    private static void check(boolean yes,String why) {
        if(!yes)throw new AssertionError(why);
    }
    public static void main(String[] args) {
        ControllerState pad=new ControllerState();
        ControllerState.Snapshot initial=pad.snapshot();
        check(initial.buttons==0,"blank");
        pad.button(HARDWARE,A,true);
        pad.button(TOUCH,B,true);
        ControllerState.Snapshot both=pad.snapshot();
        check(both.down(A)&&both.down(B),"merge");
        pad.clear(TOUCH);
        check(pad.snapshot().down(A) && !pad.snapshot().down(B),"clear one source only");
        pad.stick(HARDWARE,false,0.12f,-0.9f);
        check(pad.snapshot().leftX==0,"deadzone");
        check(pad.snapshot().leftY<-.8f,"axis");
        pad.stick(TOUCH,false,0.75f,Float.NaN);
        check(pad.snapshot().leftX>.7f,"stick merge");
        check(pad.snapshot().leftY<-.8f,"stick Y preserves hardware");
        pad.triggers(HARDWARE,Float.POSITIVE_INFINITY,2f);
        check(pad.snapshot().leftTrigger==0,"NaN/inf");
        check(pad.snapshot().rightTrigger==1f,"clamp");
        pad.triggers(TOUCH,.8f,.5f);
        check(pad.snapshot().leftTrigger==.8f,"trigger merge");
        pad.button(HARDWARE,A,false);
        check(!pad.snapshot().down(A),"release");
        pad.touchButtons(A.mask|Y.mask);
        check(pad.snapshot().down(A)&&pad.snapshot().down(Y),"touch mask");
        pad.clear(HARDWARE);
        pad.clear(TOUCH);
        check(pad.snapshot().buttons==0,"all released");
        check(pad.snapshot().rightTrigger==0,"triggers reset");
        check(pad.snapshot().sequence>initial.sequence,"sequence");
        System.out.println("ControllerStateTest PASS");
    }
}
