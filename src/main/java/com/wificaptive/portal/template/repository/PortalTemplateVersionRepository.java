package com.wificaptive.portal.template.repository;

import com.wificaptive.portal.template.entity.PortalTemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortalTemplateVersionRepository extends JpaRepository<PortalTemplateVersion, Long> {

    List<PortalTemplateVersion> findByTemplateIdOrderByVersionNumberDesc(Long templateId);

    @Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM PortalTemplateVersion v WHERE v.templateId = :templateId")
    int findMaxVersionNumber(@Param("templateId") Long templateId);

    Optional<PortalTemplateVersion> findByTemplateIdAndVersionNumber(Long templateId, Integer versionNumber);
}

