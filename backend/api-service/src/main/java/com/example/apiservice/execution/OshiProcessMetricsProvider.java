package com.example.apiservice.execution;

import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 基于 OSHI 的进程级 CPU / 内存指标实现
 */
@Component
public class OshiProcessMetricsProvider implements ProcessMetricsProvider {

    private static final Logger log = LoggerFactory.getLogger(OshiProcessMetricsProvider.class);

    private final SystemInfo systemInfo;
    private final OperatingSystem operatingSystem;

    public OshiProcessMetricsProvider() {
        this.systemInfo = new SystemInfo();
        this.operatingSystem = systemInfo.getOperatingSystem();
    }

    @Override
    public ProcessMetricsSnapshot snapshotForPid(long pid) {
        int intPid;
        try {
            intPid = Math.toIntExact(pid);
        } catch (ArithmeticException ex) {
            log.warn("PID {} is out of int range, skip process metrics", pid);
            return null;
        }

        OSProcess process = operatingSystem.getProcess(intPid);
        if (process == null) {
            log.debug("Process {} not found by OSHI, skip process metrics", pid);
            return null;
        }

        try {
            ProcessMetricsSnapshot snapshot = new ProcessMetricsSnapshot();

            double cpuLoad = process.getProcessCpuLoadCumulative();
            if (cpuLoad >= 0 && cpuLoad <= 1) {
                snapshot.setCpuPct(cpuLoad * 100.0);
            } else {
                snapshot.setCpuPct(null);
            }

            long rss = process.getResidentSetSize();
            snapshot.setMemBytes(rss >= 0 ? rss : null);

            return snapshot;
        } catch (Exception e) {
            log.warn("Failed to get process metrics for pid {}: {}", pid, e.getMessage());
            return null;
        }
    }
}
