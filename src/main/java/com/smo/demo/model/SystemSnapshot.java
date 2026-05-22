package com.smo.demo.model;


import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * DBMS MODULE: JPA Entity mapped to `system_snapshots` table in MySQL.
 */
@Entity
@Table(name = "system_snapshots", indexes = {
    @Index(name = "idx_timestamp", columnList = "recorded_at"),
    @Index(name = "idx_cpu_usage", columnList = "cpu_usage")
})
public class SystemSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // OS MODULE: CPU metrics
    @Column(name = "cpu_usage", nullable = false)
    private Double cpuUsage;           // % usage

    @Column(name = "cpu_cores")
    private Integer cpuCores;

    @Column(name = "cpu_temp")
    private Double cpuTemp;            // Celsius (if available)

    // OS MODULE: Memory metrics
    @Column(name = "total_memory")
    private Long totalMemory;          // bytes

    @Column(name = "used_memory")
    private Long usedMemory;           // bytes

    @Column(name = "free_memory")
    private Long freeMemory;           // bytes

    // OS MODULE: Disk metrics
    @Column(name = "total_disk")
    private Long totalDisk;            // bytes

    @Column(name = "used_disk")
    private Long usedDisk;             // bytes

    // CN MODULE: Network metrics
    @Column(name = "bytes_sent")
    private Long bytesSent;            // total bytes sent

    @Column(name = "bytes_received")
    private Long bytesReceived;        // total bytes received

    @Column(name = "active_connections")
    private Integer activeConnections; // active TCP connections

    // Metadata
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "hostname")
    private String hostname;

    public SystemSnapshot() {
    }

    @PrePersist
    protected void onCreate() {
        if (recordedAt == null) recordedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }

    public Integer getCpuCores() { return cpuCores; }
    public void setCpuCores(Integer cpuCores) { this.cpuCores = cpuCores; }

    public Double getCpuTemp() { return cpuTemp; }
    public void setCpuTemp(Double cpuTemp) { this.cpuTemp = cpuTemp; }

    public Long getTotalMemory() { return totalMemory; }
    public void setTotalMemory(Long totalMemory) { this.totalMemory = totalMemory; }

    public Long getUsedMemory() { return usedMemory; }
    public void setUsedMemory(Long usedMemory) { this.usedMemory = usedMemory; }

    public Long getFreeMemory() { return freeMemory; }
    public void setFreeMemory(Long freeMemory) { this.freeMemory = freeMemory; }

    public Long getTotalDisk() { return totalDisk; }
    public void setTotalDisk(Long totalDisk) { this.totalDisk = totalDisk; }

    public Long getUsedDisk() { return usedDisk; }
    public void setUsedDisk(Long usedDisk) { this.usedDisk = usedDisk; }

    public Long getBytesSent() { return bytesSent; }
    public void setBytesSent(Long bytesSent) { this.bytesSent = bytesSent; }

    public Long getBytesReceived() { return bytesReceived; }
    public void setBytesReceived(Long bytesReceived) { this.bytesReceived = bytesReceived; }

    public Integer getActiveConnections() { return activeConnections; }
    public void setActiveConnections(Integer activeConnections) { this.activeConnections = activeConnections; }

    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }
}