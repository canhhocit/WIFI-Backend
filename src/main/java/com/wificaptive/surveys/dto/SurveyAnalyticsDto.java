package com.wificaptive.surveys.dto;

import java.time.Instant;
import java.util.*;

public class SurveyAnalyticsDto {

    private Long surveyId;
    private String surveyTitle;
    private long totalResponses;
    private int maxResponsesQuota;
    private double completionRatePercent;
    private Double averageRating;
    private Map<String, Long> locationDistribution = new LinkedHashMap<>();
    private List<QuestionAnalyticsDto> questionAnalytics = new ArrayList<>();

    public SurveyAnalyticsDto() {}

    public Long getSurveyId() { return surveyId; }
    public void setSurveyId(Long surveyId) { this.surveyId = surveyId; }

    public String getSurveyTitle() { return surveyTitle; }
    public void setSurveyTitle(String surveyTitle) { this.surveyTitle = surveyTitle; }

    public long getTotalResponses() { return totalResponses; }
    public void setTotalResponses(long totalResponses) { this.totalResponses = totalResponses; }

    public int getMaxResponsesQuota() { return maxResponsesQuota; }
    public void setMaxResponsesQuota(int maxResponsesQuota) { this.maxResponsesQuota = maxResponsesQuota; }

    public double getCompletionRatePercent() { return completionRatePercent; }
    public void setCompletionRatePercent(double completionRatePercent) { this.completionRatePercent = completionRatePercent; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Map<String, Long> getLocationDistribution() { return locationDistribution; }
    public void setLocationDistribution(Map<String, Long> locationDistribution) { this.locationDistribution = locationDistribution; }

    public List<QuestionAnalyticsDto> getQuestionAnalytics() { return questionAnalytics; }
    public void setQuestionAnalytics(List<QuestionAnalyticsDto> questionAnalytics) { this.questionAnalytics = questionAnalytics; }

    public static class QuestionAnalyticsDto {
        private Long questionId;
        private String questionText;
        private String questionType;
        private boolean required;
        private long totalAnswersCount;
        private Map<String, Long> optionCounts = new LinkedHashMap<>();
        private Map<String, Double> optionPercentages = new LinkedHashMap<>();
        private Double averageRating;
        private Map<Integer, Long> ratingCounts = new LinkedHashMap<>();
        private List<TextFeedbackItemDto> textResponses = new ArrayList<>();

        public QuestionAnalyticsDto() {}

        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }

        public String getQuestionText() { return questionText; }
        public void setQuestionText(String questionText) { this.questionText = questionText; }

        public String getQuestionType() { return questionType; }
        public void setQuestionType(String questionType) { this.questionType = questionType; }

        public boolean isRequired() { return required; }
        public void setRequired(boolean required) { this.required = required; }

        public long getTotalAnswersCount() { return totalAnswersCount; }
        public void setTotalAnswersCount(long totalAnswersCount) { this.totalAnswersCount = totalAnswersCount; }

        public Map<String, Long> getOptionCounts() { return optionCounts; }
        public void setOptionCounts(Map<String, Long> optionCounts) { this.optionCounts = optionCounts; }

        public Map<String, Double> getOptionPercentages() { return optionPercentages; }
        public void setOptionPercentages(Map<String, Double> optionPercentages) { this.optionPercentages = optionPercentages; }

        public Double getAverageRating() { return averageRating; }
        public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

        public Map<Integer, Long> getRatingCounts() { return ratingCounts; }
        public void setRatingCounts(Map<Integer, Long> ratingCounts) { this.ratingCounts = ratingCounts; }

        public List<TextFeedbackItemDto> getTextResponses() { return textResponses; }
        public void setTextResponses(List<TextFeedbackItemDto> textResponses) { this.textResponses = textResponses; }
    }

    public static class TextFeedbackItemDto {
        private Long responseId;
        private String locationName;
        private String clientMac;
        private Instant submittedAt;
        private String answerText;

        public TextFeedbackItemDto() {}

        public TextFeedbackItemDto(Long responseId, String locationName, String clientMac, Instant submittedAt, String answerText) {
            this.responseId = responseId;
            this.locationName = locationName;
            this.clientMac = clientMac;
            this.submittedAt = submittedAt;
            this.answerText = answerText;
        }

        public Long getResponseId() { return responseId; }
        public void setResponseId(Long responseId) { this.responseId = responseId; }

        public String getLocationName() { return locationName; }
        public void setLocationName(String locationName) { this.locationName = locationName; }

        public String getClientMac() { return clientMac; }
        public void setClientMac(String clientMac) { this.clientMac = clientMac; }

        public Instant getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }

        public String getAnswerText() { return answerText; }
        public void setAnswerText(String answerText) { this.answerText = answerText; }
    }
}

