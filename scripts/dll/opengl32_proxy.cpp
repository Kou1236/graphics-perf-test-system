// opengl32_proxy.cpp
// 一个简单的 opengl32 代理 DLL：
//  - 在加载时打开日志文件
//  - 动态加载原始的 opengl32_org.dll
//  - 转发 wglCreateContext / wglMakeCurrent / wglDeleteContext / wglGetProcAddress
//  - hook wglSwapBuffers，在每一帧写一行日志，然后调用原始实现

#include <windows.h>
#include <wchar.h>

static HMODULE g_hThisModule    = nullptr;   // 本 DLL 句柄
static HMODULE g_hOriginalOGL   = nullptr;   // opengl32_org.dll 句柄
static HANDLE  g_hLogFile       = INVALID_HANDLE_VALUE;
static CRITICAL_SECTION g_LogLock;

// 简单的日志函数：写入同目录下的 opengl32_proxy.log
static void LogMessage(const wchar_t* msg)
{
    if (g_hLogFile == INVALID_HANDLE_VALUE)
        return;

    SYSTEMTIME st;
    GetLocalTime(&st);

    wchar_t buffer[512];
    int len = _snwprintf_s(
        buffer,
        _TRUNCATE,
        L"[%04u-%02u-%02u %02u:%02u:%02u.%03u] %s\r\n",
        st.wYear, st.wMonth, st.wDay,
        st.wHour, st.wMinute, st.wSecond, st.wMilliseconds,
        msg
    );
    if (len <= 0)
        return;

    int bytesToWrite = WideCharToMultiByte(CP_UTF8, 0,
                                           buffer, len,
                                           nullptr, 0,
                                           nullptr, nullptr);
    if (bytesToWrite <= 0)
        return;

    char utf8buf[1024];
    if (bytesToWrite > (int)sizeof(utf8buf))
        bytesToWrite = (int)sizeof(utf8buf);

    WideCharToMultiByte(CP_UTF8, 0,
                        buffer, len,
                        utf8buf, bytesToWrite,
                        nullptr, nullptr);

    EnterCriticalSection(&g_LogLock);
    DWORD written = 0;
    WriteFile(g_hLogFile, utf8buf, (DWORD)bytesToWrite, &written, nullptr);
    LeaveCriticalSection(&g_LogLock);
}

// 打开日志文件：与本 DLL 同目录，名为 opengl32_proxy.log
static void OpenLogFile()
{
    if (!g_hThisModule)
        return;

    wchar_t path[MAX_PATH];
    DWORD n = GetModuleFileNameW(g_hThisModule, path, MAX_PATH);
    if (n == 0 || n >= MAX_PATH)
        return;

    // 把文件名部分替换成 opengl32_proxy.log
    wchar_t* p = wcsrchr(path, L'\\');
    if (!p)
        return;
    ++p; // 指向文件名开始位置
    wcscpy_s(p, MAX_PATH - (p - path), L"opengl32_proxy.log");

    g_hLogFile = CreateFileW(
        path,
        FILE_APPEND_DATA,
        FILE_SHARE_READ,
        nullptr,
        OPEN_ALWAYS,
        FILE_ATTRIBUTE_NORMAL,
        nullptr
    );
}

// 关闭日志文件
static void CloseLogFile()
{
    if (g_hLogFile != INVALID_HANDLE_VALUE)
    {
        CloseHandle(g_hLogFile);
        g_hLogFile = INVALID_HANDLE_VALUE;
    }
}

// 加载原始的 opengl32_org.dll
static HMODULE LoadOriginalOpenGL()
{
    if (g_hOriginalOGL)
        return g_hOriginalOGL;

    if (!g_hThisModule)
        return nullptr;

    wchar_t path[MAX_PATH];
    DWORD n = GetModuleFileNameW(g_hThisModule, path, MAX_PATH);
    if (n == 0 || n >= MAX_PATH)
        return nullptr;

    // 把本 DLL 名替换成 opengl32_org.dll
    wchar_t* p = wcsrchr(path, L'\\');
    if (!p)
        return nullptr;
    ++p;
    wcscpy_s(p, MAX_PATH - (p - path), L"opengl32_org.dll");

    HMODULE h = LoadLibraryW(path);
    if (!h)
    {
        // 兜底：尝试从系统目录加载系统 opengl32.dll
        wchar_t sysDir[MAX_PATH];
        UINT m = GetSystemDirectoryW(sysDir, MAX_PATH);
        if (m == 0 || m >= MAX_PATH)
            return nullptr;

        if (sysDir[m - 1] != L'\\')
            wcscat_s(sysDir, L"\\");

        wcscat_s(sysDir, L"opengl32.dll");
        h = LoadLibraryW(sysDir);
        if (!h)
            return nullptr;
    }

    g_hOriginalOGL = h;
    LogMessage(L"Loaded original OpenGL DLL");
    return h;
}

