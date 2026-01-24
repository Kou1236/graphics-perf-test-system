package com.example.apiservice.execution;

/**
 * CPU / 内存采集接口
 */
public interface CpuMemProvider {

    /**
     * 基于 pid 获取当前时刻的 CPU / 内存快照
     * 当前实现忽略 pid，采集系统整体 CPU / 内存
     */
    CpuMemSnapshot snapshotForPid(long pid);
}