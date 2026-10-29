package com.wificaptive.network.repository;

import com.wificaptive.network.entity.NetworkBandwidthPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkBandwidthPolicyRepository extends JpaRepository<NetworkBandwidthPolicy, Long> {
    List<NetworkBandwidthPolicy> findByActiveTrueOrderByPriorityDesc();
    Optional<NetworkBandwidthPolicy> findByName(String name);
}
