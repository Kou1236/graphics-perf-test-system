package com.example.apiservice.execution;

import lombok.Data;

/**
 * 单次 CPU / 内存快照
 */
@Data
public class CpuMemSnapshot {

    /**
     * CPU 使用率（0-100）
     */
    private double cpuPct;

    /**
     * 内存占用（字节），此处为系统已用内存
     */
    private long memBytes;
}