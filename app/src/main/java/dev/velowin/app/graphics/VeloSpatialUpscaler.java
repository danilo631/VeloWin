package dev.velowin.app.graphics;

import android.opengl.GLES30;

/**
 * VeloWin original GLES3 spatial reconstruction. Unlike Frame Generation,
 * this upscales a real low-resolution 3D render without synthesizing time.
 * FSR1 is a methodological reference, NOT the source of this shader.
 */
public final class VeloSpatialUpscaler {
    public enum Mode {
        NATIVE(1f,0f,"Nativo"), QUALITY(.77f,.25f,"Qualidade 77%"),
        BALANCED(.67f,.30f,"Equilibrado 67%"),
        PERFORMANCE(.50f,.35f,"Performance 50%");
        public final float scale,sharpness;
        public final String label;
        Mode(float scale,float sharpness,String label){
            this.scale=scale;this.sharpness=sharpness;this.label=label;
        }
    }
    private static final String VERTEX = """
        #version 300 es
        out vec2 uv;
        void main(){
            vec2 p=vec2(gl_VertexID==1?3.0:-1.0,gl_VertexID==2?3.0:-1.0);
            gl_Position=vec4(p,0.0,1.0);
            uv=p*0.5+0.5;
        }
        """;
    private static final String FRAGMENT = """
        #version 300 es
        precision highp float;
        in vec2 uv;
        out vec4 frag;
        uniform sampler2D uScene;
        uniform vec2 uTexel;
        uniform float uSharpness;
        float lum(vec3 v){return dot(v,vec3(.2126,.7152,.0722));}
        void main(){
            vec3 c=texture(uScene,uv).rgb;
            vec3 l=texture(uScene,uv-vec2(uTexel.x,0.)).rgb;
            vec3 r=texture(uScene,uv+vec2(uTexel.x,0.)).rgb;
            vec3 t=texture(uScene,uv-vec2(0.,uTexel.y)).rgb;
            vec3 b=texture(uScene,uv+vec2(0.,uTexel.y)).rgb;
            float gx=abs(lum(r)-lum(l)), gy=abs(lum(b)-lum(t));
            float gradient=gx+gy;
            float orientation=gx/(gradient+0.00001);
            vec3 directional=mix((l+r)*.5,(t+b)*.5,orientation);
            vec3 spatial=mix(c,directional,0.18*(1.-clamp(gradient*2.,0.,1.)));
            vec3 surround=(l+r+t+b)*.25;
            vec3 sharpened=spatial+uSharpness*(spatial-surround)
                *(.2+.8*clamp(gradient*2.,0.,1.));
            vec3 lower=min(c,min(min(l,r),min(t,b)));
            vec3 upper=max(c,max(max(l,r),max(t,b)));
            frag=vec4(clamp(sharpened,lower,upper),1.);
        }
        """;
    private int target,texture,depth,program,vao,srcW,srcH,outW,outH;
    private int samplerLoc,texelLoc,sharpLoc;
    private boolean active;
    private String fallback="";
    public String fallback(){return fallback;}
    public String resolution(){return srcW>0?srcW+"x"+srcH:"nativo";}
    private static int compile(int kind,String code){
        int shader=GLES30.glCreateShader(kind);
        if(shader==0)throw new IllegalStateException("No GLSL shader");
        GLES30.glShaderSource(shader,code);
        GLES30.glCompileShader(shader);
        int[] ok=new int[1];
        GLES30.glGetShaderiv(shader,GLES30.GL_COMPILE_STATUS,ok,0);
        if(ok[0]==0){
            String message=GLES30.glGetShaderInfoLog(shader);
            GLES30.glDeleteShader(shader);
            throw new IllegalStateException("GLSL: "+message);
        }
        return shader;
    }
    private void ensureProgram(){
        if(program!=0)return;
        int vs=compile(GLES30.GL_VERTEX_SHADER,VERTEX),fs=0;
        try{
            fs=compile(GLES30.GL_FRAGMENT_SHADER,FRAGMENT);
            program=GLES30.glCreateProgram();
            GLES30.glAttachShader(program,vs);
            GLES30.glAttachShader(program,fs);
            GLES30.glLinkProgram(program);
            int[] linked=new int[1];
            GLES30.glGetProgramiv(program,GLES30.GL_LINK_STATUS,linked,0);
            if(linked[0]==0)throw new IllegalStateException(
                "VeloSpatial link: "+GLES30.glGetProgramInfoLog(program));
            samplerLoc=GLES30.glGetUniformLocation(program,"uScene");
            texelLoc=GLES30.glGetUniformLocation(program,"uTexel");
            sharpLoc=GLES30.glGetUniformLocation(program,"uSharpness");
            int[] ids=new int[1];
            GLES30.glGenVertexArrays(1,ids,0);vao=ids[0];
            if(vao==0)throw new IllegalStateException("VeloSpatial VAO");
        }finally{
            GLES30.glDeleteShader(vs);
            if(fs!=0)GLES30.glDeleteShader(fs);
        }
    }
    private void releaseTargets(){
        if(depth!=0)GLES30.glDeleteRenderbuffers(1,new int[]{depth},0);
        if(texture!=0)GLES30.glDeleteTextures(1,new int[]{texture},0);
        if(target!=0)GLES30.glDeleteFramebuffers(1,new int[]{target},0);
        target=texture=depth=srcW=srcH=0;
    }
    private void resize(int w,int h){
        releaseTargets();
        int[] id=new int[1];
        GLES30.glGenTextures(1,id,0);texture=id[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,texture);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D,0,GLES30.GL_RGBA8,w,h,0,
            GLES30.GL_RGBA,GLES30.GL_UNSIGNED_BYTE,null);
        GLES30.glGenRenderbuffers(1,id,0);depth=id[0];
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER,depth);
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER,GLES30.GL_DEPTH_COMPONENT16,w,h);
        GLES30.glGenFramebuffers(1,id,0);target=id[0];
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,target);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D,texture,0);
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER,GLES30.GL_DEPTH_ATTACHMENT,
            GLES30.GL_RENDERBUFFER,depth);
        int status=GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,0);
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER,0);
        if(status!=GLES30.GL_FRAMEBUFFER_COMPLETE){
            releaseTargets();
            throw new IllegalStateException("GL framebuffer incomplete "+status);
        }
        srcW=w;srcH=h;
    }
    public boolean begin(int width,int height,Mode mode){
        active=false;
        outW=Math.max(1,width);outH=Math.max(1,height);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0);
        GLES30.glViewport(0,0,outW,outH);
        if(width<=1||height<=1||mode==Mode.NATIVE)return false;
        try{
            ensureProgram();
            int w=Math.max(1,Math.round(outW*mode.scale));
            int h=Math.max(1,Math.round(outH*mode.scale));
            if(target==0||srcW!=w||srcH!=h)resize(w,h);
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,target);
            GLES30.glViewport(0,0,srcW,srcH);
            fallback="";
            active=true;
            return true;
        }catch(RuntimeException error){
            releaseTargets();
            fallback=error.toString();
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0);
            GLES30.glViewport(0,0,outW,outH);
            return false;
        }
    }
    public void present(Mode mode){
        if(!active)return;
        active=false;
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0);
        GLES30.glViewport(0,0,outW,outH);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_BLEND);
        GLES30.glUseProgram(program);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,texture);
        GLES30.glUniform1i(samplerLoc,0);
        GLES30.glUniform2f(texelLoc,1f/srcW,1f/srcH);
        GLES30.glUniform1f(sharpLoc,mode.sharpness);
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES,0,3);
        GLES30.glBindVertexArray(0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,0);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }
    public void close(){
        releaseTargets();
        if(vao!=0)GLES30.glDeleteVertexArrays(1,new int[]{vao},0);
        if(program!=0)GLES30.glDeleteProgram(program);
        vao=program=0;
    }
}
