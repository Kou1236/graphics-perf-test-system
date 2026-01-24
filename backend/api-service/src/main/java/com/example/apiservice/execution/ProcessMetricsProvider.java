package com.example.apiservice.execution;

/**
 * 进程级 CPU / 内存指标提供者
 */
public interface ProcessMetricsProvider {

    /**
     * 根据 pid 获取当前时刻该进程的 CPU / 内存指标
     *
     * @param pid 目标进程 pid
     * @return 采样结果；如果进程不存在或获取失败，返回 null
     */
    ProcessMetricsSnapshot snapshotForPid(long pid);
}