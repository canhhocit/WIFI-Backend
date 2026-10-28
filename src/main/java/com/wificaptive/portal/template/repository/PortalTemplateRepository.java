package com.wificaptive.portal.template.repository;

import com.wificaptive.portal.template.entity.PortalTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortalTemplateRepository extends JpaRepository<PortalTemplate, Long> {
    List<PortalTemplate> findByActiveTrue();
}

