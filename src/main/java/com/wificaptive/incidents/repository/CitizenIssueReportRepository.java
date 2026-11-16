package com.wificaptive.incidents.repository;

import com.wificaptive.incidents.entity.CitizenIssueReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CitizenIssueReportRepository extends JpaRepository<CitizenIssueReport, Long> {

    long countByLocationIdAndCreatedAtAfterAndIncidentTicketIdIsNull(Long locationId, Instant cutoff);

    List<CitizenIssueReport> findByLocationIdAndCreatedAtAfterAndIncidentTicketIdIsNullOrderByCreatedAtAsc(Long locationId, Instant cutoff);

    Page<CitizenIssueReport> findByLocationIdOrderByCreatedAtDesc(Long locationId, Pageable pageable);

    Page<CitizenIssueReport> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<CitizenIssueReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<CitizenIssueReport> findByIncidentTicketId(Long incidentTicketId);
}
