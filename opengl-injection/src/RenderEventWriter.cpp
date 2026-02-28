#include "RenderEventWriter.h"

#include <windows.h>
#include <cstdint>
#include <fstream>
#include <sstream>
#include <string>
#include <filesystem>

bool RenderEventWriter::s_initialized = false;
LONG64 RenderEventWriter::s_swapCount = 0;

CRITICAL_SECTION RenderEventWriter::s_lock;
bool RenderEventWriter::s_lockInited = false;

HANDLE RenderEventWriter::s_file = INVALID_HANDLE_VALUE;
std::wstring RenderEventWriter::s_logPath;

static std::wstring Utf8ToWide(const std::string& s) {
    if (s.empty()) return L"";
    int len = MultiByteToWideChar(CP_UTF8, 0, s.c_str(), (int)s.size(), nullptr, 0);
    if (len <= 0) return L"";
    std::wstring ws((size_t)len, L'\0');
    MultiByteToWideChar(CP_UTF8, 0, s.c_str(), (int)s.size(), ws.data(), len);
    return ws;
}

static bool ExtractRenderEventsPathUtf8(const std::string& json, std::string& outPath) {
    const std::string key = "\"renderEventsPath\":\"";
    size_t p = json.find(key);
    if (p == std::string::npos) return false;
    p += key.size();

    size_t q = json.find('"', p);
    if (q == std::string::npos) return false;

    std::string raw = json.substr(p, q - p);

    std::string unescaped;
    unescaped.reserve(raw.size());
    for (size_t i = 0; i < raw.size(); i++) {
        if (raw[i] == '\\' && (i + 1) < raw.size() && raw[i + 1] == '\\') {
            unescaped.push_back('\\');
            i++;
        } else {
            unescaped.push_back(raw[i]);
        }
    }

    outPath = unescaped;
    return true;
}

std::wstring RenderEventWriter::GetTempDir() {
    wchar_t buf[MAX_PATH] = {0};
    DWORD n = GetTempPathW(MAX_PATH, buf);
    if (n == 0 || n >= MAX_PATH) return L"C:\\Windows\\Temp\\";
    return std::wstring(buf);
}

std::wstring RenderEventWriter::GetProbeConfigPathForCurrentPid() {
    DWORD pid = GetCurrentProcessId();
    std::wstringstream ss;
    ss << GetTempDir() << L"tp_probe_config_" << pid << L".json";
    return ss.str();
}

std::wstring RenderEventWriter::GetErrorPathForCurrentPid() {
    DWORD pid = GetCurrentProcessId();
    std::wstringstream ss;
    ss << GetTempDir() << L"tp_opengl_hook_error_" << pid << L".txt";
    return ss.str();
}

void RenderEventWriter::WriteErrorFile(const std::wstring& msg) {
    std::wofstream out(GetErrorPathForCurrentPid(), std::ios::out | std::ios::trunc);
    out << msg;
}

bool RenderEventWriter::LoadRenderEventsPathFromTempConfig(std::wstring& outPath) {
    std::wstring cfgPath = GetProbeConfigPathForCurrentPid();

    std::ifstream in(cfgPath);
    if (!in.good()) {
        WriteErrorFile(L"Failed to open probe config: " + cfgPath);
        return false;
    }

    std::stringstream buffer;
    buffer << in.rdbuf();
    std::string json = buffer.str();

    std::string pathUtf8;
    if (!ExtractRenderEventsPathUtf8(json, pathUtf8)) {
        WriteErrorFile(L"Failed to parse renderEventsPath from config: " + cfgPath);
        return false;
    }

    outPath = Utf8ToWide(pathUtf8);
    if (outPath.empty()) {
        WriteErrorFile(L"renderEventsPath utf8->wide failed");
        return false;
    }

    return true;
}

bool RenderEventWriter::OpenLogFile(const std::wstring& logPath) {
    try {
        std::filesystem::path p(logPath);
        auto parent = p.parent_path();
        if (!parent.empty()) {
            std::filesystem::create_directories(parent);
        }
    } catch (...) {
    }

    s_file = CreateFileW(
        logPath.c_str(),
        FILE_APPEND_DATA,
        FILE_SHARE_READ | FILE_SHARE_WRITE,
        nullptr,
        OPEN_ALWAYS,
        FILE_ATTRIBUTE_NORMAL,
        nullptr
    );

    if (s_file == INVALID_HANDLE_VALUE) {
        DWORD err = GetLastError();
        std::wstringstream ss;
        ss << L"CreateFileW failed. path=" << logPath << L", GetLastError=" << err;
        WriteErrorFile(ss.str());
        return false;
    }

    return true;
}

void RenderEventWriter::AppendLineUtf8(const char* line, size_t len) {
    if (s_file == INVALID_HANDLE_VALUE) return;
    DWORD written = 0;
    WriteFile(s_file, line, (DWORD)len, &written, nullptr);
}

uint64_t RenderEventWriter::NowMsQpc() {
    LARGE_INTEGER freq = {0}, counter = {0};
    if (!QueryPerformanceFrequency(&freq) || freq.QuadPart == 0) return 0;
    QueryPerformanceCounter(&counter);

    double ms = (double)counter.QuadPart * 1000.0 / (double)freq.QuadPart;
    if (ms < 0) ms = 0;
    return (uint64_t)ms;
}

bool RenderEventWriter::Init() {
    if (s_initialized) return true;

    if (!s_lockInited) {
        InitializeCriticalSection(&s_lock);
        s_lockInited = true;
    }

    s_swapCount = 0;
    s_file = INVALID_HANDLE_VALUE;
    s_logPath.clear();

    std::wstring logPath;
    if (!LoadRenderEventsPathFromTempConfig(logPath)) return false;
    if (!OpenLogFile(logPath)) return false;

    s_logPath = logPath;
    s_initialized = true;

    OutputDebugStringA("[tp_opengl_hook] RenderEventWriter::Init OK\n");

    const char* initLine = "{\"ts\":0,\"eventType\":\"INIT\"}\n";
    AppendLineUtf8(initLine, strlen(initLine));
    return true;
}

void RenderEventWriter::OnSwap() {
    if (!s_initialized) return;

    EnterCriticalSection(&s_lock);

    s_swapCount++;
    uint64_t ts = NowMsQpc();

    char buf[160] = {0};
    int n = _snprintf_s(
        buf, sizeof(buf), _TRUNCATE,
        "{\"ts\":%llu,\"eventType\":\"SWAP\"}\n",
        (unsigned long long)ts
    );

    if (n > 0) AppendLineUtf8(buf, (size_t)n);

    LeaveCriticalSection(&s_lock);
}

void RenderEventWriter::Shutdown() {
    if (!s_initialized) return;

    if (s_file != INVALID_HANDLE_VALUE) {
        CloseHandle(s_file);
        s_file = INVALID_HANDLE_VALUE;
    }

    s_initialized = false;

    if (s_lockInited) {
        DeleteCriticalSection(&s_lock);
        s_lockInited = false;
    }
}