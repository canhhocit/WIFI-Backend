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

    public CampaignService(CampaignRepository campaignRepository, CampaignApprovalRepository approvalRepository) {
        this.campaignRepository = campaignRepository;
        this.approvalRepository = approvalRepository;
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
        c.setBannerUrl(req.getBannerUrl());
        c.setVideoUrl(req.getVideoUrl());
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

    @Transactional(readOnly = true)
    public Campaign getActiveCampaignForLocation(Long locationId, Long adminUnitId) {
        Instant now = Instant.now();
        List<Campaign> approved = campaignRepository.findActiveApprovedCampaigns(now);
        for (Campaign c : approved) {
            // Check exact location target
            if (c.getTargetLocationId() != null && locationId != null) {
                if (c.getTargetLocationId().equals(locationId)) {
                    return c;
                }
            } else if (c.getTargetAdminUnitId() != null && adminUnitId != null) {
                // Check admin unit target
                if (c.getTargetAdminUnitId().equals(adminUnitId)) {
                    return c;
                }
            } else if (c.getTargetLocationId() == null && c.getTargetAdminUnitId() == null) {
                // City-wide campaign
                return c;
            }
        }
        return null;
    }
}

