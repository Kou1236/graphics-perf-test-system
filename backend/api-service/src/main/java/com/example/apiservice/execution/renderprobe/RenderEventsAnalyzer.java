package com.example.apiservice.execution.renderprobe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class RenderEventsAnalyzer {

    private final ObjectMapper objectMapper;

    public RenderEventsAnalyzer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RenderSummary analyzeJsonl(Path jsonlPath) {
        RenderSummary summary = new RenderSummary();
        summary.setRenderEventsPath(jsonlPath.toAbsolutePath().toString());

        if (jsonlPath == null || !Files.exists(jsonlPath)) {
            summary.setError("render_events.jsonl not found");
            return summary;
        }

        long n = 0;
        long t0 = -1;
        long t1 = -1;

        try (BufferedReader br = Files.newBufferedReader(jsonlPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(line);
                if (!node.hasNonNull("ts")) {
                    continue;
                }
                String eventType = node.hasNonNull("eventType") ? node.get("eventType").asText() : null;
                if (eventType != null && !"SWAP".equalsIgnoreCase(eventType)) {
                    continue;
                }
                long ts = node.get("ts").asLong();
                if (t0 < 0) {
                    t0 = ts;
                }
                t1 = ts;
                n++;
            }
        } catch (Exception e) {
            summary.setError("failed to parse jsonl: " + e.getMessage());
            return summary;
        }

        summary.setSwapEventCount(n);

        if (n < 2 || t0 < 0 || t1 < 0 || t1 <= t0) {
            summary.setError("insufficient events to compute swapsPerSecAvg");
            return summary;
        }

        double durationSec = (t1 - t0) / 1000.0;
        double swapsPerSecAvg = (n - 1) / durationSec;

        summary.setDurationSec(durationSec);
        summary.setSwapsPerSecAvg(swapsPerSecAvg);
        return summary;
    }
}