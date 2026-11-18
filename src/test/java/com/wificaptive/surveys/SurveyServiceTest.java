package com.wificaptive.surveys;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.surveys.dto.SurveyResponseDto;
import com.wificaptive.surveys.dto.SurveyResponseSubmitRequest;
import com.wificaptive.surveys.dto.SurveyBranchingRuleDto;
import com.wificaptive.surveys.dto.SurveyDto;
import com.wificaptive.surveys.dto.SurveyQuestionDto;
import com.wificaptive.surveys.entity.Survey;
import com.wificaptive.surveys.entity.SurveyBranchingRule;
import com.wificaptive.surveys.entity.SurveyQuestion;
import com.wificaptive.surveys.entity.SurveyResponse;
import com.wificaptive.surveys.repository.SurveyBranchingRuleRepository;
import com.wificaptive.surveys.repository.SurveyQuestionRepository;
import com.wificaptive.surveys.repository.SurveyRepository;
import com.wificaptive.surveys.repository.SurveyResponseRepository;
import com.wificaptive.locations.repository.WifiLocationRepository;
import com.wificaptive.surveys.service.SurveyExcelService;
import com.wificaptive.surveys.service.SurveyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SurveyServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private SurveyQuestionRepository questionRepository;

    @Mock
    private SurveyBranchingRuleRepository branchingRuleRepository;

    @Mock
    private SurveyResponseRepository responseRepository;

    @Mock
    private WifiLocationRepository locationRepository;

    @Mock
    private SurveyExcelService surveyExcelService;

    private SurveyService surveyService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        surveyService = new SurveyService(
                surveyRepository,
                questionRepository,
                branchingRuleRepository,
                responseRepository,
                locationRepository,
                surveyExcelService,
                objectMapper
        );
    }

    @Test
    void testSubmitSurveyResponseEnforcesQuota() {
        Survey s = new Survey();
        s.setId(1L);
        s.setTitle("Khảo sát mức độ hài lòng về chất lượng Wi-Fi Nhà văn hóa");
        s.setActive(true);
        s.setMaxResponsesQuota(100);
        s.setCurrentResponsesCount(100); // Already reached quota

        when(surveyRepository.findById(1L)).thenReturn(Optional.of(s));

        SurveyResponseSubmitRequest req = new SurveyResponseSubmitRequest();
        req.setClientMac("00:11:22:33:44:55");
        req.setAnswers(Map.of("q1", "Rất hài lòng"));

        assertThrows(BadRequestException.class, () -> surveyService.submitResponse(1L, req));
    }

    @Test
    void createSurveyMapsDraftBranchTargetsToSavedQuestionIds() {
        Survey survey = new Survey();
        survey.setId(7L);
        List<SurveyQuestion> createdQuestions = new ArrayList<>();
        when(surveyRepository.save(any(Survey.class))).thenAnswer(invocation -> {
            Survey saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });
        when(surveyRepository.findById(7L)).thenReturn(Optional.of(survey));
        when(questionRepository.save(any(SurveyQuestion.class))).thenAnswer(invocation -> {
            SurveyQuestion question = invocation.getArgument(0);
            question.setId(100L + question.getSortOrder());
            createdQuestions.add(question);
            return question;
        });
        when(questionRepository.findBySurveyIdOrderBySortOrderAsc(7L)).thenReturn(createdQuestions);
        when(branchingRuleRepository.findByQuestionId(any())).thenReturn(List.of());

        SurveyQuestionDto first = question(-1L, "Câu đầu", 0);
        SurveyBranchingRuleDto branch = new SurveyBranchingRuleDto();
        branch.setConditionValue("Không hài lòng");
        branch.setNextQuestionId(-2L);
        first.setBranchingRules(List.of(branch));
        SurveyQuestionDto second = question(-2L, "Câu tiếp", 1);

        SurveyDto request = new SurveyDto();
        request.setTitle("Khảo sát thử");
        request.setQuestions(List.of(first, second));

        surveyService.createSurvey(request);

        org.mockito.ArgumentCaptor<SurveyBranchingRule> ruleCaptor = org.mockito.ArgumentCaptor.forClass(SurveyBranchingRule.class);
        verify(branchingRuleRepository).save(ruleCaptor.capture());
        assertEquals(100L, ruleCaptor.getValue().getQuestionId());
        assertEquals(101L, ruleCaptor.getValue().getNextQuestionId());
        assertTrue(createdQuestions.get(0).isRequired());
    }

    @Test
    void createSurveyRejectsBackwardBranchTargets() {
        Survey survey = new Survey();
        survey.setId(8L);
        when(surveyRepository.save(any(Survey.class))).thenAnswer(invocation -> {
            Survey saved = invocation.getArgument(0);
            saved.setId(8L);
            return saved;
        });
        when(questionRepository.save(any(SurveyQuestion.class))).thenAnswer(invocation -> {
            SurveyQuestion question = invocation.getArgument(0);
            question.setId(200L + question.getSortOrder());
            return question;
        });

        SurveyQuestionDto first = question(-1L, "Câu đầu", 0);
        SurveyQuestionDto second = question(-2L, "Câu sau", 1);
        SurveyBranchingRuleDto branch = new SurveyBranchingRuleDto();
        branch.setConditionValue("Có");
        branch.setNextQuestionId(-1L);
        second.setBranchingRules(List.of(branch));

        SurveyDto request = new SurveyDto();
        request.setTitle("Khảo sát thử");
        request.setQuestions(List.of(first, second));

        assertThrows(BadRequestException.class, () -> surveyService.createSurvey(request));
        verify(branchingRuleRepository, never()).save(any(SurveyBranchingRule.class));
    }

    private SurveyQuestionDto question(Long id, String text, int order) {
        SurveyQuestionDto question = new SurveyQuestionDto();
        question.setId(id);
        question.setQuestionText(text);
        question.setQuestionType("SINGLE_CHOICE");
        question.setSortOrder(order);
        question.setRequired(false);
        return question;
    }

    @Test
    void testSubmitSurveyResponseSuccess() {
        Survey s = new Survey();
        s.setId(2L);
        s.setTitle("Khảo sát chuyển đổi số");
        s.setActive(true);
        s.setMaxResponsesQuota(50);
        s.setCurrentResponsesCount(10);

        when(surveyRepository.findById(2L)).thenReturn(Optional.of(s));

        SurveyResponse mockResp = new SurveyResponse();
        mockResp.setId(101L);
        mockResp.setSurveyId(2L);
        mockResp.setClientMac("00:11:22:33:44:55");
        mockResp.setAnswersJson("{\"q1\":\"Rất tốt\"}");

        when(responseRepository.save(any(SurveyResponse.class))).thenReturn(mockResp);

        SurveyResponseSubmitRequest req = new SurveyResponseSubmitRequest();
        req.setClientMac("00:11:22:33:44:55");
        req.setAnswers(Map.of("q1", "Rất tốt"));

        SurveyResponseDto result = surveyService.submitResponse(2L, req);
        assertNotNull(result);
        assertEquals(11, s.getCurrentResponsesCount());
        verify(surveyRepository, times(1)).save(s);
    }

    @Test
    void testGetSurveyAnalyticsComputesBreakdownAndPercentage() {
        Survey s = new Survey();
        s.setId(5L);
        s.setTitle("Khảo sát tiện ích số TDP 1");
        s.setMaxResponsesQuota(200);

        when(surveyRepository.findById(5L)).thenReturn(Optional.of(s));

        SurveyQuestion q1 = new SurveyQuestion();
        q1.setId(10L);
        q1.setSurveyId(5L);
        q1.setQuestionText("Chất lượng Wi-Fi");
        q1.setQuestionType("SINGLE_CHOICE");
        q1.setOptionsJson("[\"Tốt\",\"Kém\"]");

        when(questionRepository.findBySurveyIdOrderBySortOrderAsc(5L)).thenReturn(java.util.List.of(q1));

        SurveyResponse r1 = new SurveyResponse();
        r1.setId(1L);
        r1.setSurveyId(5L);
        r1.setAnswersJson("{\"10\":\"Tốt\"}");

        SurveyResponse r2 = new SurveyResponse();
        r2.setId(2L);
        r2.setAnswersJson("{\"10\":\"Tốt\"}");

        SurveyResponse r3 = new SurveyResponse();
        r3.setId(3L);
        r3.setAnswersJson("{\"10\":\"Kém\"}");

        when(responseRepository.findBySurveyId(eq(5L), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(r1, r2, r3)));

        var analytics = surveyService.getSurveyAnalytics(5L);
        assertNotNull(analytics);
        assertEquals(3, analytics.getTotalResponses());
        assertEquals(200, analytics.getMaxResponsesQuota());
        assertEquals(1.5, analytics.getCompletionRatePercent());
        assertEquals(1, analytics.getQuestionAnalytics().size());

        var qa = analytics.getQuestionAnalytics().get(0);
        assertEquals(2L, qa.getOptionCounts().get("Tốt"));
        assertEquals(1L, qa.getOptionCounts().get("Kém"));
        assertEquals(66.7, qa.getOptionPercentages().get("Tốt"));
        assertEquals(33.3, qa.getOptionPercentages().get("Kém"));
    }
}

