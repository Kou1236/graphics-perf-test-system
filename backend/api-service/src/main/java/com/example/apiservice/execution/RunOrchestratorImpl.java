package com.example.apiservice.execution;

import com.example.apiservice.domain.entity.ApplicationEntity;
import com.example.apiservice.domain.entity.RunEntity;
import com.example.apiservice.domain.entity.SceneEntity;
import com.example.apiservice.domain.repository.ApplicationRepository;
import com.example.apiservice.domain.repository.RunRepository;
import com.example.apiservice.domain.repository.SceneRepository;
import com.example.apiservice.execution.renderprobe.OpenGlRenderProbeConfig;
import com.example.apiservice.execution.renderprobe.RenderProbePlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class RunOrchestratorImpl implements RunOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RunOrchestratorImpl.class);
    private static final String RUNS_BASE_DIR = "./data/runs";

    private final RunRepository runRepository;
    private final SceneRepository sceneRepository;
    private final ApplicationRepository applicationRepository;
    private final ProcessLauncher processLauncher;
    private final ObjectMapper objectMapper;

    public RunOrchestratorImpl(RunRepository runRepository,
                               SceneRepository sceneRepository,
                               ApplicationRepository applicationRepository,
                               ProcessLauncher processLauncher,
                               ObjectMapper objectMapper) {
        this.runRepository = runRepository;
        this.sceneRepository = sceneRepository;
        this.applicationRepository = applicationRepository;
        this.processLauncher = processLauncher;
        this.objectMapper = objectMapper;
    }

    @Async
    @Override
    public void startRunAsync(Long runId) {
        log.info("Submitting run {} to async executor", runId);
        doRun(runId);
    }

    private void doRun(Long runId) {
        RunEntity run = runRepository.findById(runId).orElse(null);
        if (run == null) {
            log.warn("Run {} not found, aborting", runId);
            return;
        }

        try {
            SceneEntity scene = sceneRepository.findById(run.getSceneId())
                    .orElseThrow(() -> new IllegalArgumentException("Scene not found: " + run.getSceneId()));
            ApplicationEntity app = applicationRepository.findById(scene.getAppId())
                    .orElseThrow(() -> new IllegalArgumentException("Application not found: " + scene.getAppId()));

            Path artifactDir = Path.of(RUNS_BASE_DIR, Long.toString(runId));
            Files.createDirectories(artifactDir);
            String renderEventsPath = artifactDir.resolve("render_events.jsonl").toAbsolutePath().toString();

            run.setArtifactDir(artifactDir.toAbsolutePath().toString());
            run.setRenderEventsPath(renderEventsPath);

            RenderProbePlan renderProbePlan = buildRenderProbePlan(scene, renderEventsPath);
            if (renderProbePlan != null && renderProbePlan.isEnabled()) {
                run.setRenderProbeStatus("ENABLED");
                run.setRenderProbeError(null);
            } else {
                run.setRenderProbeStatus("DISABLED");
            }

            ProcessStartRequest request = buildProcessStartRequest(app);

            run.setStatus("Running");
            run.setStartAt(Instant.now());
            runRepository.save(run);

            RunExecutionResult result = processLauncher.launchAndWait(
                    request,
                    runId,
                    scene.getSampleIntervalMs(),
                    renderProbePlan
            );

            if (result.getPid() != null) {
                run.setPid(result.getPid());
            }
            if (result.getStartAt() != null) {
                run.setStartAt(result.getStartAt());
            }
            run.setEndAt(result.getEndAt());
            run.setExitCode(result.getExitCode());

            if (result.getExitCode() == 0) {
                run.setStatus("Completed");
            } else {
                run.setStatus("Failed");
                if (result.getErrorMessage() != null && !result.getErrorMessage().isBlank()) {
                    run.setErrorMessage(result.getErrorMessage());
                } else {
                    run.setErrorMessage("Process exited with code " + result.getExitCode());
                }
            }

            runRepository.save(run);
            log.info("Run {} finished with status={}, exitCode={}", runId, run.getStatus(), run.getExitCode());
        } catch (Exception e) {
            log.error("Run {} execution failed", runId, e);
            try {
                run.setStatus("Failed");
                run.setExitCode(-1);
                run.setEndAt(Instant.now());
                run.setErrorMessage("Run execution failed: " + e.getMessage());
                runRepository.save(run);
            } catch (Exception ex) {
                log.error("Failed to persist failed status for run {}", runId, ex);
            }
        }
    }

    private RenderProbePlan buildRenderProbePlan(SceneEntity scene, String renderEventsPath) {
        if (scene == null) {
            return null;
        }
        String json = scene.getRenderProbeConfigJson();
        if (json == null || json.isBlank()) {
            return null;
        }

        OpenGlRenderProbeConfig cfg;
        try {
            cfg = objectMapper.readValue(json, OpenGlRenderProbeConfig.class);
        } catch (Exception e) {
            return null;
        }

        if (!cfg.isEnabled()) {
            return null;
        }

        RenderProbePlan plan = new RenderProbePlan();
        plan.setEnabled(true);
        plan.setInjectorExePath(cfg.getInjectorExePath());
        plan.setHookDllPath(cfg.getHookDllPath());
        plan.setRenderEventsPath(renderEventsPath);
        return plan;
    }

    private ProcessStartRequest buildProcessStartRequest(ApplicationEntity app) {
        ProcessStartRequest request = new ProcessStartRequest();
        request.setExecutablePath(app.getExePath());

        List<String> arguments = new ArrayList<>();
        if (app.getDefaultArgs() != null && !app.getDefaultArgs().isBlank()) {
            String[] parts = app.getDefaultArgs().trim().split("\\s+");
            arguments.addAll(Arrays.asList(parts));
        }
        request.setArguments(arguments);

        request.setWorkingDirectory(app.getWorkDir());
        request.setEnvironment(null);

        return request;
    }
}