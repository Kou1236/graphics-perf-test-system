#pragma once

#include <windows.h>

class RenderEventWriter {
public:
    // 初始化内部状态
    static bool Init();

    // 每次 SwapBuffers 被调用时调用
    static void OnSwap();

    // 释放资源
    static void Shutdown();

private:
    static bool s_initialized;
    static LONG64 s_frameCount;
    static CRITICAL_SECTION s_lock;
};