package com.example.apiservice.service;

import com.example.apiservice.domain.entity.RunEntity;

public interface RunService {

    RunEntity createPendingRun(Long sceneId);

    RunEntity getById(Long id);
}