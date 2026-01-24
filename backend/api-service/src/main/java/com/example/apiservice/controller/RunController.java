package com.example.apiservice.controller;

import com.example.apiservice.domain.entity.RunEntity;
import com.example.apiservice.execution.RunOrchestrator;
import com.example.apiservice.service.RunService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/runs")
public class RunController {

    private final RunService runService;
    private final RunOrchestrator runOrchestrator;

    public RunController(RunService runService,
                         RunOrchestrator runOrchestrator) {
        this.runService = runService;
        this.runOrchestrator = runOrchestrator;
    }

    @PostMapping
    public RunEntity createAndStart(@RequestParam("sceneId") Long sceneId) {
        RunEntity run = runService.createPendingRun(sceneId);
        runOrchestrator.startRunAsync(run.getId());
        return run;
    }

    @GetMapping("/{id}")
    public RunEntity getById(@PathVariable Long id) {
        return runService.getById(id);
    }
}