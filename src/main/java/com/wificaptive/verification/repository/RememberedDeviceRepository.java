package com.wificaptive.verification.repository;

import com.wificaptive.verification.entity.RememberedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RememberedDeviceRepository extends JpaRepository<RememberedDevice, Long> {
    Optional<RememberedDevice> findByClientMac(String clientMac);
}

