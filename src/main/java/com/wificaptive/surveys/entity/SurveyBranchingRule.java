package com.wificaptive.surveys.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "survey_branching_rules")
public class SurveyBranchingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "condition_value", nullable = false)
    private String conditionValue;

    @Column(name = "next_question_id")
    private Long nextQuestionId;

    @Column(name = "terminate_survey", nullable = false)
    private boolean terminateSurvey = false;

    public SurveyBranchingRule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getConditionValue() { return conditionValue; }
    public void setConditionValue(String conditionValue) { this.conditionValue = conditionValue; }

    public Long getNextQuestionId() { return nextQuestionId; }
    public void setNextQuestionId(Long nextQuestionId) { this.nextQuestionId = nextQuestionId; }

    public boolean isTerminateSurvey() { return terminateSurvey; }
    public void setTerminateSurvey(boolean terminateSurvey) { this.terminateSurvey = terminateSurvey; }
}

