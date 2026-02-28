package com.example.apiservice.execution.renderprobe;

public interface OpenGlInjectionService {

    void injectOrThrow(long pid, String injectorExePath, String hookDllPath);
}