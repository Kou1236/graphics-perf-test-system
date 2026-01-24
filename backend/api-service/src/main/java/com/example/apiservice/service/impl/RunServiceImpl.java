package com.example.apiservice.service.impl;

import com.example.apiservice.domain.entity.RunEntity;
import com.example.apiservice.domain.repository.RunRepository;
import com.example.apiservice.domain.repository.SceneRepository;
import com.example.apiservice.service.RunService;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RunServiceImpl implements RunService {

    private final RunRepository runRepository;
    private final SceneRepository sceneRepository;

    public RunServiceImpl(RunRepository runRepository,
                          SceneRepository sceneRepository) {
        this.runRepository = runRepository;
        this.sceneRepository = sceneRepository;
    }

    @Override
    public RunEntity createPendingRun(Long sceneId) {
        sceneRepository.findById(sceneId)
                .orElseThrow(() -> new IllegalArgumentException("Scene not found: " + sceneId));

        RunEntity run = new RunEntity();
        run.setSceneId(sceneId);
        run.setStatus("Pending");
        run.setCreatedAt(Instant.now());
        return runRepository.save(run);
    }

    @Override
    public RunEntity getById(Long id) {
        return runRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + id));
    }
}