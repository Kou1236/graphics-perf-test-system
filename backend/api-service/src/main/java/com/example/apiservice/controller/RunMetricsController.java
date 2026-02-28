package com.example.apiservice.controller;

import com.example.apiservice.controller.dto.MetricSampleDto;
import com.example.apiservice.domain.entity.MetricSampleEntity;
import com.example.apiservice.domain.entity.RunEntity;
import com.example.apiservice.domain.repository.MetricSampleRepository;
import com.example.apiservice.domain.repository.RunRepository;
import com.example.apiservice.execution.renderprobe.RenderEventsAnalyzer;
import com.example.apiservice.execution.renderprobe.RenderSummary;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/runs/{runId}/metrics")
public class RunMetricsController {

    private final MetricSampleRepository metricSampleRepository;
    private final RunRepository runRepository;
    private final RenderEventsAnalyzer renderEventsAnalyzer;

    public RunMetricsController(MetricSampleRepository metricSampleRepository,
                                RunRepository runRepository,
                                RenderEventsAnalyzer renderEventsAnalyzer) {
        this.metricSampleRepository = metricSampleRepository;
        this.runRepository = runRepository;
        this.renderEventsAnalyzer = renderEventsAnalyzer;
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
                        e.getGpuUtilPct(),
                        e.getGpuMemUsedBytes()
                ))
                .toList();
    }

    @GetMapping("/render-summary")
    public RenderSummary renderSummary(@PathVariable("runId") Long runId) {
        RunEntity run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));

        if (run.getRenderEventsPath() == null || run.getRenderEventsPath().isBlank()) {
            RenderSummary s = new RenderSummary();
            s.setError("renderEventsPath is empty on Run");
            return s;
        }

        return renderEventsAnalyzer.analyzeJsonl(Path.of(run.getRenderEventsPath()));
    }
}