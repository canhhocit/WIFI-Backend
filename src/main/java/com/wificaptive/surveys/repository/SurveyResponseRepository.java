package com.wificaptive.surveys.repository;

import com.wificaptive.surveys.entity.SurveyResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SurveyResponseRepository extends JpaRepository<SurveyResponse, Long> {
    Page<SurveyResponse> findBySurveyId(Long surveyId, Pageable pageable);
    Page<SurveyResponse> findBySurveyIdAndLocationId(Long surveyId, Long locationId, Pageable pageable);
    long countBySurveyId(Long surveyId);
    long countBySurveyIdAndLocationId(Long surveyId, Long locationId);
    List<SurveyResponse> findBySubmittedAtGreaterThanEqualAndSubmittedAtLessThan(Instant start, Instant end);

    /** Câu trả lời khảo sát theo MAC (không phân biệt hoa thường), mới nhất trước — dùng ở màn chi tiết thiết bị khách. */
    List<SurveyResponse> findByClientMacIgnoreCaseOrderBySubmittedAtDesc(String clientMac);
}

