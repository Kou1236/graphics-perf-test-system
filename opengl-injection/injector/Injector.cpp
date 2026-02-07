#include <windows.h>
#include <string>
#include <iostream>

static void PrintUsage() {
    std::wcout << L"Usage: injector.exe <pid> <full_path_to_dll>\n";
}

int wmain(int argc, wchar_t* argv[]) {
    if (argc != 3) {
        PrintUsage();
        return 1;
    }

    DWORD pid = _wtoi(argv[1]);
    if (pid == 0) {
        std::wcout << L"Invalid PID.\n";
        return 1;
    }

    std::wstring dllPath = argv[2];
    if (dllPath.empty()) {
        std::wcout << L"Invalid DLL path.\n";
        return 1;
    }

    HANDLE hProcess = OpenProcess(
        PROCESS_CREATE_THREAD | PROCESS_QUERY_INFORMATION | PROCESS_VM_OPERATION | PROCESS_VM_WRITE | PROCESS_VM_READ,
        FALSE,
        pid
    );

    if (!hProcess) {
        std::wcout << L"OpenProcess failed, error=" << GetLastError() << L"\n";
        return 1;
    }

    SIZE_T dllPathSizeBytes = (dllPath.size() + 1) * sizeof(wchar_t);

    LPVOID remoteMem = VirtualAllocEx(
        hProcess,
        nullptr,
        dllPathSizeBytes,
        MEM_COMMIT | MEM_RESERVE,
        PAGE_READWRITE
    );

    if (!remoteMem) {
        std::wcout << L"VirtualAllocEx failed, error=" << GetLastError() << L"\n";
        CloseHandle(hProcess);
        return 1;
    }

    BOOL okWrite = WriteProcessMemory(
        hProcess,
        remoteMem,
        dllPath.c_str(),
        dllPathSizeBytes,
        nullptr
    );

    if (!okWrite) {
        std::wcout << L"WriteProcessMemory failed, error=" << GetLastError() << L"\n";
        VirtualAllocEx(hProcess, remoteMem, 0, MEM_RELEASE, PAGE_NOACCESS);
        CloseHandle(hProcess);
        return 1;
    }

    HMODULE hKernel32 = GetModuleHandleW(L"kernel32.dll");
    if (!hKernel32) {
        std::wcout << L"GetModuleHandleW(kernel32.dll) failed, error=" << GetLastError() << L"\n";
        VirtualAllocEx(hProcess, remoteMem, 0, MEM_RELEASE, PAGE_NOACCESS);
        CloseHandle(hProcess);
        return 1;
    }

    LPTHREAD_START_ROUTINE pLoadLibraryW = reinterpret_cast<LPTHREAD_START_ROUTINE>(
        GetProcAddress(hKernel32, "LoadLibraryW")
    );

    if (!pLoadLibraryW) {
        std::wcout << L"GetProcAddress(LoadLibraryW) failed, error=" << GetLastError() << L"\n";
        VirtualAllocEx(hProcess, remoteMem, 0, MEM_RELEASE, PAGE_NOACCESS);
        CloseHandle(hProcess);
        return 1;
    }

    HANDLE hThread = CreateRemoteThread(
        hProcess,
        nullptr,
        0,
        pLoadLibraryW,
        remoteMem,
        0,
        nullptr
    );

    if (!hThread) {
        std::wcout << L"CreateRemoteThread failed, error=" << GetLastError() << L"\n";
        VirtualAllocEx(hProcess, remoteMem, 0, MEM_RELEASE, PAGE_NOACCESS);
        CloseHandle(hProcess);
        return 1;
    }

    std::wcout << L"CreateRemoteThread success, waiting for completion...\n";

    WaitForSingleObject(hThread, INFINITE);

    DWORD exitCode = 0;
    if (GetExitCodeThread(hThread, &exitCode)) {
        if (exitCode == 0) {
            std::wcout << L"LoadLibraryW failed in remote process.\n";
        } else {
            std::wcout << L"LoadLibraryW succeeded, remote module handle = 0x"
                       << std::hex << exitCode << std::dec << L"\n";
        }
    } else {
        std::wcout << L"GetExitCodeThread failed, error=" << GetLastError() << L"\n";
    }

    CloseHandle(hThread);

    VirtualAllocEx(hProcess, remoteMem, 0, MEM_RELEASE, PAGE_NOACCESS);
    CloseHandle(hProcess);

    std::wcout << L"Injection finished.\n";
    return 0;
}