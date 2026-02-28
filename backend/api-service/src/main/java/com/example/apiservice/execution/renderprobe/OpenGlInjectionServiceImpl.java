package com.example.apiservice.execution.renderprobe;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenGlInjectionServiceImpl implements OpenGlInjectionService {

    @Override
    public void injectOrThrow(long pid, String injectorExePath, String hookDllPath) {
        if (pid <= 0) {
            throw new IllegalArgumentException("pid must be positive");
        }
        if (injectorExePath == null || injectorExePath.isBlank()) {
            throw new IllegalArgumentException("injectorExePath is blank");
        }
        if (hookDllPath == null || hookDllPath.isBlank()) {
            throw new IllegalArgumentException("hookDllPath is blank");
        }

        Path injectorPath = Path.of(injectorExePath);
        Path dllPath = Path.of(hookDllPath);

        if (!Files.exists(injectorPath)) {
            throw new IllegalArgumentException("injector.exe not found: " + injectorExePath);
        }
        if (!Files.exists(dllPath)) {
            throw new IllegalArgumentException("hook dll not found: " + hookDllPath);
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(injectorPath.toAbsolutePath().toString());
        cmd.add(Long.toString(pid));
        cmd.add(dllPath.toAbsolutePath().toString());

        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            int exit = p.waitFor();
            if (exit != 0) {
                throw new RuntimeException("injector.exe exit code=" + exit);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("injector.exe interrupted", e);
        } catch (Exception e) {
            throw new RuntimeException("injector.exe failed: " + e.getMessage(), e);
        }
    }
}