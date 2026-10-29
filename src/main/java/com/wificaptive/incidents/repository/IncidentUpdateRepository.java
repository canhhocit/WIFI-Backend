package com.wificaptive.incidents.repository;

import com.wificaptive.incidents.entity.IncidentUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentUpdateRepository extends JpaRepository<IncidentUpdate, Long> {
    List<IncidentUpdate> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}

