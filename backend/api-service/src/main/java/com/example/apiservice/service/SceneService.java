package com.example.apiservice.service;

import com.example.apiservice.domain.entity.SceneEntity;

import java.util.List;

public interface SceneService {

    SceneEntity create(SceneEntity scene);

    List<SceneEntity> listAll();

    SceneEntity getById(Long id);
}