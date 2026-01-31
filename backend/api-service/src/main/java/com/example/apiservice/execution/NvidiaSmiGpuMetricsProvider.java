package com.example.apiservice.execution;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 通过 nvidia-smi 命令获取 GPU 指标：
 * - utilization.gpu      → gpuUtilPct
 * - memory.used (MiB)   → gpuMemUsedBytes（字节）
 */
@Component
public class NvidiaSmiGpuMetricsProvider implements GpuMetricsProvider {

    private static final Logger log = LoggerFactory.getLogger(NvidiaSmiGpuMetricsProvider.class);

    @Override
    public GpuMetricsSnapshot snapshot() {
        Process process = null;
        try {
            String[] cmd = new String[]{
                    "nvidia-smi",
                    "--query-gpu=utilization.gpu,memory.used",
                    "--format=csv,noheader,nounits"
            };

            process = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line = reader.readLine();
                if (line == null || line.isBlank()) {
                    return null;
                }

                // 形如："12, 345"
                String[] parts = line.split(",");
                if (parts.length < 2) {
                    return null;
                }

                String utilStr = parts[0].trim();
                String memMiBStr = parts[1].trim();

                Double gpuUtilPct = parseDoubleSafe(utilStr);
                Long memMiB = parseLongSafe(memMiBStr);
                Long gpuMemUsedBytes = (memMiB != null ? memMiB * 1024L * 1024L : null);

                GpuMetricsSnapshot snapshot = new GpuMetricsSnapshot();
                snapshot.setGpuUtilPct(gpuUtilPct);
                snapshot.setGpuMemUsedBytes(gpuMemUsedBytes);
                return snapshot;
            }
        } catch (IOException e) {
            log.debug("Failed to run nvidia-smi for gpu metrics: {}", e.getMessage());
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private Double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLongSafe(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
