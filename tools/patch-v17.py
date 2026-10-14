#!/usr/bin/env python3
"""Inject independent VeloFlow pacing into real X11 and image presenter paths.

Strict, revision-pinned anchors prevent silently patching unrelated versions.
The DIS optical flow and Wine/XServer continue as separate upstream modules.
"""
from pathlib import Path
import argparse
import shutil

def once(s, old, new, where):
    if s.count(old)!=1:
        raise RuntimeError(f"VeloFlow {where}: upstream changed, occurrences={s.count(old)}")
    return s.replace(old,new)

def patch(source,root):
    native=root/"overlays/velowin-native/veloflow_pacing.h"
    if not native.is_file(): raise RuntimeError("missing original VeloFlow governor")
    vk=source/"app/src/main/cpp/winlator/vk"
    shutil.copyfile(native,vk/"veloflow_pacing.h")
    # Standalone image presenter, used by OpenFlow/DIS paths.
    present=vk/"framegen/fg_present.c"
    s=present.read_text()
    s=once(s,'#include "vkr_dis.h"',
        '#include "vkr_dis.h"\n#include "../veloflow_pacing.h"',"present include")
    old="""static uint32_t fg_dis_target(uint32_t multiplier, uint32_t target_rate, float source_rate) {
    if (target_rate != 0) return target_rate;
    if (multiplier > 1 && source_rate > 1.0f) {
        return (uint32_t)(source_rate * (float)multiplier + 0.5f);
    }
    return 0;
}"""
    new="""static uint32_t fg_dis_target(uint32_t multiplier, uint32_t target_rate,
                              float source_rate, float refresh_rate) {
    return veloflow_safe_target(target_rate, refresh_rate, source_rate, multiplier);
}"""
    s=once(s,old,new,"standalone target")
    s=once(s,"fg_dis_target(multiplier, target_rate, source_rate), refresh_rate",
        "fg_dis_target(multiplier, target_rate, source_rate, refresh_rate), refresh_rate",
        "present target call")
    present.write_text(s)
    # Main X11/Surface Vulkan compositor: clamp fixed DIS target to physical
    # panel Hz, preserving adaptive target=0 as the upstream estimator's input.
    renderer=vk/"vk_renderer.c"
    s=renderer.read_text()
    anchor='#include "vk_state.h"'
    if anchor not in s: raise RuntimeError("upstream X11 Vulkan header moved")
    s=once(s,anchor,anchor+'\n#include "veloflow_pacing.h"',"X11 include")
    old='vkr_dis_configure(r->dis, r->dis_scale, r->dis_target_fps, r->framegen_refresh_rate);'
    new='vkr_dis_configure(r->dis, r->dis_scale,\n'
    new+='                       veloflow_safe_target(r->dis_target_fps, r->framegen_refresh_rate,\n'
    new+='                                            0.0f, 2u), r->framegen_refresh_rate);'
    s=once(s,old,new,"X11 DIS pacing")
    renderer.write_text(s)
    print("VeloFlow: C11 FPS governor compiled into real X11 renderer and native presenter")

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--source",default="third_party/winnative")
    p.add_argument("--root",default=".")
    a=p.parse_args()
    patch(Path(a.source).resolve(),Path(a.root).resolve())
