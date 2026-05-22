package com.smo.demo.service;

import com.smo.demo.model.Alert;
import com.smo.demo.model.ProcessInfo;
import com.smo.demo.model.SystemSnapshot;
import com.smo.demo.repository.AlertRepository;
import com.smo.demo.repository.SystemSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.*;

import java.net.InetAddress;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OS MODULE: Core service that reads real-time OS metrics using OSHI library.
 *
 * OSHI (Operating System & Hardware Information) is a JNA-based library that
 * provides cross-platform access to OS-level data — same data you'd get from
 * /proc/cpuinfo on Linux or Task Manager on Windows.
 *
 * Concepts demonstrated:
 * - Process Management: listing processes, PID, state, priority
 * - Memory Management: heap/RAM usage, virtual memory
 * - CPU Scheduling: load, core count, usage per core
 * - File System: disk usage, mount points
 */
@Service
public class SystemMonitorService {

    private static final Logger log = LoggerFactory.getLogger(SystemMonitorService.class);

    private final SystemSnapshotRepository snapshotRepository;
    private final AlertRepository alertRepository;

    public SystemMonitorService(SystemSnapshotRepository snapshotRepository, AlertRepository alertRepository) {
        this.snapshotRepository = snapshotRepository;
        this.alertRepository = alertRepository;
    }

    // OSHI system info - initialized once
    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hal = systemInfo.getHardware();
    private final OperatingSystem os = systemInfo.getOperatingSystem();

    // Thresholds for alerts (can be made configurable)
    private static final double CPU_ALERT_THRESHOLD = 85.0;
    private static final double MEM_ALERT_THRESHOLD = 90.0;
    private static final double DISK_ALERT_THRESHOLD = 95.0;

    // ─────────────────────────────────────────
    // OS MODULE: CPU Metrics
    // ─────────────────────────────────────────

