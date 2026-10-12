package dev.velowin.app.engine;

import java.util.Locale;

/**
 * Platform-neutral, synchronized controller state for the future Wine/XInput
 * bridge. The real Android lab uses it, but NO Win32 events are emitted yet.
 *
 * Bit flags follow XInput's XINPUT_GAMEPAD_* masks, except GUIDE is deliberately
 * omitted (Android can reserve it for system UI).
 */
public final class ControllerState {
    public enum Source { TOUCH, HARDWARE }
    public enum Button {
        DPAD_UP(0x0001), DPAD_DOWN(0x0002),
        DPAD_LEFT(0x0004), DPAD_RIGHT(0x0008),
        START(0x0010), BACK(0x0020),
        LEFT_THUMB(0x0040), RIGHT_THUMB(0x0080),
        LEFT_SHOULDER(0x0100), RIGHT_SHOULDER(0x0200),
        A(0x1000), B(0x2000), X(0x4000), Y(0x8000);
        public final int mask;
        Button(int mask) { this.mask=mask; }
    }
    public static final class Snapshot {
        public final int buttons;
        public final float leftX,leftY,rightX,rightY,leftTrigger,rightTrigger;
        public final long sequence;
        private Snapshot(int buttons,float leftX,float leftY,float rightX,float rightY,
                         float leftTrigger,float rightTrigger,long sequence) {
            this.buttons=buttons;this.leftX=leftX;this.leftY=leftY;
            this.rightX=rightX;this.rightY=rightY;
            this.leftTrigger=leftTrigger;this.rightTrigger=rightTrigger;
            this.sequence=sequence;
        }
        public boolean down(Button b) { return (buttons&b.mask)!=0; }
        public String describe() {
            StringBuilder names=new StringBuilder();
            for(Button b:Button.values())if(down(b)){
                if(names.length()>0)names.append(" · ");
                names.append(b.name());
            }
            if(names.length()==0)names.append("nenhum");
            return String.format(Locale.ROOT,
                "XInput-like · botões: %s\nL(%.2f, %.2f) R(%.2f, %.2f)\nLT %.2f · RT %.2f",
                names,leftX,leftY,rightX,rightY,leftTrigger,rightTrigger);
        }
    }

    private int touchButtons,hardwareButtons;
    private float touchLX,touchLY,touchRX,touchRY,hardwareLX,hardwareLY,hardwareRX,hardwareRY;
    private float touchLT,touchRT,hardwareLT,hardwareRT;
    private long sequence;

    private static float normalized(float value) {
        if(Float.isNaN(value)||Float.isInfinite(value))return 0f;
        return Math.max(-1f,Math.min(1f,value));
    }
    private static float axis(float value) {
        float v=normalized(value);
        // Device-independent preliminary deadzone. Does not implement per-pad calibration.
        return Math.abs(v)<0.16f?0f:v;
    }
    private static float trigger(float value) {
        if(Float.isNaN(value)||Float.isInfinite(value))return 0f;
        return Math.max(0f,Math.min(1f,value));
    }

    public synchronized void button(Source source,Button b,boolean pressed) {
        if(source==Source.HARDWARE)
            hardwareButtons=pressed?hardwareButtons|b.mask:hardwareButtons&~b.mask;
        else touchButtons=pressed?touchButtons|b.mask:touchButtons&~b.mask;
        sequence++;
    }
    /** Replaces the virtual control bitmask from all active touch pointers. */
    public synchronized void touchButtons(int mask) {
        int allowed=0;
        for(Button b:Button.values())allowed|=b.mask;
        touchButtons=mask&allowed;
        sequence++;
    }
    public synchronized void stick(Source source,boolean right,float x,float y) {
        x=axis(x);y=axis(y);
        if(source==Source.TOUCH){
            if(right){touchRX=x;touchRY=y;}else{touchLX=x;touchLY=y;}
        }else{
            if(right){hardwareRX=x;hardwareRY=y;}else{hardwareLX=x;hardwareLY=y;}
        }
        sequence++;
    }
    public synchronized void triggers(Source source,float left,float right) {
        if(source==Source.TOUCH){touchLT=trigger(left);touchRT=trigger(right);}
        else{hardwareLT=trigger(left);hardwareRT=trigger(right);}
        sequence++;
    }
    public synchronized void clear(Source source) {
        if(source==Source.TOUCH){
            touchButtons=0;touchLX=touchLY=touchRX=touchRY=touchLT=touchRT=0f;
        }else{
            hardwareButtons=0;hardwareLX=hardwareLY=hardwareRX=hardwareRY=hardwareLT=hardwareRT=0f;
        }
        sequence++;
    }
    public synchronized Snapshot snapshot() {
        return new Snapshot(touchButtons|hardwareButtons,
            normalized(touchLX+hardwareLX),normalized(touchLY+hardwareLY),
            normalized(touchRX+hardwareRX),normalized(touchRY+hardwareRY),
            Math.max(touchLT,hardwareLT),Math.max(touchRT,hardwareRT),sequence);
    }
}
