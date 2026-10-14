/*
 * VeloWin v16: ABI compatibility only, NO Lossless Scaling implementation.
 * Consumers compiled against Winlator's legacy VkrLsfg interface still link;
 * every LSFG capability returns unsupported. The only operational generator
 * is the GPLv3 OpenFlow DIS engine, linked separately in libopenflow.
 *
 * Copyright (c) VeloWin contributors. GPL-3.0-or-later.
 */
#include "../lsfg/vkr_lsfg.h"

extern "C" {
VkrLsfg* vkr_lsfg_create(VkDevice, VkPhysicalDevice, const char*) { return nullptr; }
void vkr_lsfg_destroy(VkrLsfg*) {}
void vkr_lsfg_configure(VkrLsfg*, uint32_t, uint32_t, float, float, float) {}
void vkr_lsfg_set_refresh_rate(VkrLsfg*, float) {}
void vkr_lsfg_set_guest_extent(VkrLsfg*, uint32_t, uint32_t) {}
bool vkr_lsfg_needs_rebuild(const VkrLsfg*, uint32_t, uint32_t, VkFormat) { return false; }
bool vkr_lsfg_prepare(VkrLsfg*, uint32_t, uint32_t, VkFormat) { return false; }
uint32_t vkr_lsfg_plan(VkrLsfg*, uint32_t, uint64_t) { return 0; }
void vkr_lsfg_note_frame(VkrLsfg*, uint64_t, uint32_t) {}
void vkr_lsfg_process(VkrLsfg*, VkCommandBuffer, VkImage, uint32_t, uint32_t, uint32_t) {}
void vkr_lsfg_generate_into(VkrLsfg*, VkCommandBuffer, uint32_t, uint32_t,
                            VkImage, VkImageView, uint32_t, uint32_t) {}
void vkr_lsfg_forget_targets(VkrLsfg*) {}
void vkr_lsfg_reset(VkrLsfg*) {}
}
