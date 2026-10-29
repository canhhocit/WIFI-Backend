package com.wificaptive.incidents.repository;

import com.wificaptive.incidents.entity.IncidentTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentTicketRepository extends JpaRepository<IncidentTicket, Long> {

    Optional<IncidentTicket> findByTicketCode(String ticketCode);

    Page<IncidentTicket> findByStatus(String status, Pageable pageable);

    List<IncidentTicket> findByLocationId(Long locationId);

    long countByStatus(String status);
}

