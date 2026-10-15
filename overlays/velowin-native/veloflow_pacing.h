/* VeloWin-owned low-overhead Vulkan frame-pacing guard.
 * The DIS optical-flow kernel is third-party GPL. This policy is original.
 * Ensures a requested FPS cannot exceed panel refresh and rejects impossible
 * NaN/negative rates; independent of WinNative and builds with C11 host tests.
 */
#ifndef VELOFLOW_PACING_H
#define VELOFLOW_PACING_H
#include <math.h>
#include <stdint.h>
static inline uint32_t veloflow_safe_target(uint32_t requested, float display_hz,
                                             float source_hz, uint32_t multiplier) {
    /* Zero means adaptive; respect the source frame rate and GPU queue. */
    if (!isfinite(display_hz) || display_hz < 1.0f) return 0u;
    uint32_t panel=(uint32_t)floorf(display_hz+0.25f);
    if(panel>240u)panel=240u;
    if (panel<30u)return 0u;
    if(requested>0u) return requested<panel?requested:panel;
    if(!isfinite(source_hz) || source_hz<1.0f) return 0u;
    const uint32_t m=multiplier>1u?(multiplier>3u?3u:multiplier):2u;
    const float goal=source_hz*(float)m;
    if(goal<1.0f)return 0u;
    uint32_t projected=(uint32_t)(goal+0.5f);
    return projected<panel?projected:panel;
}
/*
 * DIS generation pressure guard. Without this, a saturated 120 Hz display
 * can repeatedly wait for unavailable swapchain images, compounding input
 * latency. Adapt at most once per real frame; always preserve the actual
 * image and allow one synthesized frame if available.
 *
 * VeloWin original C11 policy, independent of Vulkan/Android and covered
 * by host-side tests. Not a replacement optical-flow algorithm.
 */
typedef struct {
    uint64_t last_misses;
    uint64_t resume_after_frame;
    uint32_t capacity_limit;
    uint8_t initialized;
} VeloFlowCapacityGuard;

static inline uint32_t veloflow_capacity_guard(VeloFlowCapacityGuard* guard,
                                                uint32_t capacity, uint64_t misses,
                                                uint64_t real_frame_index) {
    if(!guard)return capacity;
    if(!guard->initialized){
        guard->initialized=1;
        guard->capacity_limit=capacity;
        guard->last_misses=misses;
        guard->resume_after_frame=real_frame_index;
    }
    if(misses>guard->last_misses){
        guard->last_misses=misses;
        if(guard->capacity_limit>1u)guard->capacity_limit=1u;
        guard->resume_after_frame=real_frame_index+120u;
    }else if(real_frame_index>=guard->resume_after_frame
             && guard->capacity_limit<capacity){
        guard->capacity_limit++;
        guard->resume_after_frame=real_frame_index+120u;
    }
    return capacity<guard->capacity_limit?capacity:guard->capacity_limit;
}

#endif
