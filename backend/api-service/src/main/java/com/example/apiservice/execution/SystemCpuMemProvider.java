package com.example.apiservice.execution;

import com.sun.management.OperatingSystemMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;

/**
 * 使用 JVM 提供的 OperatingSystemMXBean 采集系统级 CPU / 内存
 */
@Component
public class SystemCpuMemProvider implements CpuMemProvider {

    private static final Logger log = LoggerFactory.getLogger(SystemCpuMemProvider.class);

    private final OperatingSystemMXBean osBean;

    public SystemCpuMemProvider() {
        var baseBean = ManagementFactory.getOperatingSystemMXBean();
        if (baseBean instanceof OperatingSystemMXBean mxBean) {
            this.osBean = mxBean;
        } else {
            this.osBean = null;
            log.warn("OperatingSystemMXBean is not com.sun.management.OperatingSystemMXBean, metrics will be zeros");
        }
    }

    @Override
    public CpuMemSnapshot snapshotForPid(long pid) {
        CpuMemSnapshot snapshot = new CpuMemSnapshot();

        if (osBean == null) {
            snapshot.setCpuPct(0.0);
            snapshot.setMemBytes(0L);
            return snapshot;
        }

        double load = osBean.getSystemCpuLoad(); // 0..1 或 -1
        if (load >= 0) {
            snapshot.setCpuPct(load * 100.0);
        } else {
            snapshot.setCpuPct(0.0);
        }

        long total = osBean.getTotalPhysicalMemorySize();
        long free = osBean.getFreePhysicalMemorySize();
        long used = Math.max(total - free, 0L);
        snapshot.setMemBytes(used);

        return snapshot;
    }
}