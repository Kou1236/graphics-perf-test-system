package com.example.apiservice.execution.renderprobe;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Component
public class ProbeConfigFileWriter {

    private final ObjectMapper objectMapper;

    public ProbeConfigFileWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Path writeForPid(long pid, String renderEventsPath) {
        if (pid <= 0) {
            throw new IllegalArgumentException("pid must be positive");
        }
        if (renderEventsPath == null || renderEventsPath.isBlank()) {
            throw new IllegalArgumentException("renderEventsPath is blank");
        }

        String tmp = System.getProperty("java.io.tmpdir");
        Path cfgPath = Path.of(tmp, "tp_probe_config_" + pid + ".json");

        try {
            Files.createDirectories(cfgPath.getParent());
            String json = objectMapper.writeValueAsString(Map.of(
                    "renderEventsPath", renderEventsPath,
                    "format", "jsonl",
                    "eventType", "SWAP",
                    "timebase", "qpc_ms"
            ));
            Files.writeString(cfgPath, json);
            return cfgPath;
        } catch (Exception e) {
            throw new RuntimeException("Failed to write probe config: " + cfgPath + ", err=" + e.getMessage(), e);
        }
    }
}