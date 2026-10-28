package com.wificaptive.locations.repository;

import com.wificaptive.locations.entity.WifiLocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WifiLocationRepository extends JpaRepository<WifiLocation, Long> {
    Optional<WifiLocation> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<WifiLocation> findByActiveTrue();
    Page<WifiLocation> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