// 根据函数名从原始 DLL 里取地址
static FARPROC GetOriginalProc(const char* name)
{
    HMODULE h = LoadOriginalOpenGL();
    if (!h)
        return nullptr;
    return GetProcAddress(h, name);
}

// 定义需要用到的原始函数类型
typedef HGLRC (WINAPI *PFNWGLCREATECONTEXTPROC)(HDC);
typedef BOOL  (WINAPI *PFNWGLMAKECURRENTPROC)(HDC, HGLRC);
typedef BOOL  (WINAPI *PFNWGLDELETECONTEXTPROC)(HGLRC);
typedef PROC  (WINAPI *PFNWGLGETPROCADDRESSPROC)(LPCSTR);
typedef BOOL  (WINAPI *PFNWGLSWAPBUFFERSPROC)(HDC);

// 导出函数区域
extern "C"
{

__declspec(dllexport)
HGLRC WINAPI wglCreateContext(HDC hdc)
{
    LogMessage(L"wglCreateContext called");
    static PFNWGLCREATECONTEXTPROC real = nullptr;
    if (!real)
        real = (PFNWGLCREATECONTEXTPROC)GetOriginalProc("wglCreateContext");
    if (!real)
        return nullptr;
    return real(hdc);
}

__declspec(dllexport)
BOOL WINAPI wglMakeCurrent(HDC hdc, HGLRC hglrc)
{
    LogMessage(L"wglMakeCurrent called");
    static PFNWGLMAKECURRENTPROC real = nullptr;
    if (!real)
        real = (PFNWGLMAKECURRENTPROC)GetOriginalProc("wglMakeCurrent");
    if (!real)
        return FALSE;
    return real(hdc, hglrc);
}

__declspec(dllexport)
BOOL WINAPI wglDeleteContext(HGLRC hglrc)
{
    LogMessage(L"wglDeleteContext called");
    static PFNWGLDELETECONTEXTPROC real = nullptr;
    if (!real)
        real = (PFNWGLDELETECONTEXTPROC)GetOriginalProc("wglDeleteContext");
    if (!real)
        return FALSE;
    return real(hglrc);
}

__declspec(dllexport)
PROC WINAPI wglGetProcAddress(LPCSTR lpszProc)
{
    // 这里也可以做日志，不过有些程序会频繁调用
    static PFNWGLGETPROCADDRESSPROC real = nullptr;
    if (!real)
        real = (PFNWGLGETPROCADDRESSPROC)GetOriginalProc("wglGetProcAddress");
    if (!real)
        return nullptr;
    return real(lpszProc);
}

__declspec(dllexport)
BOOL WINAPI wglSwapBuffers(HDC hdc)
{
    LogMessage(L"wglSwapBuffers called");
    static PFNWGLSWAPBUFFERSPROC real = nullptr;
    if (!real)
        real = (PFNWGLSWAPBUFFERSPROC)GetOriginalProc("wglSwapBuffers");
    if (!real)
        return FALSE;

    // 这里是你真正可以插 FPS 计数、截图、GPU 时间戳之类逻辑的地方

    return real(hdc);
}

} // extern "C"


// DllMain：初始化 / 清理
BOOL WINAPI DllMain(HINSTANCE hinstDLL, DWORD fdwReason, LPVOID lpReserved)
{
    switch (fdwReason)
    {
    case DLL_PROCESS_ATTACH:
        g_hThisModule = (HMODULE)hinstDLL;
        InitializeCriticalSection(&g_LogLock);
        OpenLogFile();
        LogMessage(L"opengl32_proxy loaded");
        break;

    case DLL_PROCESS_DETACH:
        LogMessage(L"opengl32_proxy detached");
        CloseLogFile();
        if (g_hOriginalOGL)
        {
            FreeLibrary(g_hOriginalOGL);
            g_hOriginalOGL = nullptr;
        }
        DeleteCriticalSection(&g_LogLock);
        break;

    default:
        break;
    }
    return TRUE;
}