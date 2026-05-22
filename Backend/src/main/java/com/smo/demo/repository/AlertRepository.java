package com.smo.demo.repository;

import com.smo.demo.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByResolvedFalseOrderByCreatedAtDesc();
    List<Alert> findTop10ByOrderByCreatedAtDesc();
    Long countByResolvedFalse();
}