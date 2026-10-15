#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include "../overlays/velowin-native/veloflow_pacing.h"
int main(void){
    VeloFlowCapacityGuard g={0};
    assert(veloflow_capacity_guard(&g,3,0,1)==3);
    assert(veloflow_capacity_guard(&g,3,1,2)==1);
    assert(veloflow_capacity_guard(&g,3,1,30)==1);
    assert(veloflow_capacity_guard(&g,3,1,121)==1);
    assert(veloflow_capacity_guard(&g,3,1,122)==2);
    assert(veloflow_capacity_guard(&g,3,1,150)==2);
    assert(veloflow_capacity_guard(&g,3,1,242)==3);
    assert(veloflow_capacity_guard(&g,3,2,243)==1);
    assert(veloflow_capacity_guard(&g,0,2,250)==0);
    assert(veloflow_capacity_guard(NULL,3,0,1)==3);
    printf("VeloFlowCapacityTest PASS\n");
    return 0;
}
