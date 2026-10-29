package com.wificaptive.network.repository;

import com.wificaptive.network.entity.MacBlacklist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MacBlacklistRepository extends JpaRepository<MacBlacklist, Long> {

    Optional<MacBlacklist> findByMacAddressIgnoreCase(String macAddress);

    boolean existsByMacAddressIgnoreCaseAndActiveTrue(String macAddress);

    Page<MacBlacklist> findByActive(Boolean active, Pageable pageable);
}

