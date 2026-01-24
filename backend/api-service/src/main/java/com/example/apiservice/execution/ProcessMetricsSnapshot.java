package com.example.apiservice.execution;

/**
 * 单进程 CPU / 内存采样结果
 */
public class ProcessMetricsSnapshot {

    private Double cpuPct;
    private Long memBytes;

    public Double getCpuPct() {
        return cpuPct;
    }

    public void setCpuPct(Double cpuPct) {
        this.cpuPct = cpuPct;
    }

    public Long getMemBytes() {
        return memBytes;
    }

    public void setMemBytes(Long memBytes) {
        this.memBytes = memBytes;
    }
}