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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SurveyService {

    private static final Logger log = LoggerFactory.getLogger(SurveyService.class);

    private final SurveyRepository surveyRepository;
    private final SurveyQuestionRepository questionRepository;
    private final SurveyBranchingRuleRepository branchingRuleRepository;
    private final SurveyResponseRepository responseRepository;
    private final ObjectMapper objectMapper;

    public SurveyService(
            SurveyRepository surveyRepository,
            SurveyQuestionRepository questionRepository,
            SurveyBranchingRuleRepository branchingRuleRepository,
            SurveyResponseRepository responseRepository,
            ObjectMapper objectMapper) {
        this.surveyRepository = surveyRepository;
        this.questionRepository = questionRepository;
        this.branchingRuleRepository = branchingRuleRepository;
        this.responseRepository = responseRepository;
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
    public PageResponse<SurveyResponseDto> getResponses(Long surveyId, int page, int size) {
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<SurveyResponse> p = responseRepository.findBySurveyId(surveyId, pr);
        return PageResponse.from(p.map(SurveyResponseDto::from));
    }
}

