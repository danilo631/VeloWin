package dev.velowin.app.hardware;

import android.content.Context;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Build;

import java.util.Locale;

/** Creates a tiny private GLES2 pbuffer, reads GPU strings, always releases EGL. */
public final class GpuInspector {
    private GpuInspector() {}

    public static final class Info {
        public String renderer = "Desconhecida";
        public String vendor = "Desconhecido";
        public String opengl = "Não identificado";
        public String vulkan = "Não anunciado";
        public String abi = "";
        public String recommendation = "";
    }

    public static Info inspect(Context context) {
        Info out = new Info();
        out.abi = String.join(", ", Build.SUPPORTED_ABIS);
        PackageManager pm = context.getPackageManager();
        for (FeatureInfo f : pm.getSystemAvailableFeatures()) {
            if (f != null && PackageManager.FEATURE_VULKAN_HARDWARE_VERSION.equals(f.name)) {
                int major = (f.version >>> 22) & 0x3ff;
                int minor = (f.version >>> 12) & 0x3ff;
                out.vulkan = major + "." + minor + " (hardware informado pelo Android)";
            }
        }

        EGLDisplay display = EGL14.EGL_NO_DISPLAY;
        EGLContext gl = EGL14.EGL_NO_CONTEXT;
        EGLSurface surface = EGL14.EGL_NO_SURFACE;
        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            int[] version = new int[2];
            if (display == EGL14.EGL_NO_DISPLAY || !EGL14.eglInitialize(display, version, 0, version, 1))
                return finish(out);
            int[] attrs = {
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] count = new int[1];
            if (!EGL14.eglChooseConfig(display, attrs, 0, configs, 0, 1, count, 0)
                || count[0] == 0 || configs[0] == null) return finish(out);
            gl = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT,
                new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE}, 0);
            if (gl == EGL14.EGL_NO_CONTEXT) return finish(out);
            surface = EGL14.eglCreatePbufferSurface(display, configs[0],
                new int[]{EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE}, 0);
            if (surface == EGL14.EGL_NO_SURFACE
                    || !EGL14.eglMakeCurrent(display, surface, surface, gl)) return finish(out);
            String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
            String vendor = GLES20.glGetString(GLES20.GL_VENDOR);
            String glVersion = GLES20.glGetString(GLES20.GL_VERSION);
            if (renderer != null) out.renderer = renderer;
            if (vendor != null) out.vendor = vendor;
            if (glVersion != null) out.opengl = glVersion;
        }
        catch (RuntimeException | LinkageError ignored) {}
        finally {
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface);
                if (gl != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, gl);
                EGL14.eglTerminate(display);
            }
        }
        return finish(out);
    }

    private static Info finish(Info info) {
        String gpu = info.renderer.toLowerCase(Locale.ROOT);
        if (gpu.contains("mali") || gpu.contains("immortalis") || gpu.contains("xclipse")
                || gpu.contains("powervr")) {
            info.recommendation = "GPU não Adreno: preferir Vulkan do sistema. "
                + "Evite Turnip; DXVK ainda depende das extensões disponíveis.";
        }
        else if (gpu.contains("adreno")) {
            info.recommendation = "Adreno: driver do sistema primeiro; "
                + "Turnip poderá ser opção por jogo após teste de compatibilidade.";
        }
        else {
            info.recommendation = "GPU não classificada. Não substituir o driver automaticamente.";
        }
        if ("Não anunciado".equals(info.vulkan)) {
            info.recommendation += " Vulkan não anunciado pelo Android; não habilitar DXVK/VKD3D.";
        }
        else {
            String api = info.vulkan;
            if (api.startsWith("1.0") || api.startsWith("1.1") || api.startsWith("1.2"))
                info.recommendation += " VKD3D-Proton moderno requer Vulkan 1.3; este nível não foi anunciado.";
            else
                info.recommendation += " Vulkan informado pelo Android não garante todas as extensões DXVK/VKD3D.";
        }
        return info;
    }
}
