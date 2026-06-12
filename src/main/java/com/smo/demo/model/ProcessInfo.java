package com.smo.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * OS MODULE: Represents a running OS process.
 * Also stored in DB for audit trail of resource-heavy processes.
 *
 * Demonstrates: Process management concepts (PID, priority, state),
 * JPA entity relationships, DB audit logs.
 */
@Entity
@Table(name = "process_logs")
@Data
@NoArgsConstructor
public class ProcessInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pid", nullable = false)
    private Long pid;

    @Column(name = "process_name", nullable = false)
    private String processName;

    @Column(name = "cpu_percent")
    private Double cpuPercent;

    @Column(name = "memory_bytes")
    private Long memoryBytes;

    @Column(name = "thread_count")
    private Integer threadCount;

    @Column(name = "priority")
    private Integer priority;

    // OS Scheduling: Process state
    @Column(name = "state")
    private String state;   // Running, Sleeping, Stopped, Zombie

    @Column(name = "user_name")
    private String userName;

    @Column(name = "start_time")
    private String startTime;

    @Column(name = "logged_at")
    private LocalDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        if (loggedAt == null) loggedAt = LocalDateTime.now();
    }
}
