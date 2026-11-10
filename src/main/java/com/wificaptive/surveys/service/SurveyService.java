package com.wificaptive.surveys.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.surveys.dto.*;
import com.wificaptive.surveys.entity.Survey;
import com.wificaptive.surveys.entity.SurveyBranchingRule;
import com.wificaptive.surveys.entity.SurveyQuestion;
import com.wificaptive.surveys.entity.SurveyResponse;
import com.wificaptive.surveys.repository.SurveyBranchingRuleRepository;
import com.wificaptive.surveys.repository.SurveyQuestionRepository;
import com.wificaptive.surveys.repository.SurveyRepository;
import com.wificaptive.surveys.repository.SurveyResponseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SurveyService {

    private static final Logger log = LoggerFactory.getLogger(SurveyService.class);

    private final SurveyRepository surveyRepository;
    private final SurveyQuestionRepository questionRepository;
    private final SurveyBranchingRuleRepository branchingRuleRepository;
    private final SurveyResponseRepository responseRepository;
    private final WifiLocationRepository locationRepository;
    private final SurveyExcelService surveyExcelService;
    private final ObjectMapper objectMapper;

    public SurveyService(
            SurveyRepository surveyRepository,
            SurveyQuestionRepository questionRepository,
            SurveyBranchingRuleRepository branchingRuleRepository,
            SurveyResponseRepository responseRepository,
            WifiLocationRepository locationRepository,
            SurveyExcelService surveyExcelService,
            ObjectMapper objectMapper) {
        this.surveyRepository = surveyRepository;
        this.questionRepository = questionRepository;
        this.branchingRuleRepository = branchingRuleRepository;
        this.responseRepository = responseRepository;
        this.locationRepository = locationRepository;
        this.surveyExcelService = surveyExcelService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<SurveyDto> getSurveys(Boolean active, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Survey> p;
        if (active != null) {
            p = surveyRepository.findByActive(active, pr);
        } else {
            p = surveyRepository.findAll(pr);
        }
        return PageResponse.from(p.map(SurveyDto::from));
    }

    @Transactional(readOnly = true)
    public SurveyDto getSurveyWithDetails(Long id) {
        Survey s = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found with id: " + id));

        SurveyDto dto = SurveyDto.from(s);
        List<SurveyQuestion> questions = questionRepository.findBySurveyIdOrderBySortOrderAsc(id);
        List<SurveyQuestionDto> qDtos = new ArrayList<>();

        for (SurveyQuestion q : questions) {
            SurveyQuestionDto qDto = SurveyQuestionDto.from(q);
            List<SurveyBranchingRule> rules = branchingRuleRepository.findByQuestionId(q.getId());
            qDto.setBranchingRules(rules.stream().map(SurveyBranchingRuleDto::from).collect(Collectors.toList()));
            qDtos.add(qDto);
        }

        dto.setQuestions(qDtos);
        return dto;
    }

    @Transactional(readOnly = true)
    public SurveyDto getActiveSurveyForCampaign(Long campaignId) {
        if (campaignId == null) return null;
        return surveyRepository.findByCampaignIdAndActiveTrue(campaignId)
                .map(s -> getSurveyWithDetails(s.getId()))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public SurveyDto getLatestActiveSurvey() {
        return surveyRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .map(s -> getSurveyWithDetails(s.getId()))
                .orElse(null);
    }

    @Transactional
    public SurveyDto createSurvey(SurveyDto dto) {
        Survey s = new Survey();
        s.setCampaignId(dto.getCampaignId());
        s.setTitle(dto.getTitle().trim());
        s.setDescription(dto.getDescription());
        s.setMaxResponsesQuota(dto.getMaxResponsesQuota() != null ? dto.getMaxResponsesQuota() : 1000);
        s.setActive(dto.isActive());

        Survey saved = surveyRepository.save(s);

        if (dto.getQuestions() != null) {
            int order = 0;
            for (SurveyQuestionDto qDto : dto.getQuestions()) {
                SurveyQuestion q = new SurveyQuestion();
                q.setSurveyId(saved.getId());
                q.setQuestionText(qDto.getQuestionText().trim());
                q.setQuestionType(qDto.getQuestionType() != null ? qDto.getQuestionType() : "SINGLE_CHOICE");
                q.setOptionsJson(qDto.getOptionsJson());
                q.setSortOrder(order++);
                q.setRequired(qDto.isRequired());

                SurveyQuestion savedQ = questionRepository.save(q);

                if (qDto.getBranchingRules() != null) {
                    for (SurveyBranchingRuleDto rDto : qDto.getBranchingRules()) {
                        SurveyBranchingRule rule = new SurveyBranchingRule();
                        rule.setQuestionId(savedQ.getId());
                        rule.setConditionValue(rDto.getConditionValue());
                        rule.setNextQuestionId(rDto.getNextQuestionId());
                        rule.setTerminateSurvey(rDto.isTerminateSurvey());
                        branchingRuleRepository.save(rule);
                    }
                }
            }
        }

        log.info("Created survey id {} with title: {}", saved.getId(), saved.getTitle());
        return getSurveyWithDetails(saved.getId());
    }

    @Transactional
    public SurveyResponseDto submitResponse(Long surveyId, SurveyResponseSubmitRequest req) {
        Survey s = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found with id: " + surveyId));

        if (!s.isActive()) {
            throw new BadRequestException("Khảo sát này hiện đã đóng hoặc tạm dừng");
        }

        if (s.getMaxResponsesQuota() != null && s.getCurrentResponsesCount() >= s.getMaxResponsesQuota()) {
            throw new BadRequestException("Khảo sát đã đạt định ngạch phản hồi tối đa (" + s.getMaxResponsesQuota() + ")");
        }

        String answersJson;
        try {
            answersJson = objectMapper.writeValueAsString(req.getAnswers());
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Dữ liệu câu trả lời khảo sát không đúng định dạng JSON");
        }

        SurveyResponse response = new SurveyResponse();
        response.setSurveyId(surveyId);
        response.setLocationId(req.getLocationId());
        response.setClientMac(req.getClientMac().trim().toLowerCase());
        response.setAnswersJson(answersJson);
        response.setSubmittedAt(Instant.now());

        SurveyResponse saved = responseRepository.save(response);

        s.setCurrentResponsesCount(s.getCurrentResponsesCount() + 1);
        surveyRepository.save(s);

        log.info("Recorded response id {} for survey id {} (total: {}/{})",
                saved.getId(), surveyId, s.getCurrentResponsesCount(), s.getMaxResponsesQuota());
        return SurveyResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<SurveyResponseDto> getResponses(Long surveyId, Long locationId, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<SurveyResponse> p;
        if (locationId != null && locationId > 0) {
            p = responseRepository.findBySurveyIdAndLocationId(surveyId, locationId, pr);
        } else {
            p = responseRepository.findBySurveyId(surveyId, pr);
        }

        Map<Long, String> locationNameMap = new HashMap<>();
        for (WifiLocation loc : locationRepository.findAll()) {
            locationNameMap.put(loc.getId(), loc.getName());
        }

        return PageResponse.from(p.map(r -> {
            SurveyResponseDto dto = SurveyResponseDto.from(r);
            if (r.getLocationId() != null) {
                dto.setLocationName(locationNameMap.getOrDefault(r.getLocationId(), "Chung hệ thống"));
            } else {
                dto.setLocationName("Chung hệ thống");
            }
            return dto;
        }));
    }

    @Transactional(readOnly = true)
    public PageResponse<SurveyResponseDto> getResponses(Long surveyId, int page, int size) {
        return getResponses(surveyId, null, page, size);
    }

    @Transactional(readOnly = true)
    public SurveyAnalyticsDto getSurveyAnalytics(Long surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found with id: " + surveyId));

        List<SurveyQuestion> questions = questionRepository.findBySurveyIdOrderBySortOrderAsc(surveyId);
        List<SurveyResponse> responses = responseRepository.findBySurveyId(surveyId, PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "submittedAt"))).getContent();

        Map<Long, String> locationNameMap = new HashMap<>();
        for (WifiLocation loc : locationRepository.findAll()) {
            locationNameMap.put(loc.getId(), loc.getName());
        }

        SurveyAnalyticsDto analytics = new SurveyAnalyticsDto();
        analytics.setSurveyId(survey.getId());
        analytics.setSurveyTitle(survey.getTitle());
        analytics.setTotalResponses(responses.size());
        analytics.setMaxResponsesQuota(survey.getMaxResponsesQuota() != null ? survey.getMaxResponsesQuota() : 1000);
        double completionPct = analytics.getMaxResponsesQuota() > 0
                ? Math.min(100.0, (responses.size() * 100.0 / analytics.getMaxResponsesQuota()))
                : 0.0;
        analytics.setCompletionRatePercent(Math.round(completionPct * 10.0) / 10.0);

        // Location distribution
        Map<String, Long> locDist = new LinkedHashMap<>();
        for (SurveyResponse r : responses) {
            String locName = r.getLocationId() != null ? locationNameMap.getOrDefault(r.getLocationId(), "Chung hệ thống") : "Chung hệ thống";
            locDist.put(locName, locDist.getOrDefault(locName, 0L) + 1);
        }
        analytics.setLocationDistribution(locDist);

        // Question analytics
        List<SurveyAnalyticsDto.QuestionAnalyticsDto> qList = new ArrayList<>();
        double overallRatingSum = 0.0;
        long overallRatingCount = 0;

        for (SurveyQuestion q : questions) {
            SurveyAnalyticsDto.QuestionAnalyticsDto qa = new SurveyAnalyticsDto.QuestionAnalyticsDto();
            qa.setQuestionId(q.getId());
            qa.setQuestionText(q.getQuestionText());
            qa.setQuestionType(q.getQuestionType() != null ? q.getQuestionType() : "SINGLE_CHOICE");
            qa.setRequired(q.isRequired());

            List<String> options = new ArrayList<>();
            if (q.getOptionsJson() != null && !q.getOptionsJson().isBlank()) {
                try {
                    options = objectMapper.readValue(q.getOptionsJson(), new TypeReference<List<String>>() {});
                } catch (Exception ignored) {}
            }

            Map<String, Long> optionCounts = new LinkedHashMap<>();
            for (String opt : options) {
                optionCounts.put(opt, 0L);
            }

            Map<Integer, Long> ratingCounts = new LinkedHashMap<>();
            for (int i = 1; i <= 5; i++) ratingCounts.put(i, 0L);

            List<SurveyAnalyticsDto.TextFeedbackItemDto> textList = new ArrayList<>();
            long answeredCount = 0;
            double ratingSum = 0.0;
            long ratingNum = 0;

            for (SurveyResponse r : responses) {
                Map<String, Object> ansMap = parseAnswersJson(r.getAnswersJson());
                Object ansVal = ansMap.get(q.getId().toString());
                if (ansVal == null) continue;

                answeredCount++;
                String locName = r.getLocationId() != null ? locationNameMap.getOrDefault(r.getLocationId(), "Chung hệ thống") : "Chung hệ thống";

                if ("TEXT".equalsIgnoreCase(qa.getQuestionType())) {
                    String strVal = ansVal.toString().trim();
                    if (!strVal.isEmpty()) {
                        textList.add(new SurveyAnalyticsDto.TextFeedbackItemDto(
                                r.getId(), locName, r.getClientMac(), r.getSubmittedAt(), strVal
                        ));
                    }
                } else if ("RATING".equalsIgnoreCase(qa.getQuestionType())) {
                    try {
                        int star = Integer.parseInt(ansVal.toString().trim());
                        if (star >= 1 && star <= 5) {
                            ratingCounts.put(star, ratingCounts.getOrDefault(star, 0L) + 1);
                            ratingSum += star;
                            ratingNum++;
                            overallRatingSum += star;
                            overallRatingCount++;
                        }
                    } catch (Exception ignored) {}
                } else if ("MULTI_CHOICE".equalsIgnoreCase(qa.getQuestionType())) {
                    List<?> selectedList = (ansVal instanceof List<?>) ? (List<?>) ansVal : List.of(ansVal.toString());
                    for (Object item : selectedList) {
                        String optStr = item.toString().trim();
                        optionCounts.put(optStr, optionCounts.getOrDefault(optStr, 0L) + 1);
                    }
                } else {
                    String optStr = ansVal.toString().trim();
                    optionCounts.put(optStr, optionCounts.getOrDefault(optStr, 0L) + 1);
                }
            }

            qa.setTotalAnswersCount(answeredCount);
            qa.setOptionCounts(optionCounts);

            Map<String, Double> optionPercentages = new LinkedHashMap<>();
            if (answeredCount > 0) {
                for (Map.Entry<String, Long> entry : optionCounts.entrySet()) {
                    double pct = Math.round((entry.getValue() * 100.0 / answeredCount) * 10.0) / 10.0;
                    optionPercentages.put(entry.getKey(), pct);
                }
            }
            qa.setOptionPercentages(optionPercentages);

            if (ratingNum > 0) {
                qa.setAverageRating(Math.round((ratingSum / ratingNum) * 100.0) / 100.0);
                qa.setRatingCounts(ratingCounts);
            }
            qa.setTextResponses(textList);
            qList.add(qa);
        }

        analytics.setQuestionAnalytics(qList);
        if (overallRatingCount > 0) {
            analytics.setAverageRating(Math.round((overallRatingSum / overallRatingCount) * 100.0) / 100.0);
        }
        return analytics;
    }

    private Map<String, Object> parseAnswersJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportResponsesExcel(Long surveyId) {
        Survey survey = surveyRepository.findById(surveyId)
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found with id: " + surveyId));
        List<SurveyQuestion> questions = questionRepository.findBySurveyIdOrderBySortOrderAsc(surveyId);
        List<SurveyResponse> responses = responseRepository.findBySurveyId(surveyId, PageRequest.of(0, 10000, Sort.by(Sort.Direction.ASC, "submittedAt"))).getContent();
        SurveyAnalyticsDto analytics = getSurveyAnalytics(surveyId);

        return surveyExcelService.generateSurveyExcel(survey, questions, responses, analytics);
    }

    @Transactional
    public SurveyDto toggleSurvey(Long id) {
        Survey s = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found with id: " + id));
        s.setActive(!s.isActive());
        Survey saved = surveyRepository.save(s);
        log.info("Toggled survey id {} active status to {}", id, saved.isActive());
        return getSurveyWithDetails(saved.getId());
    }

    @Transactional
    public void deleteSurvey(Long id) {
        Survey s = surveyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Survey not found with id: " + id));

        // Delete associated responses, rules, questions
        responseRepository.deleteAll(responseRepository.findBySurveyId(id, PageRequest.of(0, 10000)).getContent());
        List<SurveyQuestion> questions = questionRepository.findBySurveyIdOrderBySortOrderAsc(id);
        for (SurveyQuestion q : questions) {
            branchingRuleRepository.deleteAll(branchingRuleRepository.findByQuestionId(q.getId()));
        }
        questionRepository.deleteAll(questions);
        surveyRepository.delete(s);
        log.info("Deleted survey id {}", id);
    }

    @Transactional(readOnly = true)
    public String exportResponsesCsv(Long surveyId) {
        if (!surveyRepository.existsById(surveyId)) {
            throw new ResourceNotFoundException("Survey not found with id: " + surveyId);
        }

        List<SurveyResponse> list = responseRepository.findBySurveyId(surveyId, PageRequest.of(0, 10000, Sort.by(Sort.Direction.ASC, "submittedAt"))).getContent();
        StringBuilder sb = new StringBuilder();
        sb.append("STT,ID Phan Hoi,Dia Chi MAC,Thoi Gian Gui,Noi Dung Cau Tra Loi\n");
        int stt = 1;
        for (SurveyResponse r : list) {
            String cleanJson = r.getAnswersJson() != null ? r.getAnswersJson().replace("\"", "\"\"") : "";
            sb.append(stt++).append(",")
              .append(r.getId()).append(",")
              .append(r.getClientMac()).append(",")
              .append(r.getSubmittedAt()).append(",")
              .append("\"").append(cleanJson).append("\"\n");
        }
        return sb.toString();
    }
}

