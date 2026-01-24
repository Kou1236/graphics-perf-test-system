package com.example.apiservice.execution;

/**
 * GPU 利用率采样结果（当前只关心整体 GPU 使用率）
 */
public class GpuMetricsSnapshot {

    private Double gpuUtilPct;

    public Double getGpuUtilPct() {
        return gpuUtilPct;
    }

    public void setGpuUtilPct(Double gpuUtilPct) {
        this.gpuUtilPct = gpuUtilPct;
    }
}