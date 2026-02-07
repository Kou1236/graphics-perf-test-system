// opengl_demo.cpp
// Minimal Win32 + OpenGL demo for GPU load and diagnostics.
// Builds with MSVC (cl) and uses only system OpenGL (opengl32.lib).

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <GL/gl.h>
#include <cstdio>
#include <cmath>

#pragma comment(lib, "opengl32.lib")
#pragma comment(lib, "gdi32.lib")
#pragma comment(lib, "user32.lib")

static HGLRC g_hglrc = nullptr;
static HDC   g_hdc   = nullptr;
static bool  g_running = true;

LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam);

bool CreateGLContext(HWND hwnd, HGLRC& outContext, HDC& outDC)
{
    HDC hdc = GetDC(hwnd);
    if (!hdc) return false;

    PIXELFORMATDESCRIPTOR pfd = {};
    pfd.nSize      = sizeof(PIXELFORMATDESCRIPTOR);
    pfd.nVersion   = 1;
    pfd.dwFlags    = PFD_DRAW_TO_WINDOW | PFD_SUPPORT_OPENGL | PFD_DOUBLEBUFFER;
    pfd.iPixelType = PFD_TYPE_RGBA;
    pfd.cColorBits = 24;
    pfd.cDepthBits = 24;
    pfd.iLayerType = PFD_MAIN_PLANE;

    int pixelFormat = ChoosePixelFormat(hdc, &pfd);
    if (pixelFormat == 0) {
        ReleaseDC(hwnd, hdc);
        return false;
    }

    if (!SetPixelFormat(hdc, pixelFormat, &pfd)) {
        ReleaseDC(hwnd, hdc);
        return false;
    }

    HGLRC hglrc = wglCreateContext(hdc);
    if (!hglrc) {
        ReleaseDC(hwnd, hdc);
        return false;
    }

    if (!wglMakeCurrent(hdc, hglrc)) {
        wglDeleteContext(hglrc);
        ReleaseDC(hwnd, hdc);
        return false;
    }

    outContext = hglrc;
    outDC = hdc;
    return true;
}

