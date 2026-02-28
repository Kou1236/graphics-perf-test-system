#pragma once
#include <windows.h>
#include <string>
#include <cstdint>

class RenderEventWriter {
public:
    static bool Init();
    static void OnSwap();
    static void Shutdown();

private:
    static bool LoadRenderEventsPathFromTempConfig(std::wstring& outPath);
    static std::wstring GetTempDir();
    static std::wstring GetProbeConfigPathForCurrentPid();
    static std::wstring GetErrorPathForCurrentPid();
    static void WriteErrorFile(const std::wstring& msg);

    static bool OpenLogFile(const std::wstring& logPath);
    static void AppendLineUtf8(const char* line, size_t len);
    static uint64_t NowMsQpc();

private:
    static bool s_initialized;
    static LONG64 s_swapCount;

    static CRITICAL_SECTION s_lock;
    static bool s_lockInited;

    static HANDLE s_file;
    static std::wstring s_logPath;
};