package com.example.apiservice.controller.dto;

import java.time.Instant;

/**
 * 指标采样点对外返回 DTO：
 * - cpuPct / memBytes：进程级
 * - systemCpuPct / systemMemBytes：系统级
 * - gpuUtilPct：GPU 利用率
 * - gpuMemUsedBytes：GPU 显存占用（字节）
 */
public record MetricSampleDto(
        Instant ts,
        Double cpuPct,
        Long memBytes,
        Double systemCpuPct,
        Long systemMemBytes,
        Double gpuUtilPct,
        Long gpuMemUsedBytes
) {
}
