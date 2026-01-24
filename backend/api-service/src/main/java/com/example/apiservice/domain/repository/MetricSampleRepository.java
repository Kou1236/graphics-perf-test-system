package com.example.apiservice.domain.repository;

import com.example.apiservice.domain.entity.MetricSampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricSampleRepository extends JpaRepository<MetricSampleEntity, Long> {

    List<MetricSampleEntity> findByRunIdOrderByTsAsc(Long runId);
}