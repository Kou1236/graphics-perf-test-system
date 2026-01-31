package com.example.apiservice.domain.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "metric_samples")
public class MetricSampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "ts", nullable = false)
    private Instant ts;

    /**
     * 进程 CPU 使用率（百分比 0-100）
     */
    @Column(name = "cpu_pct")
    private Double cpuPct;

    /**
     * 进程内存占用（字节）
     */
    @Column(name = "mem_bytes")
    private Long memBytes;

    /**
     * 系统 CPU 使用率（百分比 0-100）
     */
    @Column(name = "system_cpu_pct")
    private Double systemCpuPct;

    /**
     * 系统内存占用（字节）
     */
    @Column(name = "system_mem_bytes")
    private Long systemMemBytes;

    /**
     * GPU 使用率（百分比 0-100），当前通过 nvidia-smi 获取
     */
    @Column(name = "gpu_util_pct")
    private Double gpuUtilPct;

    /**
     * GPU 显存占用（字节），通过 nvidia-smi 的 memory.used 转换
     */
    @Column(name = "gpu_mem_used_bytes")
    private Long gpuMemUsedBytes;

    /**
     * 兼容历史的扩展 JSON 字段（当前采集逻辑不再写入）
     */
    @Column(name = "extra_json", columnDefinition = "TEXT")
    private String extraJson;

    // ===== getters / setters =====

    public Long getId() {
        return id;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public Instant getTs() {
        return ts;
    }

    public void setTs(Instant ts) {
        this.ts = ts;
    }

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

    public Double getSystemCpuPct() {
        return systemCpuPct;
    }

    public void setSystemCpuPct(Double systemCpuPct) {
        this.systemCpuPct = systemCpuPct;
    }

    public Long getSystemMemBytes() {
        return systemMemBytes;
    }

    public void setSystemMemBytes(Long systemMemBytes) {
        this.systemMemBytes = systemMemBytes;
    }

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

    public String getExtraJson() {
        return extraJson;
    }

    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson;
    }
}
