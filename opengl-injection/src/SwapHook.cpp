#include "SwapHook.h"
#include "RenderEventWriter.h"

#include <windows.h>
#include "MinHook.h"

typedef BOOL (WINAPI* PFN_SwapBuffers)(HDC hdc);

static PFN_SwapBuffers g_originalSwapBuffers = nullptr;
static bool g_hookEnabled = false;

static BOOL WINAPI HookedSwapBuffers(HDC hdc) {
    if (g_hookEnabled) {
        RenderEventWriter::OnSwap();
    }

    if (g_originalSwapBuffers) {
        return g_originalSwapBuffers(hdc);
    }

    return FALSE;
}

bool InitSwapHook() {
    OutputDebugStringA("[tp_opengl_hook] InitSwapHook begin\n");

    if (MH_Initialize() != MH_OK) {
        OutputDebugStringA("[tp_opengl_hook] MH_Initialize failed\n");
        return false;
    }

    HMODULE hGdi32 = GetModuleHandleW(L"gdi32.dll");
    if (!hGdi32) {
        OutputDebugStringA("[tp_opengl_hook] GetModuleHandleW(gdi32.dll) failed\n");
        return false;
    }

    FARPROC pSwap = GetProcAddress(hGdi32, "SwapBuffers");
    if (!pSwap) {
        OutputDebugStringA("[tp_opengl_hook] GetProcAddress(SwapBuffers) failed\n");
        return false;
    }

    MH_STATUS status = MH_CreateHook(
        pSwap,
        reinterpret_cast<LPVOID>(&HookedSwapBuffers),
        reinterpret_cast<LPVOID*>(&g_originalSwapBuffers)
    );
    if (status != MH_OK) {
        OutputDebugStringA("[tp_opengl_hook] MH_CreateHook failed\n");
        return false;
    }

    status = MH_EnableHook(pSwap);
    if (status != MH_OK) {
        OutputDebugStringA("[tp_opengl_hook] MH_EnableHook failed\n");
        return false;
    }

    g_hookEnabled = true;
    OutputDebugStringA("[tp_opengl_hook] InitSwapHook success\n");
    return true;
}

void ShutdownSwapHook() {
    OutputDebugStringA("[tp_opengl_hook] ShutdownSwapHook begin\n");

    g_hookEnabled = false;

    MH_DisableHook(MH_ALL_HOOKS);
    MH_Uninitialize();

    OutputDebugStringA("[tp_opengl_hook] ShutdownSwapHook end\n");
}