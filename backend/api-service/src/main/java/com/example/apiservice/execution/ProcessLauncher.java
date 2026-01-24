package com.example.apiservice.execution;

/**
 * 外部进程启动器接口
 */
public interface ProcessLauncher {

    /**
     * 启动外部进程并在内部完成 CPU / 内存采集，直到进程结束
     *
     * @param request          启动参数
     * @param runId            对应的 RunId，用于写入 MetricSample
     * @param sampleIntervalMs 采样间隔（毫秒）
     */
    RunExecutionResult launchAndWait(ProcessStartRequest request, Long runId, long sampleIntervalMs);
}