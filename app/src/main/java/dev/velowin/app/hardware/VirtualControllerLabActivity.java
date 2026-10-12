package dev.velowin.app.hardware;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import dev.velowin.app.engine.ControllerState;
import java.util.Locale;

/**
 * Multi-touch virtual controller and Android physical gamepad input lab.
 * It drives an XInput-compatible value model only, not the Wine/X11 input stack.
 * No accessibility permissions, overlays over other apps or root needed.
 */
public final class VirtualControllerLabActivity extends Activity {
    private final ControllerState gamepad=new ControllerState();
    private TextView status;
    private InputPad pad;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16,16,16,16);
        root.setBackgroundColor(0xff0c0f14);
        TextView heading=new TextView(this);
        heading.setText("VeloWin · Controle de jogo");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(20);
        heading.setPadding(4,6,4,10);
        root.addView(heading);
        status=new TextView(this);
        status.setTextColor(0xffb0c9ed);
        status.setTextSize(12);
        status.setPadding(4,8,4,12);
        root.addView(status);
        pad=new InputPad();
        root.addView(pad,new LinearLayout.LayoutParams(-1,0,1f));
        TextView notice=new TextView(this);
        notice.setText("Toque com vários dedos ou conecte um controle Bluetooth/USB. "
            +"O estado XInput-like aparece acima. Ainda não injeta botões em Wine.");
        notice.setTextColor(0xff91a0b5);
        notice.setTextSize(12);
        notice.setPadding(4,10,4,18);
        root.addView(notice);
        setContentView(root);
        showState();
    }

    private void showState() {
        status.setText(gamepad.snapshot().describe()+"\n\nBluetooth/USB: "+physicalCount());
    }

    private String physicalCount() {
        int count=0;
        for(int id:InputDevice.getDeviceIds()) {
            InputDevice d=InputDevice.getDevice(id);
            if(d!=null && AndroidGamepadMapper.hasControllerSource(d.getSources()))count++;
        }
        return count+" controle(s)";
    }

    @Override public boolean onKeyDown(int code,KeyEvent event) {
        if(AndroidGamepadMapper.hasControllerSource(event.getSource())
            && AndroidGamepadMapper.key(gamepad,code,true)) {
            showState();pad.invalidate();return true;
        }
        return super.onKeyDown(code,event);
    }

    @Override public boolean onKeyUp(int code,KeyEvent event) {
        if(AndroidGamepadMapper.hasControllerSource(event.getSource())
            && AndroidGamepadMapper.key(gamepad,code,false)) {
            showState();pad.invalidate();return true;
        }
        return super.onKeyUp(code,event);
    }

    @Override public boolean onGenericMotionEvent(MotionEvent event) {
        if(AndroidGamepadMapper.motion(gamepad,event)){
            showState();pad.invalidate();return true;
        }
        return super.onGenericMotionEvent(event);
    }

    @Override protected void onPause() {
        gamepad.clear(ControllerState.Source.TOUCH);
        gamepad.clear(ControllerState.Source.HARDWARE);
        super.onPause();
    }

    private final class InputPad extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        InputPad() {super(VirtualControllerLabActivity.this);setFocusable(true);}
        private void circle(Canvas canvas,float x,float y,float radius,int color) {
            paint.setColor(color);
            canvas.drawCircle(x,y,radius,paint);
        }
        private void label(Canvas canvas,String text,float x,float y,float size) {
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(size);
            canvas.drawText(text,x,y+size*.32f,paint);
        }
        private void button(Canvas canvas,String name,float x,float y,float radius,int color,
                            ControllerState.Button button) {
            ControllerState.Snapshot state=gamepad.snapshot();
            circle(canvas,x,y,radius,state.down(button)?0xff70a5fa:color);
            label(canvas,name,x,y,radius*.75f);
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            int w=getWidth(),h=getHeight();
            if(w<=0||h<=0)return;
            c.drawColor(0xff121924);
            float r=Math.min(w,h)*.095f;
            ControllerState.Snapshot snap=gamepad.snapshot();
            float lx=w*.24f,ly=h*.68f,limit=Math.min(w*.18f,h*.20f);
            circle(c,lx,ly,limit,0xff293548);
            circle(c,lx+snap.leftX*limit*.70f,ly+snap.leftY*limit*.70f,
                limit*.32f,0xff8daeff);
            label(c,"STICK / DPAD",lx,ly+limit+27f,Math.max(12f,r*.47f));

            button(c,"X",w*.70f,h*.61f,r,0xff355278,ControllerState.Button.X);
            button(c,"Y",w*.81f,h*.47f,r,0xff355278,ControllerState.Button.Y);
            button(c,"A",w*.81f,h*.75f,r,0xff355278,ControllerState.Button.A);
            button(c,"B",w*.92f,h*.61f,r,0xff355278,ControllerState.Button.B);

            button(c,"BACK",w*.40f,h*.23f,r*.78f,0xff3a4251,ControllerState.Button.BACK);
            button(c,"START",w*.59f,h*.23f,r*.78f,0xff3a4251,ControllerState.Button.START);
            button(c,"LB",w*.20f,h*.095f,r*.78f,0xff46506a,ControllerState.Button.LEFT_SHOULDER);
            button(c,"RB",w*.80f,h*.095f,r*.78f,0xff46506a,ControllerState.Button.RIGHT_SHOULDER);
        }

        private boolean near(float x,float y,float cx,float cy,float r) {
            float dx=x-cx,dy=y-cy;
            return dx*dx+dy*dy<=r*r;
        }
        private int addIf(int mask,boolean inside,ControllerState.Button b) {
            return inside ? mask|b.mask : mask;
        }
        @Override public boolean onTouchEvent(MotionEvent e) {
            int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_CANCEL){
                gamepad.clear(ControllerState.Source.TOUCH);
                showState();invalidate();return true;
            }
            int skip=(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_POINTER_UP)
                ? e.getActionIndex():-1;
            int mask=0;
            float stickX=0,stickY=0;
            float w=getWidth(),h=getHeight(),radius=Math.min(w,h)*.095f;
            float limit=Math.min(w*.18f,h*.20f);
            for(int i=0;i<e.getPointerCount();i++) {
                if(i==skip)continue;
                float x=e.getX(i),y=e.getY(i);
                float dx=x-w*.24f,dy=y-h*.68f;
                if(Math.abs(dx)<limit*1.5f && Math.abs(dy)<limit*1.5f) {
                    float length=(float)Math.sqrt(dx*dx+dy*dy);
                    float scale=length>limit && length>0?limit/length:1f;
                    stickX=Math.max(-1f,Math.min(1f,dx*scale/limit));
                    stickY=Math.max(-1f,Math.min(1f,dy*scale/limit));
                    mask=addIf(mask,stickX<-.48f,ControllerState.Button.DPAD_LEFT);
                    mask=addIf(mask,stickX>.48f,ControllerState.Button.DPAD_RIGHT);
                    mask=addIf(mask,stickY<-.48f,ControllerState.Button.DPAD_UP);
                    mask=addIf(mask,stickY>.48f,ControllerState.Button.DPAD_DOWN);
                    continue;
                }
                mask=addIf(mask,near(x,y,w*.70f,h*.61f,radius*1.12f),ControllerState.Button.X);
                mask=addIf(mask,near(x,y,w*.81f,h*.47f,radius*1.12f),ControllerState.Button.Y);
                mask=addIf(mask,near(x,y,w*.81f,h*.75f,radius*1.12f),ControllerState.Button.A);
                mask=addIf(mask,near(x,y,w*.92f,h*.61f,radius*1.12f),ControllerState.Button.B);
                mask=addIf(mask,near(x,y,w*.40f,h*.23f,radius),ControllerState.Button.BACK);
                mask=addIf(mask,near(x,y,w*.59f,h*.23f,radius),ControllerState.Button.START);
                mask=addIf(mask,near(x,y,w*.20f,h*.095f,radius),ControllerState.Button.LEFT_SHOULDER);
                mask=addIf(mask,near(x,y,w*.80f,h*.095f,radius),ControllerState.Button.RIGHT_SHOULDER);
            }
            gamepad.touchButtons(mask);
            gamepad.stick(ControllerState.Source.TOUCH,false,stickX,stickY);
            showState();invalidate();
            return true;
        }
    }
}
