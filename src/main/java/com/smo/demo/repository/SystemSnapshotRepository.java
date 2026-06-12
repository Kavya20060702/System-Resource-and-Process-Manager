package com.smo.demo.repository;

import com.smo.demo.model.SystemSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DBMS MODULE: Repository with custom JPQL/SQL queries.
 * Demonstrates: Aggregation, filtering by time range, derived queries.
 */
@Repository
public interface SystemSnapshotRepository extends JpaRepository<SystemSnapshot, Long> {

    // Get last N snapshots (for chart history)
    List<SystemSnapshot> findTop20ByOrderByRecordedAtDesc();

    // Get snapshots in a time range (for historical queries)
    List<SystemSnapshot> findByRecordedAtBetweenOrderByRecordedAtAsc(
        LocalDateTime start, LocalDateTime end
    );

    // DBMS: Aggregation - Average CPU usage over last 24 hours
    @Query("SELECT AVG(s.cpuUsage) FROM SystemSnapshot s WHERE s.recordedAt >= :since")
    Double findAvgCpuUsageSince(@Param("since") LocalDateTime since);

    // DBMS: Aggregation - Peak memory usage in a period
    @Query("SELECT MAX(s.usedMemory) FROM SystemSnapshot s WHERE s.recordedAt >= :since")
    Long findPeakMemoryUsageSince(@Param("since") LocalDateTime since);

    // DBMS: Count snapshots where CPU was critically high
    @Query("SELECT COUNT(s) FROM SystemSnapshot s WHERE s.cpuUsage > :threshold AND s.recordedAt >= :since")
    Long countHighCpuEvents(@Param("threshold") Double threshold, @Param("since") LocalDateTime since);

    // DBMS: Get last 7 days hourly avg for trend chart
    @Query(value = """
        SELECT DATE_FORMAT(recorded_at, '%Y-%m-%d %H:00:00') as hour,
               AVG(cpu_usage) as avg_cpu,
               AVG(used_memory / total_memory * 100) as avg_mem
        FROM system_snapshots
        WHERE recorded_at >= NOW() - INTERVAL 7 DAY
        GROUP BY hour
        ORDER BY hour ASC
        """, nativeQuery = true)
    List<Object[]> findHourlyAveragesLast7Days();
}