#include "RenderEventWriter.h"
#include <string>

bool RenderEventWriter::s_initialized = false;
LONG64 RenderEventWriter::s_frameCount = 0;
CRITICAL_SECTION RenderEventWriter::s_lock;

static void OutputDebugStringFormat(const char* fmt, LONG64 value) {
    char buffer[256] = {0};
    int len = _snprintf_s(buffer, sizeof(buffer), _TRUNCATE, fmt, value);
    if (len > 0) {
        OutputDebugStringA(buffer);
    }
}

bool RenderEventWriter::Init() {
    if (s_initialized) {
        return true;
    }

    InitializeCriticalSection(&s_lock);
    s_frameCount = 0;
    s_initialized = true;

    OutputDebugStringA("[tp_opengl_hook] RenderEventWriter::Init\n");
    return true;
}

void RenderEventWriter::OnSwap() {
    if (!s_initialized) {
        return;
    }

    EnterCriticalSection(&s_lock);

    s_frameCount++;
    LONG64 currentCount = s_frameCount;

    OutputDebugStringFormat("[tp_opengl_hook] SwapBuffers called, count=%lld\n", currentCount);

    LeaveCriticalSection(&s_lock);
}

void RenderEventWriter::Shutdown() {
    if (!s_initialized) {
        return;
    }

    OutputDebugStringFormat("[tp_opengl_hook] RenderEventWriter::Shutdown, final count=%lld\n", s_frameCount);

    DeleteCriticalSection(&s_lock);
    s_initialized = false;
}