package com.example.apiservice.execution;

import com.example.apiservice.domain.entity.ApplicationEntity;
import com.example.apiservice.domain.entity.RunEntity;
import com.example.apiservice.domain.entity.SceneEntity;
import com.example.apiservice.domain.repository.ApplicationRepository;
import com.example.apiservice.domain.repository.RunRepository;
import com.example.apiservice.domain.repository.SceneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class RunOrchestratorImpl implements RunOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RunOrchestratorImpl.class);

    private final RunRepository runRepository;
    private final SceneRepository sceneRepository;
    private final ApplicationRepository applicationRepository;
    private final ProcessLauncher processLauncher;

    public RunOrchestratorImpl(RunRepository runRepository,
                               SceneRepository sceneRepository,
                               ApplicationRepository applicationRepository,
                               ProcessLauncher processLauncher) {
        this.runRepository = runRepository;
        this.sceneRepository = sceneRepository;
        this.applicationRepository = applicationRepository;
        this.processLauncher = processLauncher;
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

            ProcessStartRequest request = buildProcessStartRequest(app);

            run.setStatus("Running");
            run.setStartAt(Instant.now());
            runRepository.save(run);

            RunExecutionResult result = processLauncher.launchAndWait(
                    request,
                    runId,
                    scene.getSampleIntervalMs()
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