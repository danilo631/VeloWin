package dev.velowin.app.graphics;

import android.opengl.GLES30;

/**
 * Experimental Android GLES3 temporal midpoint synthesis for VeloBench.
 *
 * Captures TWO actually rendered images, compares neighborhood luminance
 * along 9 candidate offsets, warps both frames towards their midpoint and
 * uses a mismatch threshold to reduce double edges. Not AMD FSR3/DLSS,
 * not Vulkan compute, and NOT connected to Wine/XServer swapchains.
 *
 * Runs exclusively on the GLSurfaceView GL thread. A generated preview
 * frame is never counted as another real game-rendered frame.
 */
public final class VeloTemporalFrameGen {
    private static final String VS = """
        #version 300 es
        out vec2 uv;
        void main(){
            vec2 p=vec2(gl_VertexID==1?3.0:-1.0,gl_VertexID==2?3.0:-1.0);
            gl_Position=vec4(p,0.0,1.0);
            uv=p*0.5+0.5;
        }
        """;
    private static final String FS = """
        #version 300 es
        precision highp float;
        in vec2 uv;
        out vec4 color;
        uniform sampler2D uPrev, uNext;
        uniform vec2 uTexel;
        uniform int uMid;
        float brightness(vec3 c){return dot(c,vec3(.2126,.7152,.0722));}
        void main(){
            if(uMid==0){color=texture(uNext,uv);return;}
            vec3 target=texture(uNext,uv).rgb;
            float yTarget=brightness(target);
            vec2 offset=vec2(0.);
            float best=100.0;
            // Small per-pixel optical correspondence search, not game motion vectors.
            for(int y=-1;y<=1;y++){
                for(int x=-1;x<=1;x++){
                    vec2 o=vec2(float(x),float(y))*uTexel;
                    vec3 candidate=texture(uPrev,uv+o).rgb;
                    float error=abs(brightness(candidate)-yTarget);
                    // Prefer zero motion when correspondence is ambiguous.
                    error+=.008*float(abs(x)+abs(y));
                    if(error<best){best=error;offset=o;}
                }
            }
            vec3 a=texture(uPrev,uv+offset*.5).rgb;
            vec3 b=texture(uNext,uv-offset*.5).rgb;
            float disagree=length(a-b);
            // Occlusion / disocclusion: avoid ghosting when colors disagree.
            float confidence=1.-smoothstep(.07,.28,disagree);
            vec3 interpolated=mix(b,(a+b)*.5,confidence);
            vec3 lower=min(a,b);
            vec3 upper=max(a,b);
            color=vec4(clamp(interpolated,lower,upper),1.);
        }
        """;
    private int[] frames=new int[2];
    private int width,height,previous=-1,current=-1;
    private int program,vao,prevLoc,nextLoc,texelLoc,midLoc;
    private boolean pending;
    private String problem="";
    public boolean hasPending(){return pending;}
    public String problem(){return problem;}
    public void reset(){
        releaseImages();
        pending=false;
        current=previous=-1;
    }
    private static int shader(int kind,String code){
        int obj=GLES30.glCreateShader(kind);
        if(obj==0)throw new IllegalStateException("Create temporal shader failed");
        GLES30.glShaderSource(obj,code);
        GLES30.glCompileShader(obj);
        int[] ok={0};
        GLES30.glGetShaderiv(obj,GLES30.GL_COMPILE_STATUS,ok,0);
        if(ok[0]==0){
            String error=GLES30.glGetShaderInfoLog(obj);
            GLES30.glDeleteShader(obj);
            throw new IllegalStateException(error);
        }
        return obj;
    }
    private void ensureProgram(){
        if(program!=0)return;
        int v=shader(GLES30.GL_VERTEX_SHADER,VS),f=0;
        try{
            f=shader(GLES30.GL_FRAGMENT_SHADER,FS);
            program=GLES30.glCreateProgram();
            GLES30.glAttachShader(program,v);
            GLES30.glAttachShader(program,f);
            GLES30.glLinkProgram(program);
            int[] ok={0};
            GLES30.glGetProgramiv(program,GLES30.GL_LINK_STATUS,ok,0);
            if(ok[0]==0)throw new IllegalStateException(GLES30.glGetProgramInfoLog(program));
            prevLoc=GLES30.glGetUniformLocation(program,"uPrev");
            nextLoc=GLES30.glGetUniformLocation(program,"uNext");
            texelLoc=GLES30.glGetUniformLocation(program,"uTexel");
            midLoc=GLES30.glGetUniformLocation(program,"uMid");
            int[] ids={0};
            GLES30.glGenVertexArrays(1,ids,0);vao=ids[0];
            if(vao==0)throw new IllegalStateException("Temporal VAO failed");
        }finally{
            GLES30.glDeleteShader(v);
            if(f!=0)GLES30.glDeleteShader(f);
        }
    }
    private void releaseImages(){
        if(frames[0]!=0||frames[1]!=0)
            GLES30.glDeleteTextures(2,frames,0);
        frames[0]=frames[1]=0;
        width=height=0;
    }
    private void ensureSize(int w,int h){
        if(w==width&&h==height&&frames[0]!=0)return;
        reset();
        GLES30.glGenTextures(2,frames,0);
        for(int texture:frames){
            if(texture==0)throw new IllegalStateException("Temporal texture failed");
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,texture);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_S,GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_WRAP_T,GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D,0,GLES30.GL_RGBA8,w,h,0,
                GLES30.GL_RGBA,GLES30.GL_UNSIGNED_BYTE,null);
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,0);
        width=w;height=h;
    }
    /**
     * Must be called after drawing the real 3D image to framebuffer 0.
     * Returns false on GL failure/unsupported resolution, preserving real frame.
     */
    public boolean capture(int w,int h){
        if(w<2||h<2||((long)w*h)>4_000_000L)return false;
        try{
            ensureProgram();
            ensureSize(w,h);
            int newIndex=current<0?0:1-current;
            GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER,0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,frames[newIndex]);
            GLES30.glCopyTexSubImage2D(GLES30.GL_TEXTURE_2D,0,0,0,0,0,w,h);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,0);
            int error=GLES30.glGetError();
            if(error!=GLES30.GL_NO_ERROR)throw new IllegalStateException("GL copy "+error);
            previous=current;
            current=newIndex;
            pending=previous>=0;
            problem="";
            return true;
        }catch(RuntimeException ex){
            reset();
            problem=ex.toString();
            return false;
        }
    }
    private void present(int mid){
        if(current<0)return;
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,0);
        GLES30.glViewport(0,0,width,height);
        GLES30.glDisable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_BLEND);
        GLES30.glUseProgram(program);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,
            previous>=0?frames[previous]:frames[current]);
        GLES30.glUniform1i(prevLoc,0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,frames[current]);
        GLES30.glUniform1i(nextLoc,1);
        GLES30.glUniform2f(texelLoc,1f/width,1f/height);
        GLES30.glUniform1i(midLoc,mid);
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES,0,3);
        GLES30.glBindVertexArray(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,0);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
    }
    /** Draw distinct halfway image from real sequential frames. */
    public boolean presentIntermediate(){
        if(!pending||previous<0)return false;
        present(1);
        return true;
    }
    /** Finish the pair by presenting the actual frame, no extra 3D render. */
    public boolean presentLatest(){
        if(!pending)return false;
        present(0);
        pending=false;
        return true;
    }
    public void close(){
        reset();
        if(vao!=0)GLES30.glDeleteVertexArrays(1,new int[]{vao},0);
        if(program!=0)GLES30.glDeleteProgram(program);
        vao=program=0;
    }
}
