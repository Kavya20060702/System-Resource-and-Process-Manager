package com.smo.demo.service;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * OS MODULE: CPU Scheduling Algorithm Simulator
 *
 * Implements the 3 classic CPU scheduling algorithms taught in OS:
 * 1. FCFS  - First Come First Served (non-preemptive)
 * 2. SJF   - Shortest Job First (non-preemptive)
 * 3. RR    - Round Robin (preemptive, with time quantum)
 *
 * Returns Gantt chart data + performance metrics:
 * - Average Waiting Time
 * - Average Turnaround Time
 * - CPU Utilization
 *
 * This is the most interview-asked OS topic — having a
 * working visual simulator is a HUGE differentiator.
 */
@Service
public class SchedulerService {

    public record Process(String id, int arrivalTime, int burstTime, int priority) {}

    public record GanttBlock(String processId, int start, int end) {}

    public static class ScheduleResult {
        public List<GanttBlock> ganttChart = new ArrayList<>();
        public List<Map<String, Object>> processStats = new ArrayList<>();
        public double avgWaitingTime;
        public double avgTurnaroundTime;
        public double cpuUtilization;
        public String algorithm;
    }

    // ─────────────────────────────────────────
    // FCFS - First Come First Served
    // ─────────────────────────────────────────
    public ScheduleResult fcfs(List<Process> processes) {
        ScheduleResult result = new ScheduleResult();
        result.algorithm = "FCFS";

        List<Process> sorted = new ArrayList<>(processes);
        sorted.sort(Comparator.comparingInt(Process::arrivalTime));

        int currentTime = 0;
        int totalWait = 0;

        for (Process p : sorted) {
            if (currentTime < p.arrivalTime()) currentTime = p.arrivalTime();

            int waitTime = currentTime - p.arrivalTime();
            int finishTime = currentTime + p.burstTime();
            int turnaround = finishTime - p.arrivalTime();

            result.ganttChart.add(new GanttBlock(p.id(), currentTime, finishTime));
            result.processStats.add(buildStat(p, waitTime, turnaround, finishTime));

            totalWait += waitTime;
            currentTime = finishTime;
        }

        result.avgWaitingTime = (double) totalWait / processes.size();
        result.avgTurnaroundTime = result.processStats.stream()
            .mapToInt(s -> (int) s.get("turnaround")).average().orElse(0);
        result.cpuUtilization = calcUtilization(processes, currentTime);
        return result;
    }

    // ─────────────────────────────────────────
    // SJF - Shortest Job First (Non-Preemptive)
    // ─────────────────────────────────────────
    public ScheduleResult sjf(List<Process> processes) {
        ScheduleResult result = new ScheduleResult();
        result.algorithm = "SJF";

        List<Process> remaining = new ArrayList<>(processes);
        int currentTime = 0;
        int totalWait = 0;

        while (!remaining.isEmpty()) {
            // Find arrived processes
            final int time = currentTime;
            List<Process> available = remaining.stream()
                .filter(p -> p.arrivalTime() <= time)
                .sorted(Comparator.comparingInt(Process::burstTime))
                .toList();

            if (available.isEmpty()) {
                currentTime = remaining.stream()
                    .mapToInt(Process::arrivalTime).min().orElse(0);
                continue;
            }

            Process p = available.get(0);
            int waitTime = currentTime - p.arrivalTime();
            int finishTime = currentTime + p.burstTime();
            int turnaround = finishTime - p.arrivalTime();

            result.ganttChart.add(new GanttBlock(p.id(), currentTime, finishTime));
            result.processStats.add(buildStat(p, waitTime, turnaround, finishTime));

            totalWait += waitTime;
            currentTime = finishTime;
            remaining.remove(p);
        }

        result.avgWaitingTime = (double) totalWait / processes.size();
        result.avgTurnaroundTime = result.processStats.stream()
            .mapToInt(s -> (int) s.get("turnaround")).average().orElse(0);
        result.cpuUtilization = calcUtilization(processes, currentTime);
        return result;
    }

    // ─────────────────────────────────────────
    // Round Robin - Preemptive with Time Quantum
    // ─────────────────────────────────────────
    public ScheduleResult roundRobin(List<Process> processes, int quantum) {
        ScheduleResult result = new ScheduleResult();
        result.algorithm = "Round Robin (Q=" + quantum + ")";

        // Copy processes with remaining burst times
        Map<String, Integer> remaining = new LinkedHashMap<>();
        Map<String, Integer> waitingTime = new LinkedHashMap<>();
        Map<String, Integer> finishTime = new LinkedHashMap<>();

        List<Process> sorted = new ArrayList<>(processes);
        sorted.sort(Comparator.comparingInt(Process::arrivalTime));

        for (Process p : sorted) {
            remaining.put(p.id(), p.burstTime());
            waitingTime.put(p.id(), 0);
        }

        Queue<Process> queue = new LinkedList<>();
        int currentTime = 0;
        int idx = 0;

        // Add initially arrived processes
        while (idx < sorted.size() && sorted.get(idx).arrivalTime() <= currentTime) {
            queue.add(sorted.get(idx++));
        }

        while (!queue.isEmpty()) {
            Process p = queue.poll();
            int rem = remaining.get(p.id());

            int execTime = Math.min(rem, quantum);
            int start = currentTime;
            currentTime += execTime;
            remaining.put(p.id(), rem - execTime);

            result.ganttChart.add(new GanttBlock(p.id(), start, currentTime));

            // Add newly arrived processes
            while (idx < sorted.size() && sorted.get(idx).arrivalTime() <= currentTime) {
                queue.add(sorted.get(idx++));
            }

            if (remaining.get(p.id()) > 0) {
                queue.add(p); // Re-add if not done
            } else {
                finishTime.put(p.id(), currentTime);
            }
        }

        // Calculate stats
        for (Process p : processes) {
            int ft = finishTime.getOrDefault(p.id(), currentTime);
            int wt = ft - p.arrivalTime() - p.burstTime();
            int ta = ft - p.arrivalTime();
            result.processStats.add(buildStat(p, Math.max(0, wt), ta, ft));
        }

        result.avgWaitingTime = result.processStats.stream()
            .mapToInt(s -> (int) s.get("waitingTime")).average().orElse(0);
        result.avgTurnaroundTime = result.processStats.stream()
            .mapToInt(s -> (int) s.get("turnaround")).average().orElse(0);
        result.cpuUtilization = calcUtilization(processes, currentTime);
        return result;
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────
    private Map<String, Object> buildStat(Process p, int wait, int turnaround, int finish) {
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("id", p.id());
        stat.put("arrivalTime", p.arrivalTime());
        stat.put("burstTime", p.burstTime());
        stat.put("waitingTime", wait);
        stat.put("turnaround", turnaround);
        stat.put("finishTime", finish);
        return stat;
    }

    private double calcUtilization(List<Process> processes, int totalTime) {
        int totalBurst = processes.stream().mapToInt(Process::burstTime).sum();
        return totalTime > 0 ? Math.round((double) totalBurst / totalTime * 1000.0) / 10.0 : 0;
    }
}