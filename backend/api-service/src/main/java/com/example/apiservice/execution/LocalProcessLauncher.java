package com.example.apiservice.execution;

import com.example.apiservice.domain.entity.MetricSampleEntity;
import com.example.apiservice.domain.repository.MetricSampleRepository;
import com.example.apiservice.execution.renderprobe.OpenGlInjectionService;
import com.example.apiservice.execution.renderprobe.ProbeConfigFileWriter;
import com.example.apiservice.execution.renderprobe.RenderProbePlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本地进程启动器：
 * - 启动被测进程
 * - 在进程存活期间周期性采样
 * - 可选：OpenGL RenderProbe 注入与事件落盘
 */
@Component
public class LocalProcessLauncher implements ProcessLauncher {

    private static final Logger log = LoggerFactory.getLogger(LocalProcessLauncher.class);

    private static final String RUNS_BASE_DIR = "./data/runs";

    private final MetricSampleRepository metricSampleRepository;
    private final CpuMemProvider cpuMemProvider;
    private final ProcessMetricsProvider processMetricsProvider;
    private final GpuMetricsProvider gpuMetricsProvider;

    @SuppressWarnings("unused")
    private final ObjectMapper objectMapper;

    private final ProbeConfigFileWriter probeConfigFileWriter;
    private final OpenGlInjectionService openGlInjectionService;

    public LocalProcessLauncher(MetricSampleRepository metricSampleRepository,
                                CpuMemProvider cpuMemProvider,
                                ProcessMetricsProvider processMetricsProvider,
                                GpuMetricsProvider gpuMetricsProvider,
                                ObjectMapper objectMapper,
                                ProbeConfigFileWriter probeConfigFileWriter,
                                OpenGlInjectionService openGlInjectionService) {
        this.metricSampleRepository = metricSampleRepository;
        this.cpuMemProvider = cpuMemProvider;
        this.processMetricsProvider = processMetricsProvider;
        this.gpuMetricsProvider = gpuMetricsProvider;
        this.objectMapper = objectMapper;
        this.probeConfigFileWriter = probeConfigFileWriter;
        this.openGlInjectionService = openGlInjectionService;
    }

    @Override
    public RunExecutionResult launchAndWait(ProcessStartRequest request, Long runId, long sampleIntervalMs) {
        return launchAndWait(request, runId, sampleIntervalMs, null);
    }

    @Override
    public RunExecutionResult launchAndWait(ProcessStartRequest request,
                                            Long runId,
                                            long sampleIntervalMs,
                                            RenderProbePlan renderProbePlan) {
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

        Path runDir = Path.of(RUNS_BASE_DIR, Long.toString(runId));
        try {
            Files.createDirectories(runDir);
        } catch (Exception e) {
            log.warn("Failed to create run dir {}: {}", runDir, e.getMessage());
        }

        try {
            log.info("Starting process for run {}: {}", runId, String.join(" ", command));
            Instant startAt = Instant.now();
            Process process = builder.start();
            long pid = process.pid();

            result.setPid(pid);
            result.setStartAt(startAt);

            if (renderProbePlan != null && renderProbePlan.isEnabled()) {
                try {
                    probeConfigFileWriter.writeForPid(pid, renderProbePlan.getRenderEventsPath());
                    openGlInjectionService.injectOrThrow(pid, renderProbePlan.getInjectorExePath(), renderProbePlan.getHookDllPath());
                    log.info("RenderProbe injected: runId={}, pid={}, events={}", runId, pid, renderProbePlan.getRenderEventsPath());
                } catch (Exception e) {
                    log.warn("RenderProbe inject failed: runId={}, pid={}, err={}", runId, pid, e.getMessage());
                }
            }

            while (process.isAlive()) {
                try {
                    CpuMemSnapshot systemSnapshot = cpuMemProvider.snapshotForPid(pid);
                    ProcessMetricsSnapshot processSnapshot = processMetricsProvider.snapshotForPid(pid);
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