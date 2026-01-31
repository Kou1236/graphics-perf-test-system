package com.example.apiservice.execution;

import com.example.apiservice.domain.entity.MetricSampleEntity;
import com.example.apiservice.domain.repository.MetricSampleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本地进程启动器：
 * - 启动被测进程
 * - 在进程存活期间周期性采样
 * - 指标包含：
 *   - 进程级：cpuPct / memBytes
 *   - 系统级：systemCpuPct / systemMemBytes
 *   - GPU：gpuUtilPct / gpuMemUsedBytes
 */
@Component
public class LocalProcessLauncher implements ProcessLauncher {

    private static final Logger log = LoggerFactory.getLogger(LocalProcessLauncher.class);

    private final MetricSampleRepository metricSampleRepository;
    private final CpuMemProvider cpuMemProvider;
    private final ProcessMetricsProvider processMetricsProvider;
    private final GpuMetricsProvider gpuMetricsProvider;

    // 保留 ObjectMapper 依赖以兼容现有构造注入（当前未使用）
    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    public LocalProcessLauncher(MetricSampleRepository metricSampleRepository,
                                CpuMemProvider cpuMemProvider,
                                ProcessMetricsProvider processMetricsProvider,
                                GpuMetricsProvider gpuMetricsProvider,
                                ObjectMapper objectMapper) {
        this.metricSampleRepository = metricSampleRepository;
        this.cpuMemProvider = cpuMemProvider;
        this.processMetricsProvider = processMetricsProvider;
        this.gpuMetricsProvider = gpuMetricsProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public RunExecutionResult launchAndWait(ProcessStartRequest request, Long runId, long sampleIntervalMs) {
        RunExecutionResult result = new RunExecutionResult();

        if (sampleIntervalMs <= 0) {
            sampleIntervalMs = 1000L;
        }

        List<String> command = new ArrayList<>();
        command.add(request.getExecutablePath());
        if (request.getArguments() != null && !request.getArguments().isEmpty()) {
            command.addAll(request.getArguments());
        }

        ProcessBuilder builder = new ProcessBuilder(command);

        if (request.getWorkingDirectory() != null && !request.getWorkingDirectory().isBlank()) {
            builder.directory(new File(request.getWorkingDirectory()));
        }

        Map<String, String> env = request.getEnvironment();
        if (env != null && !env.isEmpty()) {
            builder.environment().putAll(env);
        }

        try {
            log.info("Starting process for run {}: {}", runId, String.join(" ", command));
            Instant startAt = Instant.now();
            Process process = builder.start();
            long pid = process.pid();

            result.setPid(pid);
            result.setStartAt(startAt);

            while (process.isAlive()) {
                try {
                    // 1. 系统级 CPU / MEM
                    CpuMemSnapshot systemSnapshot = cpuMemProvider.snapshotForPid(pid);

                    // 2. 进程级 CPU / MEM（基于 OSHI）
                    ProcessMetricsSnapshot processSnapshot = processMetricsProvider.snapshotForPid(pid);

                    // 3. GPU 指标（NVIDIA）
                    GpuMetricsSnapshot gpuSnapshot = gpuMetricsProvider.snapshot();

                    Double cpuPct = null;
                    Long memBytes = null;
                    Double systemCpuPct = null;
                    Long systemMemBytes = null;
                    Double gpuUtilPct = null;
                    Long gpuMemUsedBytes = null;

                    if (processSnapshot != null) {
                        cpuPct = processSnapshot.getCpuPct();
                        memBytes = processSnapshot.getMemBytes();
                    }

                    if (systemSnapshot != null) {
                        systemCpuPct = systemSnapshot.getCpuPct();
                        systemMemBytes = systemSnapshot.getMemBytes();
                    }

                    if (gpuSnapshot != null) {
                        gpuUtilPct = gpuSnapshot.getGpuUtilPct();
                        gpuMemUsedBytes = gpuSnapshot.getGpuMemUsedBytes();
                    }

                    // 如果进程级指标为空，则退回系统级
                    if (cpuPct == null && systemCpuPct != null) {
                        cpuPct = systemCpuPct;
                    }
                    if (memBytes == null && systemMemBytes != null) {
                        memBytes = systemMemBytes;
                    }

                    MetricSampleEntity sample = new MetricSampleEntity();
                    sample.setRunId(runId);
                    sample.setTs(Instant.now());
                    sample.setCpuPct(cpuPct);
                    sample.setMemBytes(memBytes);
                    sample.setSystemCpuPct(systemCpuPct);
                    sample.setSystemMemBytes(systemMemBytes);
                    sample.setGpuUtilPct(gpuUtilPct);
                    sample.setGpuMemUsedBytes(gpuMemUsedBytes);
                    // 不再写入 extraJson，保持为 null

                    metricSampleRepository.save(sample);
                } catch (Exception e) {
                    log.warn("Failed to record metric sample for run {}: {}", runId, e.getMessage());
                }

                try {
                    Thread.sleep(sampleIntervalMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Sampling loop interrupted for run {}", runId);
                    break;
                }
            }

            int exitCode;
            try {
                exitCode = process.exitValue();
            } catch (IllegalThreadStateException e) {
                exitCode = process.waitFor();
            }
            Instant endAt = Instant.now();

            result.setEndAt(endAt);
            result.setExitCode(exitCode);

            if (exitCode != 0) {
                result.setErrorMessage("Process exited with code " + exitCode);
            }
        } catch (IOException e) {
            Instant now = Instant.now();
            result.setEndAt(now);
            result.setExitCode(-1);
            result.setErrorMessage("Failed to start process: " + e.getMessage());
            log.error("Failed to start process for run {}: {}", runId, request.getExecutablePath(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Instant now = Instant.now();
            result.setEndAt(now);
            result.setExitCode(-2);
            result.setErrorMessage("Process wait interrupted: " + e.getMessage());
            log.error("Process wait interrupted for run {}", runId, e);
        }

        return result;
    }
}
