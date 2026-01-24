package com.example.apiservice.controller;

import com.example.apiservice.controller.dto.MetricSampleDto;
import com.example.apiservice.domain.entity.MetricSampleEntity;
import com.example.apiservice.domain.repository.MetricSampleRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/runs/{runId}/metrics")
public class RunMetricsController {

    private final MetricSampleRepository metricSampleRepository;

    public RunMetricsController(MetricSampleRepository metricSampleRepository) {
        this.metricSampleRepository = metricSampleRepository;
    }

    @GetMapping
    public List<MetricSampleDto> listMetrics(@PathVariable("runId") Long runId) {
        List<MetricSampleEntity> entities =
                metricSampleRepository.findByRunIdOrderByTsAsc(runId);

        return entities.stream()
                .map(e -> new MetricSampleDto(
                        e.getTs(),
                        e.getCpuPct(),
                        e.getMemBytes(),
                        e.getSystemCpuPct(),
                        e.getSystemMemBytes(),
                        e.getGpuUtilPct()
                ))
                .toList();
    }
}