void RenderFrame(float angleDegrees, HDC hdc)
{
    RECT rect;
    GetClientRect(WindowFromDC(hdc), &rect);
    int width  = rect.right - rect.left;
    int height = rect.bottom - rect.top;
    if (height == 0) height = 1;

    glViewport(0, 0, width, height);

    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
    float aspect = static_cast<float>(width) / static_cast<float>(height);
    if (aspect >= 1.0f) {
        glOrtho(-aspect, aspect, -1.0, 1.0, -1.0, 1.0);
    } else {
        glOrtho(-1.0, 1.0, -1.0f / aspect, 1.0f / aspect, -1.0, 1.0);
    }

    glMatrixMode(GL_MODELVIEW);
    glLoadIdentity();

    glClearColor(0.1f, 0.1f, 0.15f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    glDisable(GL_DEPTH_TEST);

    glPushMatrix();
    glRotatef(angleDegrees, 0.0f, 0.0f, 1.0f);

    glBegin(GL_TRIANGLES);
    glColor3f(1.0f, 0.3f, 0.3f);
    glVertex2f(0.0f, 0.6f);

    glColor3f(0.3f, 1.0f, 0.3f);
    glVertex2f(-0.6f, -0.6f);

    glColor3f(0.3f, 0.3f, 1.0f);
    glVertex2f(0.6f, -0.6f);
    glEnd();

    glPopMatrix();

    SwapBuffers(hdc);
}

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE, LPSTR, int nCmdShow)
{
    const wchar_t CLASS_NAME[] = L"OpenGLDemoWindowClass";

    WNDCLASSEXW wc = {};
    wc.cbSize        = sizeof(WNDCLASSEXW);
    wc.style         = CS_OWNDC;
    wc.lpfnWndProc   = WndProc;
    wc.hInstance     = hInstance;
    wc.hCursor       = LoadCursor(nullptr, IDC_ARROW);
    wc.lpszClassName = CLASS_NAME;

    if (!RegisterClassExW(&wc)) {
        MessageBoxA(nullptr, "Failed to register window class", "Error", MB_ICONERROR);
        return -1;
    }

    DWORD style = WS_OVERLAPPEDWINDOW & ~WS_MAXIMIZEBOX & ~WS_THICKFRAME;

    RECT rect = { 0, 0, 800, 600 };
    AdjustWindowRect(&rect, style, FALSE);

    HWND hwnd = CreateWindowExW(
        0,
        CLASS_NAME,
        L"OpenGL GPU Test Demo",
        style,
        CW_USEDEFAULT, CW_USEDEFAULT,
        rect.right - rect.left,
        rect.bottom - rect.top,
        nullptr,
        nullptr,
        hInstance,
        nullptr
    );

    if (!hwnd) {
        MessageBoxA(nullptr, "Failed to create window", "Error", MB_ICONERROR);
        return -1;
    }

    ShowWindow(hwnd, nCmdShow);
    UpdateWindow(hwnd);

    if (!CreateGLContext(hwnd, g_hglrc, g_hdc)) {
        MessageBoxA(nullptr, "Failed to create OpenGL context", "Error", MB_ICONERROR);
        DestroyWindow(hwnd);
        return -1;
    }

    // Diagnostics: print GL vendor/renderer/version once
    {
        const char* vendor   = reinterpret_cast<const char*>(glGetString(GL_VENDOR));
        const char* renderer = reinterpret_cast<const char*>(glGetString(GL_RENDERER));
        const char* version  = reinterpret_cast<const char*>(glGetString(GL_VERSION));

        char buf[512];
        std::snprintf(
            buf,
            sizeof(buf),
            "OpenGL diagnostics:\nVendor   : %s\nRenderer : %s\nVersion  : %s",
            vendor   ? vendor   : "(null)",
            renderer ? renderer : "(null)",
            version  ? version  : "(null)"
        );

        MessageBoxA(hwnd, buf, "OpenGL Info", MB_OK | MB_ICONINFORMATION);
    }

    MSG msg;
    ZeroMemory(&msg, sizeof(msg));

    DWORD startTick = GetTickCount();
    g_running = true;

    while (g_running) {
        while (PeekMessage(&msg, nullptr, 0, 0, PM_REMOVE)) {
            if (msg.message == WM_QUIT) {
                g_running = false;
                break;
            }
            TranslateMessage(&msg);
            DispatchMessage(&msg);
        }
        if (!g_running) {
            break;
        }

        DWORD now = GetTickCount();
        float elapsedSec = (now - startTick) / 1000.0f;
        float angle = std::fmod(elapsedSec * 60.0f, 360.0f);

        RenderFrame(angle, g_hdc);
        // Optional: Sleep(1); if you want to reduce load slightly
    }

    if (g_hglrc) {
        wglMakeCurrent(nullptr, nullptr);
        wglDeleteContext(g_hglrc);
        g_hglrc = nullptr;
    }
    if (g_hdc) {
        ReleaseDC(hwnd, g_hdc);
        g_hdc = nullptr;
    }

    DestroyWindow(hwnd);
    UnregisterClassW(CLASS_NAME, hInstance);

    return 0;
}

LRESULT CALLBACK WndProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    switch (msg) {
    case WM_CLOSE:
        g_running = false;
        PostQuitMessage(0);
        return 0;
    case WM_DESTROY:
        g_running = false;
        PostQuitMessage(0);
        return 0;
    case WM_KEYDOWN:
        if (wParam == VK_ESCAPE) {
            g_running = false;
            PostQuitMessage(0);
            return 0;
        }
        break;
    default:
        break;
    }

    return DefWindowProc(hwnd, msg, wParam, lParam);
}
