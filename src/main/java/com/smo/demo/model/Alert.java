package com.smo.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * OS + DBMS MODULE: Stores system alerts when CPU/RAM/Disk exceeds thresholds.
 */
@Entity
@Table(name = "system_alerts")
public class Alert {

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
    public enum AlertType { CPU, MEMORY, DISK, NETWORK }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "threshold_value")
    private Double thresholdValue;  // the limit that was set

    @Column(name = "actual_value")
    private Double actualValue;     // the value that triggered the alert

    @Column(name = "resolved")
    private Boolean resolved = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Alert() {}

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AlertType getAlertType() { return alertType; }
    public void setAlertType(AlertType alertType) { this.alertType = alertType; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Double getThresholdValue() { return thresholdValue; }
    public void setThresholdValue(Double thresholdValue) { this.thresholdValue = thresholdValue; }

    public Double getActualValue() { return actualValue; }
    public void setActualValue(Double actualValue) { this.actualValue = actualValue; }

    public Boolean getResolved() { return resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}