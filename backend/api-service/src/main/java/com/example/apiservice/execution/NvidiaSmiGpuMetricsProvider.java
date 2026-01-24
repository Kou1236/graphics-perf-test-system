package com.example.apiservice.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 通过 nvidia-smi 命令获取 GPU 利用率
 */
@Component
public class NvidiaSmiGpuMetricsProvider implements GpuMetricsProvider {

    private static final Logger log = LoggerFactory.getLogger(NvidiaSmiGpuMetricsProvider.class);

    @Override
    public GpuMetricsSnapshot snapshot() {
        ProcessBuilder builder = new ProcessBuilder(
                "nvidia-smi",
                "--query-gpu=utilization.gpu",
                "--format=csv,noheader,nounits"
        );

        try {
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    log.debug("nvidia-smi exited with code {}, skip gpu metrics", exitCode);
                    return null;
                }

                if (line == null || line.isBlank()) {
                    log.debug("nvidia-smi output is empty, skip gpu metrics");
                    return null;
                }

                String trimmed = line.trim();
                String numeric = trimmed.replaceAll("[^0-9.]", "");
                if (numeric.isEmpty()) {
                    log.debug("nvidia-smi output '{}' has no numeric gpu util", trimmed);
                    return null;
                }

                double util = Double.parseDouble(numeric);

                GpuMetricsSnapshot snapshot = new GpuMetricsSnapshot();
                snapshot.setGpuUtilPct(util);
                return snapshot;
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.debug("Failed to run nvidia-smi for gpu metrics: {}", e.getMessage());
            return null;
        }
    }
}