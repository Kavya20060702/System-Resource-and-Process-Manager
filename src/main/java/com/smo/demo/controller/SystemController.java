package com.smo.demo.controller;

import com.smo.demo.service.SchedulerService;
import com.smo.demo.service.SchedulerService.Process;
import com.smo.demo.service.SystemMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller - exposes all OS + CN + DBMS endpoints.
 *
 * All endpoints return JSON, consumed by the frontend dashboard.
 *
 * Base URL: http://localhost:8080/api
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")  // Allow frontend to connect
public class SystemController {

    private final SystemMonitorService monitorService;
    private final SchedulerService schedulerService;

    public SystemController(SystemMonitorService monitorService, SchedulerService schedulerService) {
        this.monitorService = monitorService;
        this.schedulerService = schedulerService;
    }

    // ─────────────────────────────────────────
    // OS MODULE: System Overview (Dashboard)
    // GET /api/system/overview
    // ─────────────────────────────────────────
    @GetMapping("/system/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return ResponseEntity.ok(Map.of(
            "cpu", monitorService.getCpuMetrics(),
            "memory", monitorService.getMemoryMetrics(),
            "disks", monitorService.getDiskMetrics(),
            "network", monitorService.getNetworkMetrics()
        ));
    }

    // ─────────────────────────────────────────
    // OS MODULE: CPU Metrics
    // GET /api/cpu
    // ─────────────────────────────────────────
    @GetMapping("/cpu")
    public ResponseEntity<Map<String, Object>> getCpu() {
        return ResponseEntity.ok(monitorService.getCpuMetrics());
    }

    // ─────────────────────────────────────────
    // OS MODULE: Memory Metrics
    // GET /api/memory
    // ─────────────────────────────────────────
    @GetMapping("/memory")
    public ResponseEntity<Map<String, Object>> getMemory() {
        return ResponseEntity.ok(monitorService.getMemoryMetrics());
    }

    // ─────────────────────────────────────────
    // OS MODULE: Disk Metrics
    // GET /api/disk
    // ─────────────────────────────────────────
    @GetMapping("/disk")
    public ResponseEntity<List<Map<String, Object>>> getDisk() {
        return ResponseEntity.ok(monitorService.getDiskMetrics());
    }

    // ─────────────────────────────────────────
    // OS MODULE: Process List
    // GET /api/processes?limit=20
    // ─────────────────────────────────────────
    @GetMapping("/processes")
    public ResponseEntity<List<Map<String, Object>>> getProcesses(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(monitorService.getTopProcesses(limit));
    }

    // ─────────────────────────────────────────
    // CN MODULE: Network Metrics
    // GET /api/network
    // ─────────────────────────────────────────
    @GetMapping("/network")
    public ResponseEntity<Map<String, Object>> getNetwork() {
        return ResponseEntity.ok(monitorService.getNetworkMetrics());
    }

    // ─────────────────────────────────────────
    // CN MODULE: Open Ports
    // GET /api/ports
    // ─────────────────────────────────────────
    @GetMapping("/ports")
    public ResponseEntity<List<Map<String, Object>>> getPorts() {
        return ResponseEntity.ok(monitorService.getOpenPorts());
    }

    // ─────────────────────────────────────────
    // DBMS MODULE: Historical Statistics
    // GET /api/history/stats
    // ─────────────────────────────────────────
    @GetMapping("/history/stats")
    public ResponseEntity<Map<String, Object>> getHistoricalStats() {
        return ResponseEntity.ok(monitorService.getHistoricalStats());
    }

    // ─────────────────────────────────────────
    // OS MODULE: CPU Scheduling Simulator
    // POST /api/scheduler/simulate
    // Body: { "algorithm": "rr", "quantum": 2, "processes": [...] }
    // ─────────────────────────────────────────
    @PostMapping("/scheduler/simulate")
    public ResponseEntity<SchedulerService.ScheduleResult> simulate(
            @RequestBody Map<String, Object> body) {

        String algo = (String) body.getOrDefault("algorithm", "fcfs");
        int quantum = (int) body.getOrDefault("quantum", 2);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawProcesses = (List<Map<String, Object>>) body.get("processes");

        List<Process> processes = rawProcesses.stream().map(p -> new Process(
            (String) p.get("id"),
            (int) p.get("arrivalTime"),
            (int) p.get("burstTime"),
            (int) p.getOrDefault("priority", 0)
        )).toList();

        SchedulerService.ScheduleResult result = switch (algo.toLowerCase()) {
            case "sjf" -> schedulerService.sjf(processes);
            case "rr" -> schedulerService.roundRobin(processes, quantum);
            default -> schedulerService.fcfs(processes);
        };

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────
    // Health Check
    // GET /api/health
    // ─────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "SmartOS Monitor"));
    }
}