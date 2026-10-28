package com.wificaptive.surveys;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.surveys.dto.SurveyDto;
import com.wificaptive.surveys.dto.SurveyResponseDto;
import com.wificaptive.surveys.dto.SurveyResponseSubmitRequest;
import com.wificaptive.surveys.entity.Survey;
import com.wificaptive.surveys.entity.SurveyResponse;
import com.wificaptive.surveys.repository.SurveyBranchingRuleRepository;
import com.wificaptive.surveys.repository.SurveyQuestionRepository;
import com.wificaptive.surveys.repository.SurveyRepository;
import com.wificaptive.surveys.repository.SurveyResponseRepository;
import com.wificaptive.surveys.service.SurveyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

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

    private SurveyService surveyService;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        surveyService = new SurveyService(
                surveyRepository,
                questionRepository,
                branchingRuleRepository,
                responseRepository,
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
}

