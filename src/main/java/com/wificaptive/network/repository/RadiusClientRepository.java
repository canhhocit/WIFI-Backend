package com.wificaptive.network.repository;

import com.wificaptive.network.entity.RadiusClient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RadiusClientRepository extends JpaRepository<RadiusClient, Long> {

    Optional<RadiusClient> findByIpAddress(String ipAddress);

    List<RadiusClient> findByEnabledTrue();
}

