package com.wificaptive.campaigns.service;

import com.wificaptive.campaigns.dto.ApproveCampaignRequest;
import com.wificaptive.campaigns.dto.CampaignDto;
import com.wificaptive.campaigns.dto.CreateCampaignRequest;
import com.wificaptive.campaigns.entity.Campaign;
import com.wificaptive.campaigns.entity.CampaignApproval;
import com.wificaptive.campaigns.repository.CampaignApprovalRepository;
import com.wificaptive.campaigns.repository.CampaignRepository;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.surveys.entity.Survey;
import com.wificaptive.surveys.repository.SurveyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepository campaignRepository;
    private final CampaignApprovalRepository approvalRepository;
    private final SurveyRepository surveyRepository;

    public CampaignService(CampaignRepository campaignRepository,
                           CampaignApprovalRepository approvalRepository,
                           SurveyRepository surveyRepository) {
        this.campaignRepository = campaignRepository;
        this.approvalRepository = approvalRepository;
        this.surveyRepository = surveyRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CampaignDto> getCampaigns(String status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Campaign> p;
        if (status != null && !status.isBlank()) {
            p = campaignRepository.findByStatus(status.trim().toUpperCase(), pageRequest);
        } else {
            p = campaignRepository.findAll(pageRequest);
        }
        return PageResponse.from(p.map(CampaignDto::from));
    }

    @Transactional(readOnly = true)
    public CampaignDto getCampaignById(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));
        return CampaignDto.from(campaign);
    }

    @Transactional
    public CampaignDto createCampaign(CreateCampaignRequest req, String username) {
        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw new BadRequestException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu");
        }

        Campaign c = new Campaign();
        c.setTitle(req.getTitle().trim());
        c.setDescription(req.getDescription());
        applyMediaAndGallery(c, req);
        c.setActionUrl(req.getActionUrl());
        c.setTargetAdminUnitId(req.getTargetAdminUnitId());
        c.setTargetLocationId(req.getTargetLocationId());
        c.setPriority(req.getPriority());
        c.setStartTime(req.getStartTime());
        c.setEndTime(req.getEndTime());
        c.setStatus("DRAFT");
        c.setCreatedBy(username != null ? username : "admin");

        Campaign saved = campaignRepository.save(c);
        log.info("Created new campaign id: {} in DRAFT status by {}", saved.getId(), c.getCreatedBy());
        return CampaignDto.from(saved);
    }

    @Transactional
    public CampaignDto submitForApproval(Long id, String username) {
        Campaign c = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        if (!"DRAFT".equals(c.getStatus()) && !"REJECTED".equals(c.getStatus())) {
            throw new BadRequestException("Chỉ chiến dịch ở trạng thái DRAFT hoặc REJECTED mới có thể gửi phê duyệt");
        }

        c.setStatus("PENDING_APPROVAL");
        c.setRejectReason(null);
        Campaign saved = campaignRepository.save(c);

        approvalRepository.save(new CampaignApproval(
                saved.getId(),
                "SUBMIT",
                username != null ? username : "creator",
                "Trình duyệt chiến dịch truyền thông / chính sách số"
        ));

        log.info("Campaign id {} submitted for approval by {}", id, username);
        return CampaignDto.from(saved);
    }

    @Transactional
    public CampaignDto approveCampaign(Long id, ApproveCampaignRequest req, String approver) {
        Campaign c = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        if (!"PENDING_APPROVAL".equals(c.getStatus())) {
            throw new BadRequestException("Chỉ có thể phê duyệt chiến dịch đang ở trạng thái PENDING_APPROVAL");
        }

        c.setStatus("APPROVED");
        c.setApprovedBy(approver != null ? approver : "approver");
        c.setApprovedAt(Instant.now());
        Campaign saved = campaignRepository.save(c);

        approvalRepository.save(new CampaignApproval(
                saved.getId(),
                "APPROVE",
                c.getApprovedBy(),
                req != null && req.getComment() != null ? req.getComment() : "Phê duyệt kích hoạt chiến dịch phát sóng"
        ));

        log.info("Campaign id {} approved by {}", id, approver);
        return CampaignDto.from(saved);
    }

    @Transactional
    public CampaignDto rejectCampaign(Long id, ApproveCampaignRequest req, String rejector) {
        Campaign c = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        if (!"PENDING_APPROVAL".equals(c.getStatus())) {
            throw new BadRequestException("Chỉ có thể từ chối chiến dịch đang ở trạng thái PENDING_APPROVAL");
        }

        String reason = req != null && req.getRejectReason() != null ? req.getRejectReason() : "Nội dung chưa đạt tiêu chuẩn phê duyệt";
        c.setStatus("REJECTED");
        c.setRejectReason(reason);
        Campaign saved = campaignRepository.save(c);

        approvalRepository.save(new CampaignApproval(
                saved.getId(),
                "REJECT",
                rejector != null ? rejector : "reviewer",
                reason
        ));

        log.info("Campaign id {} rejected by {}: {}", id, rejector, reason);
        return CampaignDto.from(saved);
    }

    @Transactional
    public CampaignDto updateCampaign(Long id, CreateCampaignRequest req, String username) {
        Campaign c = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        if (req.getEndTime().isBefore(req.getStartTime())) {
            throw new BadRequestException("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu");
        }

        c.setTitle(req.getTitle().trim());
        c.setDescription(req.getDescription());
        applyMediaAndGallery(c, req);
        c.setActionUrl(req.getActionUrl());
        c.setTargetAdminUnitId(req.getTargetAdminUnitId());
        c.setTargetLocationId(req.getTargetLocationId());
        c.setPriority(req.getPriority());
        c.setStartTime(req.getStartTime());
        c.setEndTime(req.getEndTime());
        c.setUpdatedAt(Instant.now());

        Campaign saved = campaignRepository.save(c);
        log.info("Campaign id {} updated by {}", id, username);
        return CampaignDto.from(saved);
    }

    @Transactional
    public void deleteCampaign(Long id, String username) {
        Campaign c = campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found with id: " + id));

        // 1. Unlink any surveys associated with this campaign (preserve surveys and responses)
        List<Survey> linkedSurveys = surveyRepository.findByCampaignId(id);
        if (linkedSurveys != null && !linkedSurveys.isEmpty()) {
            for (Survey s : linkedSurveys) {
                s.setCampaignId(null);
            }
            surveyRepository.saveAll(linkedSurveys);
            log.info("Unlinked {} surveys associated with campaign id {}", linkedSurveys.size(), id);
        }

        // 2. Delete approval audit records
        approvalRepository.deleteByCampaignId(id);

        // 3. Delete campaign
        campaignRepository.delete(c);
        log.info("Campaign id {} deleted by {}", id, username);
    }

    @Transactional(readOnly = true)
    public Campaign getActiveCampaignForLocation(Long locationId, Long adminUnitId) {
        Instant now = Instant.now();
        List<Campaign> approved = campaignRepository.findActiveApprovedCampaigns(now);

        // Step 1: Look for exact location target (highest specificity, ordered by priority ASC: P1 > P2...)
        if (locationId != null) {
            for (Campaign c : approved) {
                if (locationId.equals(c.getTargetLocationId())) {
                    return c;
                }
            }
        }

        // Step 2: Look for administrative unit target across 3 tiers (Cấp 3 Phường/Xã -> Cấp 2 Quận/Huyện -> Cấp 1 Thành phố)
        if (adminUnitId != null) {
            java.util.List<Long> unitHierarchy = resolveUnitAndAncestorIds(adminUnitId);
            for (Long targetUnitId : unitHierarchy) {
                for (Campaign c : approved) {
                    if (targetUnitId.equals(c.getTargetAdminUnitId()) && c.getTargetLocationId() == null) {
                        return c;
                    }
                }
            }
        }

        // Step 3: City-wide broadcast campaign (targetLocationId == null && targetAdminUnitId == null)
        for (Campaign c : approved) {
            if (c.getTargetLocationId() == null && c.getTargetAdminUnitId() == null) {
                return c;
            }
        }

        return null;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.wificaptive.geography.repository.AdministrativeUnitRepository administrativeUnitRepository;

    public void setAdministrativeUnitRepository(com.wificaptive.geography.repository.AdministrativeUnitRepository administrativeUnitRepository) {
        this.administrativeUnitRepository = administrativeUnitRepository;
    }

    private java.util.List<Long> resolveUnitAndAncestorIds(Long adminUnitId) {
        java.util.List<Long> chain = new java.util.ArrayList<>();
        chain.add(adminUnitId);
        if (administrativeUnitRepository != null) {
            com.wificaptive.geography.entity.AdministrativeUnit current =
                    administrativeUnitRepository.findById(adminUnitId).orElse(null);
            int depth = 0;
            while (current != null && current.getParent() != null && depth < 5) {
                Long parentId = current.getParent().getId();
                if (parentId != null && !chain.contains(parentId)) {
                    chain.add(parentId);
                }
                current = current.getParent();
                depth++;
            }
        }
        return chain;
    }

    private void applyMediaAndGallery(Campaign c, CreateCampaignRequest req) {
        c.setVideoUrl(req.getVideoUrl());

        List<String> gallery = req.getGalleryUrls();
        String primaryBanner = req.getBannerUrl();

        if (gallery != null && !gallery.isEmpty()) {
            if (primaryBanner == null || primaryBanner.isBlank()) {
                primaryBanner = gallery.get(0);
            }
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                c.setGalleryUrls(mapper.writeValueAsString(gallery));
            } catch (Exception e) {
                c.setGalleryUrls(String.join(",", gallery));
            }
        } else if (primaryBanner != null && !primaryBanner.isBlank()) {
            c.setGalleryUrls("[\"" + primaryBanner + "\"]");
        } else {
            c.setGalleryUrls(null);
        }
        c.setBannerUrl(primaryBanner);
    }
}

