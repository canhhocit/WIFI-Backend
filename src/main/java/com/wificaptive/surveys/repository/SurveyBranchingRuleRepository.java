package com.wificaptive.surveys.repository;

import com.wificaptive.surveys.entity.SurveyBranchingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyBranchingRuleRepository extends JpaRepository<SurveyBranchingRule, Long> {
    List<SurveyBranchingRule> findByQuestionId(Long questionId);
}

