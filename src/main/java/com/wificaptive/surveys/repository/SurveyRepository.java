package com.wificaptive.surveys.repository;

import com.wificaptive.surveys.entity.Survey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurveyRepository extends JpaRepository<Survey, Long> {
    Page<Survey> findByActive(boolean active, Pageable pageable);
    Optional<Survey> findByCampaignIdAndActiveTrue(Long campaignId);
}

