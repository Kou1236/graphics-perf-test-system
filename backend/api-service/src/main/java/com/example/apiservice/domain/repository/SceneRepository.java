package com.example.apiservice.domain.repository;

import com.example.apiservice.domain.entity.SceneEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SceneRepository extends JpaRepository<SceneEntity, Long> {

    List<SceneEntity> findByAppId(Long appId);
}