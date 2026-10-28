package com.wificaptive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wificaptive.authentication.dto.LoginRequest;
import com.wificaptive.portal.dto.GuestLoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CaptivePortalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testPortalInit_ReturnsActiveLocation() throws Exception {
        mockMvc.perform(get("/api/portal/init?location=circle-k-central"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.location.slug").value("circle-k-central"))
                .andExpect(jsonPath("$.data.location.name").value("Circle K - Central Station"))
                .andExpect(jsonPath("$.data.mockGateway").value(true));
    }

    @Test
    void testGuestLogin_RequiresConsent() throws Exception {
        GuestLoginRequest request = new GuestLoginRequest();
        request.setLocationSlug("circle-k-central");
        request.setConsentGiven(false); // No consent

        mockMvc.perform(post("/api/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testGuestLogin_SuccessEndToEnd() throws Exception {
        GuestLoginRequest request = new GuestLoginRequest();
        request.setLocationSlug("circle-k-central");
        request.setMacAddress("00:11:22:33:44:99");
        request.setGuestName("Test Traveler");
        request.setGuestContact("traveler@example.com");
        request.setConsentGiven(true);

        MvcResult result = mockMvc.perform(post("/api/portal/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.data.simulated").value(true))
                .andExpect(jsonPath("$.data.sessionToken", notNullValue()))
                .andReturn();

        String token = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.sessionToken");

        // Now query session status
        mockMvc.perform(get("/api/portal/session/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AUTHORIZED"))
                .andExpect(jsonPath("$.data.macAddress").value("00:11:22:33:44:99"));
    }

    @Test
    void testAdminEndpoints_RequireAuthentication() throws Exception {
        // Unauthenticated access to admin sessions should return 401 or 403
        mockMvc.perform(get("/api/admin/sessions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminLogin_AndAccessProtectedEndpoint() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andReturn();

        String jwtToken = com.jayway.jsonpath.JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.token");

        // Access protected sessions endpoint with Bearer token
        mockMvc.perform(get("/api/admin/sessions")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", notNullValue()));

        // Access protected analytics overview endpoint
        mockMvc.perform(get("/api/admin/analytics/overview")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalSessions", greaterThanOrEqualTo(0)));
    }
}
