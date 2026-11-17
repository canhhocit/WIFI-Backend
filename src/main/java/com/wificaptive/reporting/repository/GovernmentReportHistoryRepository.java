package com.wificaptive.reporting.repository;

import com.wificaptive.reporting.entity.GovernmentReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GovernmentReportHistoryRepository extends JpaRepository<GovernmentReportHistory, Long> {
    List<GovernmentReportHistory> findAllByOrderBySubmittedAtDesc();
}
