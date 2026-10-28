package com.wificaptive.surveys.repository;

import com.wificaptive.surveys.entity.SurveyQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyQuestionRepository extends JpaRepository<SurveyQuestion, Long> {
    List<SurveyQuestion> findBySurveyIdOrderBySortOrderAsc(Long surveyId);
    void deleteBySurveyId(Long surveyId);
}

