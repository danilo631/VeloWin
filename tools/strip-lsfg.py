#!/usr/bin/env python3
"""Remove native LSFG shader chain and DXBC build dependency, keep GPL OpenFlow DIS."""
import argparse
from pathlib import Path
import re
import shutil

def check(v,reason):
    if not v: raise RuntimeError("VeloFlow incompatible upstream: "+reason)

def one(s,old,new,where):
    check(s.count(old)==1,where)
    return s.replace(old,new)

def patch(source,root):
    cpp=source/"app/src/main/cpp"
    overlay=root/"overlays/velowin-native"
    win=cpp/"CMakeLists.txt"; way=cpp/"waylandcomp/CMakeLists.txt"
    w=win.read_text(); y=way.read_text()
    w,nw=re.subn(r"(?m)^[ \t]*winlator/vk/lsfg/[\w.]+[ \t]*\n","",w)
    y,ny=re.subn(r"(?m)^[ \t]*\$\{FRAMEGEN_DIR\}/lsfg/[\w.]+[ \t]*\n","",y)
    check(nw>=10 and ny>=10,f"LSFG inventory changed: {nw}, {ny}")
    w=one(w,"        winlator/vk/framegen/fg_present.c",
          "        winlator/vk/framegen/veloflow_compat.cpp\n"
          "        winlator/vk/framegen/fg_present.c","X11 compositor native stub")
    way_var="$"+"{FRAMEGEN_DIR}"
    y=one(y,f"            {way_var}/vk_dispatch.c",
          f"            {way_var}/vk_dispatch.c\n"
          f"            {way_var}/framegen/veloflow_compat.cpp",
          "Wayland compositor native stub")
    w=one(w,"add_subdirectory(thirdparty/dxbc EXCLUDE_FROM_ALL)\n","",
          "unused LSFG shader DXBC translator")
    w=one(w,"        dxbc\n","","LSFG-only linker entry")
    y=one(y,"if (TARGET dxbc AND TARGET openflow)","if (TARGET openflow)",
          "Wayland DIS-only build gate")
    y=one(y,"target_link_libraries(wnwayland dxbc openflow)",
          "target_link_libraries(wnwayland openflow)","Wayland DIS-only link")
    check("winlator/vk/lsfg/" not in w and "/lsfg/" not in y,
          "native CMake still includes LSFG")
    win.write_text(w);way.write_text(y)
    d=cpp/"winlator/vk/framegen"
    shutil.copyfile(overlay/"veloflow_compat.cpp",d/"veloflow_compat.cpp")
    java=source/"app/src/main/runtime/display/lsfg/LosslessScaling.java"
    check(java.is_file(),"missing legacy class")
    shutil.copyfile(overlay/"LosslessScaling.java",java)
    activity=source/"app/src/main/runtime/display/XServerDisplayActivity.java"
    s=activity.read_text()
    s=one(s,'frameGenEnabled = "1".equals(getFrameGenSetting("frameGen", containerValue));',
          'frameGenEnabled = false; // VeloFlow DIS only',
          "remove shader engine activation")
    activity.write_text(s)
    drawer=source/"app/src/main/runtime/display/XServerDrawerFrameGenPane.kt"
    s=drawer.read_text()
    s=one(s,"val engines = FrameGenEngine.values()",
          "val engines = FrameGenEngine.values().filter { it != FrameGenEngine.LSFG }",
          "remove shader engine UI")
    drawer.write_text(s)
    presenter=d/"fg_present.c"
    s=presenter.read_text()
    old="fg->engine = engine == FG_ENGINE_DIS ? FG_ENGINE_DIS : FG_ENGINE_LSFG;"
    check(s.count(old)==2,"presenter engine assignments")
    s=s.replace(old,"fg->engine = FG_ENGINE_DIS; /* VeloFlow */")
    s=one(s,"if (!output || width == 0 || height == 0 || !cache_path) return NULL;",
          "if (!output || width == 0 || height == 0) return NULL;",
          "DIS requires no shader DLL cache")
    s=one(s,"fg->cache_path = strdup(cache_path);",
          "fg->cache_path = cache_path ? strdup(cache_path) : NULL;",
          "nullable unused shader cache")
    fallback=re.compile(
        r"    if \(!fg_engine_acquire\(fg\)\) \{\n"
        r"        const uint32_t fallback =\n"
        r"            fg->active_engine == FG_ENGINE_DIS \? FG_ENGINE_LSFG : FG_ENGINE_DIS;\n"
        r"        FG_LOGW\(\"%s unavailable; trying %s\", fg_engine_name\(fg->active_engine\),\n"
        r"                fg_engine_name\(fallback\)\);\n"
        r"        fg->active_engine = fallback;\n"
        r"        fg->engine = fallback;\n"
        r"        if \(!fg_engine_acquire\(fg\)\) goto fail;\n"
        r"    \}")
    s,n=fallback.subn(
        '    if (!fg_engine_acquire(fg)) {\n'
        '        FG_LOGW("VeloFlow DIS unsupported on this Vulkan driver");\n'
        '        goto fail;\n'
        '    }',s)
    check(n==1,"proprietary engine fallback")
    presenter.write_text(s)
    jni=d/"fg_jni.c"
    s=jni.read_text()
    s=one(s,"    *engine = FG_ENGINE_LSFG;\n"
          "    *flow = quality > 0 ? (float)quality / 100.0f : 0.7f;\n"
          "    *dis_min_side = FG_DIS_MIN_SIDE_DEFAULT;",
          "    *engine = FG_ENGINE_DIS;\n"
          "    *flow = 0.7f;\n"
          "    *dis_min_side = FG_DIS_MIN_SIDE_DEFAULT;",
          "JNI native engine selection")
    s=s.replace("uint32_t engine = FG_ENGINE_LSFG;",
                "uint32_t engine = FG_ENGINE_DIS;")
    check("FG_ENGINE_LSFG" not in s,"JNI still requests proprietary engine")
    jni.write_text(s)
    legacy=cpp/"winlator/vk/lsfg"
    deleted=0
    for p in legacy.iterdir():
        if p.name!="vkr_lsfg.h" and p.suffix in (".c",".cpp",".hpp",".h"):
            p.unlink();deleted+=1
    check(deleted>=15,"too few LSFG sources removed")
    print(f"VeloFlow removed {nw}+{ny} native compile entries and {deleted} LSFG source files")
    print("DIS OpenFlow remains linked into X11/Wayland; LSFG ABI returns unsupported")

if __name__=="__main__":
    a=argparse.ArgumentParser()
    a.add_argument("--source",default="third_party/winnative")
    a.add_argument("--root",default=".")
    v=a.parse_args()
    patch(Path(v.source).resolve(),Path(v.root).resolve())
