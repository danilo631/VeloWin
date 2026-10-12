package dev.velowin.app.hardware;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import dev.velowin.app.engine.ControllerState;

/** Android gamepad and joystick events translated into the shared XInput-like lab state. */
public final class AndroidGamepadMapper {
    private AndroidGamepadMapper() {}

    public static boolean hasControllerSource(int source) {
        return (source & InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD
            || (source & InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK;
    }

    public static boolean key(ControllerState controller,int code,boolean down) {
        final ControllerState.Button button;
        switch(code) {
            case KeyEvent.KEYCODE_BUTTON_A: button=ControllerState.Button.A;break;
            case KeyEvent.KEYCODE_BUTTON_B: button=ControllerState.Button.B;break;
            case KeyEvent.KEYCODE_BUTTON_X: button=ControllerState.Button.X;break;
            case KeyEvent.KEYCODE_BUTTON_Y: button=ControllerState.Button.Y;break;
            case KeyEvent.KEYCODE_BUTTON_L1: button=ControllerState.Button.LEFT_SHOULDER;break;
            case KeyEvent.KEYCODE_BUTTON_R1: button=ControllerState.Button.RIGHT_SHOULDER;break;
            case KeyEvent.KEYCODE_BUTTON_THUMBL: button=ControllerState.Button.LEFT_THUMB;break;
            case KeyEvent.KEYCODE_BUTTON_THUMBR: button=ControllerState.Button.RIGHT_THUMB;break;
            case KeyEvent.KEYCODE_BUTTON_START: button=ControllerState.Button.START;break;
            case KeyEvent.KEYCODE_BUTTON_SELECT: button=ControllerState.Button.BACK;break;
            case KeyEvent.KEYCODE_DPAD_UP: button=ControllerState.Button.DPAD_UP;break;
            case KeyEvent.KEYCODE_DPAD_DOWN: button=ControllerState.Button.DPAD_DOWN;break;
            case KeyEvent.KEYCODE_DPAD_LEFT: button=ControllerState.Button.DPAD_LEFT;break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: button=ControllerState.Button.DPAD_RIGHT;break;
            default:return false;
        }
        controller.button(ControllerState.Source.HARDWARE,button,down);
        return true;
    }

    private static float axis(MotionEvent event,int kind) {
        InputDevice device=event.getDevice();
        if(device==null)return 0;
        InputDevice.MotionRange range=device.getMotionRange(kind,event.getSource());
        return range!=null ? event.getAxisValue(kind) : 0f;
    }

    public static boolean motion(ControllerState controller,MotionEvent event) {
        if(!hasControllerSource(event.getSource())
            || event.getActionMasked()!=MotionEvent.ACTION_MOVE)return false;
        controller.stick(ControllerState.Source.HARDWARE,false,
            axis(event,MotionEvent.AXIS_X),axis(event,MotionEvent.AXIS_Y));
        float rx=axis(event,MotionEvent.AXIS_Z),ry=axis(event,MotionEvent.AXIS_RZ);
        if(Math.abs(rx)+Math.abs(ry)<0.001f) {
            rx=axis(event,MotionEvent.AXIS_RX);ry=axis(event,MotionEvent.AXIS_RY);
        }
        controller.stick(ControllerState.Source.HARDWARE,true,rx,ry);
        float lt=Math.max(axis(event,MotionEvent.AXIS_LTRIGGER),
            axis(event,MotionEvent.AXIS_BRAKE));
        float rt=Math.max(axis(event,MotionEvent.AXIS_RTRIGGER),
            axis(event,MotionEvent.AXIS_GAS));
        controller.triggers(ControllerState.Source.HARDWARE,lt,rt);

        float hx=axis(event,MotionEvent.AXIS_HAT_X),hy=axis(event,MotionEvent.AXIS_HAT_Y);
        controller.button(ControllerState.Source.HARDWARE,ControllerState.Button.DPAD_LEFT,hx<-.5f);
        controller.button(ControllerState.Source.HARDWARE,ControllerState.Button.DPAD_RIGHT,hx>.5f);
        controller.button(ControllerState.Source.HARDWARE,ControllerState.Button.DPAD_UP,hy<-.5f);
        controller.button(ControllerState.Source.HARDWARE,ControllerState.Button.DPAD_DOWN,hy>.5f);
        return true;
    }
}
