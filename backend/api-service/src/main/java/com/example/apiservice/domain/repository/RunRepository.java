package com.example.apiservice.domain.repository;

import com.example.apiservice.domain.entity.RunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunRepository extends JpaRepository<RunEntity, Long> {
}