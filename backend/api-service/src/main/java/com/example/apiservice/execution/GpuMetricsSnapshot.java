package com.example.apiservice.execution;

/**
 * GPU 指标采样结果：
 * - gpuUtilPct：GPU 利用率（百分比 0-100）
 * - gpuMemUsedBytes：GPU 显存占用（字节）
 */
public class GpuMetricsSnapshot {

    /**
     * GPU 使用率（百分比 0-100）
     */
    private Double gpuUtilPct;

    /**
     * GPU 显存使用量（字节）
     */
    private Long gpuMemUsedBytes;

    public Double getGpuUtilPct() {
        return gpuUtilPct;
    }

    public void setGpuUtilPct(Double gpuUtilPct) {
        this.gpuUtilPct = gpuUtilPct;
    }

    public Long getGpuMemUsedBytes() {
        return gpuMemUsedBytes;
    }

    public void setGpuMemUsedBytes(Long gpuMemUsedBytes) {
        this.gpuMemUsedBytes = gpuMemUsedBytes;
    }

    @Override
    public String toString() {
        return "GpuMetricsSnapshot{" +
                "gpuUtilPct=" + gpuUtilPct +
                ", gpuMemUsedBytes=" + gpuMemUsedBytes +
                '}';
    }
}