    public Map<String, Object> getCpuMetrics() {
        CentralProcessor cpu = hal.getProcessor();

        // CPU usage: tick-based calculation (OS scheduling concept)
        long[] prevTicks = cpu.getSystemCpuLoadTicks();
        try { Thread.sleep(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        double cpuLoad = cpu.getSystemCpuLoadBetweenTicks(prevTicks) * 100;

        double[] perCoreTicks = cpu.getProcessorCpuLoad(1000);
        List<Double> perCore = Arrays.stream(perCoreTicks)
            .mapToObj(d -> Math.round(d * 1000.0) / 10.0)
            .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("usagePercent", Math.round(cpuLoad * 10.0) / 10.0);
        result.put("physicalCores", cpu.getPhysicalProcessorCount());
        result.put("logicalCores", cpu.getLogicalProcessorCount());
        result.put("processorName", cpu.getProcessorIdentifier().getName());
        result.put("perCoreUsage", perCore);
        result.put("systemLoadAvg", cpu.getSystemLoadAverage(1)[0]);
        return result;
    }

    // ─────────────────────────────────────────
    // OS MODULE: Memory (RAM) Metrics
    // ─────────────────────────────────────────

    public Map<String, Object> getMemoryMetrics() {
        GlobalMemory memory = hal.getMemory();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;

        VirtualMemory vm = memory.getVirtualMemory();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalBytes", total);
        result.put("usedBytes", used);
        result.put("freeBytes", available);
        result.put("usagePercent", Math.round((double) used / total * 1000.0) / 10.0);
        result.put("totalGB", Math.round(total / 1073741824.0 * 10.0) / 10.0);
        result.put("usedGB", Math.round(used / 1073741824.0 * 10.0) / 10.0);
        result.put("swapTotal", vm.getSwapTotal());
        result.put("swapUsed", vm.getSwapUsed());
        return result;
    }

    // ─────────────────────────────────────────
    // OS MODULE: Disk / File System Metrics
    // ─────────────────────────────────────────

    public List<Map<String, Object>> getDiskMetrics() {
        FileSystem fileSystem = os.getFileSystem();
        List<OSFileStore> stores = fileSystem.getFileStores();
        List<Map<String, Object>> disks = new ArrayList<>();

        for (OSFileStore store : stores) {
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            long used = total - usable;
            if (total == 0) continue;

            Map<String, Object> disk = new LinkedHashMap<>();
            disk.put("name", store.getName());
            disk.put("mount", store.getMount());
            disk.put("type", store.getType());
            disk.put("totalBytes", total);
            disk.put("usedBytes", used);
            disk.put("freeBytes", usable);
            disk.put("usagePercent", Math.round((double) used / total * 1000.0) / 10.0);
            disk.put("totalGB", Math.round(total / 1073741824.0 * 10.0) / 10.0);
            disks.add(disk);
        }
        return disks;
    }

    // ─────────────────────────────────────────
    // OS MODULE: Process List
    // ─────────────────────────────────────────

    public List<Map<String, Object>> getTopProcesses(int limit) {
        List<OSProcess> processes = os.getProcesses();

        // Sort by CPU usage descending (like Task Manager)
        return processes.stream()
            .sorted((a, b) -> Double.compare(
                b.getProcessCpuLoadBetweenTicks(b) * 100,
                a.getProcessCpuLoadBetweenTicks(a) * 100))
            .limit(limit)
            .map(p -> {
                Map<String, Object> proc = new LinkedHashMap<>();
                proc.put("pid", p.getProcessID());
                proc.put("name", p.getName());
                proc.put("cpuPercent", Math.round(p.getProcessCpuLoadBetweenTicks(p) * 1000.0) / 10.0);
                proc.put("memoryMB", Math.round(p.getResidentSetSize() / 1048576.0 * 10.0) / 10.0);
                proc.put("threads", p.getThreadCount());
                proc.put("state", p.getState().toString());
                proc.put("user", p.getUser());
                proc.put("priority", p.getPriority());
                return proc;
            })
            .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────
    // CN MODULE: Network Metrics
    // ─────────────────────────────────────────

    public Map<String, Object> getNetworkMetrics() {
        List<NetworkIF> networks = hal.getNetworkIFs();
        Map<String, Object> result = new LinkedHashMap<>();

        long totalBytesSent = 0, totalBytesRecv = 0;
        List<Map<String, Object>> interfaces = new ArrayList<>();

        for (NetworkIF net : networks) {
            net.updateAttributes();
            totalBytesSent += net.getBytesSent();
            totalBytesRecv += net.getBytesRecv();

            Map<String, Object> iface = new LinkedHashMap<>();
            iface.put("name", net.getName());
            iface.put("displayName", net.getDisplayName());
            iface.put("ipv4", net.getIPv4addr().length > 0 ? net.getIPv4addr()[0] : "N/A");
            iface.put("macAddress", net.getMacaddr());
            iface.put("bytesSent", net.getBytesSent());
            iface.put("bytesRecv", net.getBytesRecv());
            iface.put("speed", net.getSpeed());
            interfaces.add(iface);
        }

        // Active TCP connections (CN: Transport Layer concept)
        List<InternetProtocolStats.IPConnection> connections =
            os.getInternetProtocolStats().getConnections();
        long activeConnections = connections.stream()
            .filter(c -> c.getState() == InternetProtocolStats.TcpState.ESTABLISHED)
            .count();

        result.put("totalBytesSent", totalBytesSent);
        result.put("totalBytesRecv", totalBytesRecv);
        result.put("activeConnections", activeConnections);
        result.put("totalConnections", connections.size());
        result.put("interfaces", interfaces);

        // CN: Hostname resolution
        try {
            result.put("hostname", InetAddress.getLocalHost().getHostName());
            result.put("localIp", InetAddress.getLocalHost().getHostAddress());
        } catch (Exception e) {
            result.put("hostname", "unknown");
        }

        return result;
    }

    // ─────────────────────────────────────────
    // CN MODULE: Port Scanner (Concept Demo)
    // ─────────────────────────────────────────

    public List<Map<String, Object>> getOpenPorts() {
        List<InternetProtocolStats.IPConnection> connections =
            os.getInternetProtocolStats().getConnections();

        return connections.stream()
            .filter(c -> c.getLocalPort() > 0)
            .map(c -> {
                Map<String, Object> port = new LinkedHashMap<>();
                port.put("localPort", c.getLocalPort());
                port.put("remoteAddress", c.getForeignAddress() != null ?
                    Arrays.toString(c.getForeignAddress()) : "N/A");
                port.put("remotePort", c.getForeignPort());
                port.put("state", c.getState());
                port.put("pid", getConnectionOwningPid(c));
                port.put("protocol", c.getType().toString());
                return port;
            })
            .distinct()
            .limit(50)
            .collect(Collectors.toList());
    }

    // Reflection helper to support different OSHI versions
    private Integer getConnectionOwningPid(InternetProtocolStats.IPConnection conn) {
        try {
            // Common method name variations in different OSHI versions
            String[] candidates = {"getOwningProcessId", "getOwningProcessID", "getOwningProcessId", "getOwningProcess"};
            for (String name : candidates) {
                try {
                    Method m = conn.getClass().getMethod(name);
                    Object val = m.invoke(conn);
                    if (val instanceof Number) return ((Number) val).intValue();
                } catch (NoSuchMethodException ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    // ─────────────────────────────────────────
    // DBMS MODULE: Scheduled Snapshot (every 30s)
    // Auto-saves current metrics to MySQL for history
    // ─────────────────────────────────────────

    @Scheduled(fixedRate = 30000)  // every 30 seconds
    public void savePeriodicSnapshot() {
        try {
            Map<String, Object> cpu = getCpuMetrics();
            Map<String, Object> mem = getMemoryMetrics();
            List<Map<String, Object>> disks = getDiskMetrics();
            Map<String, Object> net = getNetworkMetrics();

            SystemSnapshot snap = new SystemSnapshot();
            snap.setCpuUsage((Double) cpu.get("usagePercent"));
            snap.setCpuCores((Integer) cpu.get("physicalCores"));
            snap.setTotalMemory((Long) mem.get("totalBytes"));
            snap.setUsedMemory((Long) mem.get("usedBytes"));
            snap.setFreeMemory((Long) mem.get("freeBytes"));
            snap.setBytesSent((Long) net.get("totalBytesSent"));
            snap.setBytesReceived((Long) net.get("totalBytesRecv"));
            snap.setActiveConnections(((Long) net.get("activeConnections")).intValue());
            snap.setHostname((String) net.get("hostname"));

            if (!disks.isEmpty()) {
                snap.setTotalDisk((Long) disks.get(0).get("totalBytes"));
                snap.setUsedDisk((Long) disks.get(0).get("usedBytes"));
            }

            snapshotRepository.save(snap);

            // Check thresholds → generate alerts if needed
            checkAndCreateAlerts(snap);

            log.debug("Snapshot saved: CPU={}%, MEM={}%", snap.getCpuUsage(),
                Math.round((double) snap.getUsedMemory() / snap.getTotalMemory() * 100));
        } catch (Exception e) {
            log.error("Failed to save snapshot: {}", e.getMessage());
        }
    }

    // ─────────────────────────────────────────
    // DBMS + OS MODULE: Alert System
    // ─────────────────────────────────────────

    private void checkAndCreateAlerts(SystemSnapshot snap) {
        if (snap.getCpuUsage() > CPU_ALERT_THRESHOLD) {
            createAlert(Alert.AlertType.CPU, snap.getCpuUsage(), CPU_ALERT_THRESHOLD,
                "CPU usage is critically high: " + snap.getCpuUsage() + "%");
        }
        double memPercent = (double) snap.getUsedMemory() / snap.getTotalMemory() * 100;
        if (memPercent > MEM_ALERT_THRESHOLD) {
            createAlert(Alert.AlertType.MEMORY, memPercent, MEM_ALERT_THRESHOLD,
                "Memory usage exceeded threshold: " + Math.round(memPercent) + "%");
        }
    }

    private void createAlert(Alert.AlertType type, double actual, double threshold, String message) {
        Alert alert = new Alert();
        alert.setAlertType(type);
        alert.setActualValue(actual);
        alert.setThresholdValue(threshold);
        alert.setMessage(message);
        alert.setSeverity(actual > threshold + 10 ? Alert.Severity.CRITICAL : Alert.Severity.HIGH);
        alertRepository.save(alert);
    }

    // ─────────────────────────────────────────
    // DBMS MODULE: Historical Analytics
    // ─────────────────────────────────────────

    public Map<String, Object> getHistoricalStats() {
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        LocalDateTime since7d = LocalDateTime.now().minusDays(7);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("avgCpuLast24h", snapshotRepository.findAvgCpuUsageSince(since24h));
        stats.put("avgCpuLast7d", snapshotRepository.findAvgCpuUsageSince(since7d));
        stats.put("peakMemoryLast24h", snapshotRepository.findPeakMemoryUsageSince(since24h));
        stats.put("highCpuEvents", snapshotRepository.countHighCpuEvents(CPU_ALERT_THRESHOLD, since7d));
        stats.put("recentSnapshots", snapshotRepository.findTop20ByOrderByRecordedAtDesc());
        return stats;
    }
}