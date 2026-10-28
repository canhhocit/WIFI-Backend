package com.wificaptive.surveys.dto;

import com.wificaptive.surveys.entity.SurveyBranchingRule;

public class SurveyBranchingRuleDto {
    private Long id;
    private Long questionId;
    private String conditionValue;
    private Long nextQuestionId;
    private boolean terminateSurvey;

    public static SurveyBranchingRuleDto from(SurveyBranchingRule r) {
        SurveyBranchingRuleDto dto = new SurveyBranchingRuleDto();
        dto.setId(r.getId());
        dto.setQuestionId(r.getQuestionId());
        dto.setConditionValue(r.getConditionValue());
        dto.setNextQuestionId(r.getNextQuestionId());
        dto.setTerminateSurvey(r.isTerminateSurvey());
        return dto;
    }

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

