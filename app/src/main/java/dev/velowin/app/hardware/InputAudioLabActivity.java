package dev.velowin.app.hardware;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.media.AudioTrack;
import android.media.AudioFormat;
import android.media.AudioAttributes;
import java.util.Locale;

/** A real Android input/touch/audio test surface, not a Windows X server. */
public final class InputAudioLabActivity extends Activity {
    private TextView status;
    private InputCanvas canvas;
    private float axisX, axisY;
    private volatile boolean closed;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout layout=new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24,24,24,24);
        layout.setBackgroundColor(0xff0c0f14);
        TextView title=new TextView(this);
        title.setText("VeloWin · Laboratório de entrada");
        title.setTextSize(19);
        title.setTextColor(Color.WHITE);
        layout.addView(title);
        status=new TextView(this);
        status.setTextColor(0xff9ec2ff);
        status.setTextSize(14);
        status.setText("Toque na área, mova o controle ou pressione botões.\n"+connected());
        layout.addView(status);
        canvas=new InputCanvas();
        layout.addView(canvas,new LinearLayout.LayoutParams(-1,0,1));
        Button audio=new Button(this);
        audio.setText("Testar áudio Android (220 ms)");
        layout.addView(audio);
        audio.setOnClickListener(v->new Thread(this::audioTest,"velowin-audio-test").start());
        Button exit=new Button(this);
        exit.setText("Voltar");
        exit.setOnClickListener(v->finish());
        layout.addView(exit);
        setContentView(layout);
    }
    @Override public void onDestroy(){closed=true;super.onDestroy();}
    private String connected(){
        StringBuilder s=new StringBuilder("Controles: ");
        int matches=0;
        for(int id:InputDevice.getDeviceIds()){
            InputDevice dev=InputDevice.getDevice(id);
            if(dev==null)continue;
            int src=dev.getSources();
            if((src&InputDevice.SOURCE_GAMEPAD)==InputDevice.SOURCE_GAMEPAD
                ||(src&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK){
                if(matches++>0)s.append(", ");
                s.append(dev.getName());
            }
        }
        if(matches==0)s.append("nenhum");
        return s.toString();
    }
    @Override public boolean onGenericMotionEvent(MotionEvent event){
        if((event.getSource()&InputDevice.SOURCE_JOYSTICK)==InputDevice.SOURCE_JOYSTICK
            &&event.getAction()==MotionEvent.ACTION_MOVE){
            axisX=event.getAxisValue(MotionEvent.AXIS_X);
            axisY=event.getAxisValue(MotionEvent.AXIS_Y);
            status.setText(String.format(Locale.ROOT,"Controle: X %.2f / Y %.2f\n%s",
                axisX,axisY,connected()));
            canvas.invalidate();
            return true;
        }
        return super.onGenericMotionEvent(event);
    }
    @Override public boolean onKeyDown(int code,KeyEvent event){
        if(event.getDevice()!=null && (event.getSource()&InputDevice.SOURCE_GAMEPAD)
                ==InputDevice.SOURCE_GAMEPAD){
            status.setText("Botão do controle: "+KeyEvent.keyCodeToString(code));
            canvas.invalidate();
            return true;
        }
        return super.onKeyDown(code,event);
    }
    private void audioTest(){
        AudioTrack track=null;
        try{
            int sampleRate=44100;
            int samples=sampleRate*220/1000;
            short[] wave=new short[samples];
            for(int i=0;i<samples;i++){
                double envelope=Math.min(1.0,Math.min(i/1500.0,(samples-i)/1500.0));
                wave[i]=(short)(Math.sin(2.0*Math.PI*440.0*i/sampleRate)*envelope*2600);
            }
            track=new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples*2)
                .build();
            if(track.getState()!=AudioTrack.STATE_INITIALIZED)
                throw new IllegalStateException("AudioTrack não inicializado.");
            if(track.write(wave,0,wave.length,AudioTrack.WRITE_BLOCKING)!=samples)
                throw new IllegalStateException("Escrita de áudio incompleta.");
            track.play();
            Thread.sleep(280);
            if(!closed)runOnUiThread(()->status.setText("AudioTrack: reprodução de teste finalizada."));
        }
        catch(Exception error){
            final String msg=error.getClass().getSimpleName()+": "+error.getMessage();
            if(!closed)runOnUiThread(()->status.setText("Áudio: "+msg));
        }
        finally{
            if(track!=null){
                try{track.stop();}catch(IllegalStateException ignored){}
                track.release();
            }
        }
    }
    private final class InputCanvas extends View {
        private final Paint brush=new Paint(3);
        private float touchX=-1,touchY=-1;
        InputCanvas(){super(InputAudioLabActivity.this);setFocusable(true);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float cx=getWidth()/2f,cy=getHeight()/2f;
            brush.setColor(0xff1b2638);
            c.drawColor(0xff0c0f14);
            c.drawCircle(cx,cy,Math.min(getWidth(),getHeight())*0.30f,brush);
            brush.setColor(0xff9ec2ff);
            float rad=Math.min(getWidth(),getHeight())*0.28f;
            c.drawCircle(cx+axisX*rad,cy+axisY*rad,20,brush);
            if(touchX>=0){
                brush.setColor(0xfff6d365);
                c.drawCircle(touchX,touchY,30,brush);
            }
            brush.setTextSize(32f);
            brush.setColor(Color.WHITE);
            c.drawText("Android Canvas + Touch + Gamepad",20,45,brush);
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            int a=e.getActionMasked();
            if(a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_MOVE){
                touchX=e.getX();touchY=e.getY();
                status.setText(String.format(Locale.ROOT,"Toque: %.0f, %.0f",touchX,touchY));
            }else if(a==MotionEvent.ACTION_UP||a==MotionEvent.ACTION_CANCEL)touchX=-1;
            invalidate();return true;
        }
    }
}
