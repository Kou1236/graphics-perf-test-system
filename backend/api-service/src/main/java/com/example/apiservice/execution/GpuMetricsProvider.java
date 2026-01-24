package com.example.apiservice.execution;

/**
 * GPU 指标提供者
 */
public interface GpuMetricsProvider {

    /**
     * 获取当前时刻的 GPU 利用率等指标
     *
     * @return 采样结果；如果获取失败，返回 null
     */
    GpuMetricsSnapshot snapshot();
}