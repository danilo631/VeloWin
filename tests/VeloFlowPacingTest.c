#include <assert.h>
#include <stdint.h>
#include <math.h>
#include <stdio.h>
#include "../overlays/velowin-native/veloflow_pacing.h"
int main(void) {
    assert(veloflow_safe_target(120u,60.0f,45.0f,2u)==60u);
    assert(veloflow_safe_target(60u,120.0f,30.0f,2u)==60u);
    assert(veloflow_safe_target(0u,120.0f,30.0f,2u)==60u);
    assert(veloflow_safe_target(0u,90.0f,60.0f,3u)==90u);
    assert(veloflow_safe_target(0u,90.0f,0.0f,2u)==0u);
    assert(veloflow_safe_target(120u,0.0f,35.0f,2u)==0u);
    assert(veloflow_safe_target(120u,NAN,35.0f,2u)==0u);
    assert(veloflow_safe_target(0u,120.0f,INFINITY,2u)==0u);
    assert(veloflow_safe_target(60u,24.0f,30.0f,2u)==0u);
    assert(veloflow_safe_target(200u,240.0f,30.0f,2u)==200u);
    assert(veloflow_safe_target(400u,1000.0f,30.0f,2u)==240u);
    puts("VeloFlowPacingTest PASS");
    return 0;
}
