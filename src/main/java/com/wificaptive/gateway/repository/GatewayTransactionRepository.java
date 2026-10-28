package com.wificaptive.gateway.repository;

import com.wificaptive.gateway.entity.GatewayTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GatewayTransactionRepository extends JpaRepository<GatewayTransaction, Long> {
    Page<GatewayTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<GatewayTransaction> findBySessionIdOrderByCreatedAtDesc(Long sessionId);
    long countByOperation(String operation);
    long countBySuccessFalse();
}
