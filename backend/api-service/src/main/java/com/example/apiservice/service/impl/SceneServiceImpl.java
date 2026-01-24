package com.example.apiservice.service.impl;

import com.example.apiservice.domain.entity.SceneEntity;
import com.example.apiservice.domain.repository.ApplicationRepository;
import com.example.apiservice.domain.repository.SceneRepository;
import com.example.apiservice.service.SceneService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SceneServiceImpl implements SceneService {

    private final SceneRepository sceneRepository;
    private final ApplicationRepository applicationRepository;

    public SceneServiceImpl(SceneRepository sceneRepository,
                            ApplicationRepository applicationRepository) {
        this.sceneRepository = sceneRepository;
        this.applicationRepository = applicationRepository;
    }

    @Override
    public SceneEntity create(SceneEntity scene) {
        applicationRepository.findById(scene.getAppId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application not found: " + scene.getAppId()));
        return sceneRepository.save(scene);
    }

    @Override
    public List<SceneEntity> listAll() {
        return sceneRepository.findAll();
    }

    @Override
    public SceneEntity getById(Long id) {
        return sceneRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Scene not found: " + id));
    }
}