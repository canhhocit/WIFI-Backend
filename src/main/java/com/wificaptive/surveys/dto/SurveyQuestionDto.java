package com.wificaptive.surveys.dto;

import com.wificaptive.surveys.entity.SurveyQuestion;
import java.util.ArrayList;
import java.util.List;

public class SurveyQuestionDto {
    private Long id;
    private Long surveyId;
    private String questionText;
    private String questionType;
    private String optionsJson;
    private int sortOrder;
    private boolean required;
    private List<SurveyBranchingRuleDto> branchingRules = new ArrayList<>();

    public static SurveyQuestionDto from(SurveyQuestion q) {
        SurveyQuestionDto dto = new SurveyQuestionDto();
        dto.setId(q.getId());
        dto.setSurveyId(q.getSurveyId());
        dto.setQuestionText(q.getQuestionText());
        dto.setQuestionType(q.getQuestionType());
        dto.setOptionsJson(q.getOptionsJson());
        dto.setSortOrder(q.getSortOrder());
        dto.setRequired(q.isRequired());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public List<SurveyBranchingRuleDto> getBranchingRules() { return branchingRules; }
    public void setBranchingRules(List<SurveyBranchingRuleDto> branchingRules) { this.branchingRules = branchingRules; }
}

