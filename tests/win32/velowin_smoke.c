/*
 * VeloWin native Windows console smoke (original source).
 * Compiled on GitHub Actions with MinGW-w64 for PE32+ x86_64.
 * No network, filesystem writes, audio, graphics, dynamic loading or input.
 */
#include <windows.h>
#include <stdio.h>

int main(void) {
    SYSTEM_INFO system;
    ZeroMemory(&system, sizeof(system));
    GetSystemInfo(&system);
    if (sizeof(void *) != 8)
        return 42;
    if (system.dwPageSize == 0)
        return 43;
    printf("VELO_WIN32_SMOKE_OK\n");
    printf("Win32 API GetSystemInfo: page_size=%lu, processor_arch=%u\n",
        (unsigned long)system.dwPageSize,
        (unsigned)system.wProcessorArchitecture);
    fflush(stdout);
    return 0;
}
