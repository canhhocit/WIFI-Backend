package com.wificaptive.portal.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.wificaptive.campaigns.dto.CampaignDto;
import com.wificaptive.locations.dto.LocationDto;
import com.wificaptive.portal.dto.PortalInitResponse;
import com.wificaptive.portal.template.dto.PortalPreviewDataResponse;
import com.wificaptive.portal.template.dto.PortalPreviewLinkRequest;
import com.wificaptive.portal.template.dto.PortalPreviewLinkResponse;
import com.wificaptive.surveys.dto.SurveyDto;
import com.wificaptive.surveys.dto.SurveyQuestionDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;

@Service
public class PortalPreviewTokenService {
    private static final int DEFAULT_EXPIRATION_MINUTES = 60;
    private static final int MAX_EXPIRATION_MINUTES = 120;
    private static final String TOKEN_PURPOSE = "portal-preview";

    private final SecretKey signingKey;
    private final ObjectMapper objectMapper;

    public PortalPreviewTokenService(
            @Value("${app.jwt.secret}") String jwtSecret,
            ObjectMapper objectMapper) {
        this.signingKey = deriveSigningKey(jwtSecret);
        this.objectMapper = objectMapper;
    }

    public PortalPreviewLinkResponse createLink(PortalPreviewLinkRequest request) {
        if (request == null || request.getPreviewData() == null || request.getPreviewData().getLocation() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu dữ liệu trang cần xem trước");
        }

        int expirationMinutes = request.getExpiresInMinutes() == null
                ? DEFAULT_EXPIRATION_MINUTES
                : request.getExpiresInMinutes();
        if (expirationMinutes < 5 || expirationMinutes > MAX_EXPIRATION_MINUTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thời hạn liên kết phải từ 5 đến 120 phút");
        }

        PortalInitResponse previewData = sanitize(request.getPreviewData());
        final String previewJson;
        try {
            previewJson = objectMapper.writeValueAsString(previewData);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Không thể tuần tự hóa dữ liệu xem trước", exception);
        }
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationMinutes * 60L);
        String token = Jwts.builder()
                .claim("purpose", TOKEN_PURPOSE)
                .claim("previewData", previewJson)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        return new PortalPreviewLinkResponse(token, expiresAt);
    }

    public PortalPreviewDataResponse readLink(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!TOKEN_PURPOSE.equals(claims.get("purpose", String.class))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Liên kết xem trước không hợp lệ");
            }
            PortalInitResponse previewData = objectMapper.readValue(claims.get("previewData", String.class), PortalInitResponse.class);
            return new PortalPreviewDataResponse(previewData, claims.getExpiration().toInstant());
        } catch (ExpiredJwtException exception) {
            throw new ResponseStatusException(HttpStatus.GONE, "Liên kết xem trước đã hết hạn");
        } catch (JwtException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Liên kết xem trước không hợp lệ");
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu xem trước không hợp lệ");
        }
    }

    private PortalInitResponse sanitize(PortalInitResponse source) {
        PortalInitResponse safe = objectMapper.convertValue(source, PortalInitResponse.class);
        safe.setClientIp(null);
        safe.setClientMacHint(null);
        safe.setGatewayNotice(null);
        safe.setMockGateway(true);
        safe.setMacRemembered(false);
        safe.setOtpRequired(false);

        LocationDto location = safe.getLocation();
        if (location != null) {
            location.setId(0L);
            location.setDescription(null);
            location.setAdminUnitId(null);
            location.setAdminUnitName(null);
            location.setLatitude(null);
            location.setLongitude(null);
            location.setStatus(null);
            location.setStatusReason(null);
            location.setIspName(null);
            location.setContactName(null);
            location.setContactPhone(null);
            location.setCircuitCode(null);
            location.setCreatedAt(null);
            location.setUpdatedAt(null);
        }

        CampaignDto campaign = safe.getActiveCampaign();
        if (campaign != null) {
            campaign.setTargetAdminUnitId(null);
            campaign.setTargetLocationId(null);
            campaign.setCreatedBy(null);
            campaign.setApprovedBy(null);
            campaign.setApprovedAt(null);
            campaign.setRejectReason(null);
            campaign.setCreatedAt(null);
            campaign.setUpdatedAt(null);
        }

        SurveyDto survey = safe.getActiveSurvey();
        if (survey != null) {
            survey.setCampaignId(null);
            survey.setMaxResponsesQuota(null);
            survey.setCurrentResponsesCount(0);
            survey.setCreatedAt(null);
            survey.setUpdatedAt(null);
            if (survey.getQuestions() != null) {
                for (SurveyQuestionDto question : survey.getQuestions()) {
                    question.setSurveyId(null);
                }
            }
        }

        if (safe.getPortalTemplate() != null) {
            safe.getPortalTemplate().setId(null);
            safe.getPortalTemplate().setActive(false);
        }
        return safe;
    }

    private SecretKey deriveSigningKey(String jwtSecret) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(("portal-preview:" + jwtSecret).getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể khởi tạo khóa ký preview", exception);
        }
    }
}
