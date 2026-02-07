#include <windows.h>
#include "RenderEventWriter.h"
#include "SwapHook.h"

static bool g_moduleInitialized = false;

BOOL APIENTRY DllMain(HMODULE hModule,
                      DWORD  ul_reason_for_call,
                      LPVOID lpReserved) {
    switch (ul_reason_for_call) {
    case DLL_PROCESS_ATTACH:
        DisableThreadLibraryCalls(hModule);

        OutputDebugStringA("[tp_opengl_hook] DLL_PROCESS_ATTACH\n");

        if (!RenderEventWriter::Init()) {
            OutputDebugStringA("[tp_opengl_hook] RenderEventWriter::Init failed\n");
            break;
        }

        if (!InitSwapHook()) {
            OutputDebugStringA("[tp_opengl_hook] InitSwapHook failed\n");
            RenderEventWriter::Shutdown();
            break;
        }

        g_moduleInitialized = true;
        break;

    case DLL_PROCESS_DETACH:
        OutputDebugStringA("[tp_opengl_hook] DLL_PROCESS_DETACH\n");

        if (g_moduleInitialized) {
            ShutdownSwapHook();
            RenderEventWriter::Shutdown();
            g_moduleInitialized = false;
        }
        break;

    case DLL_THREAD_ATTACH:
    case DLL_THREAD_DETACH:
        break;
    }

    return TRUE;
}