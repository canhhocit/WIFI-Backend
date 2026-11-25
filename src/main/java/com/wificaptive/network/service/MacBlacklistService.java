package com.wificaptive.network.service;

import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.network.dto.CreateBlacklistRequest;
import com.wificaptive.network.dto.MacBlacklistDto;
import com.wificaptive.network.entity.MacBlacklist;
import com.wificaptive.network.repository.MacBlacklistRepository;
import com.wificaptive.sessions.service.GuestSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class MacBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(MacBlacklistService.class);

    private final MacBlacklistRepository blacklistRepository;
    private final GuestSessionService guestSessionService;

    public MacBlacklistService(MacBlacklistRepository blacklistRepository) {
        this(blacklistRepository, null);
    }

    @Autowired
    public MacBlacklistService(
            MacBlacklistRepository blacklistRepository,
            @Autowired(required = false) GuestSessionService guestSessionService) {
        this.blacklistRepository = blacklistRepository;
        this.guestSessionService = guestSessionService;
    }

    @Transactional(readOnly = true)
    public boolean isBlacklisted(String macAddress) {
        if (macAddress == null || macAddress.isBlank()) {
            return false;
        }
        return blacklistRepository.existsByMacAddressIgnoreCaseAndActiveTrue(macAddress.trim());
    }

    @Transactional(readOnly = true)
    public Page<MacBlacklistDto> getBlacklist(Boolean active, Pageable pageable) {
        Page<MacBlacklist> page = (active != null)
                ? blacklistRepository.findByActive(active, pageable)
                : blacklistRepository.findAll(pageable);
        return page.map(MacBlacklistDto::fromEntity);
    }

    public MacBlacklistDto addToBlacklist(CreateBlacklistRequest req, String adminUsername) {
        String normalizedMac = req.macAddress().trim().toUpperCase();
        Optional<MacBlacklist> existing = blacklistRepository.findByMacAddressIgnoreCase(normalizedMac);

        MacBlacklist entry;
        if (existing.isPresent()) {
            entry = existing.get();
            entry.setActive(true);
            entry.setReason(req.reason());
            entry.setBlockedBy(adminUsername != null ? adminUsername : "system");
            entry.setBlockedAt(Instant.now());
        } else {
            entry = new MacBlacklist();
            entry.setMacAddress(normalizedMac);
            entry.setReason(req.reason());
            entry.setBlockedBy(adminUsername != null ? adminUsername : "system");
            entry.setBlockedAt(Instant.now());
            entry.setActive(true);
        }

        log.warn("Device MAC {} added to blacklist by {}. Reason: {}", normalizedMac, adminUsername, req.reason());
        MacBlacklist saved = blacklistRepository.save(entry);
        if (guestSessionService != null) {
            guestSessionService.enforceHardwareMacBlock(
                    normalizedMac,
                    adminUsername,
                    "Danh sách đen MAC: " + (req.reason() != null ? req.reason() : "Bị chặn bởi quản trị viên")
            );
        }
        return MacBlacklistDto.fromEntity(saved);
    }

    public MacBlacklistDto unblock(Long id) {
        MacBlacklist entry = blacklistRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MacBlacklist", "id", id));
        entry.setActive(false);
        log.info("Device MAC {} unblocked", entry.getMacAddress());
        MacBlacklist saved = blacklistRepository.save(entry);
        if (guestSessionService != null) {
            guestSessionService.releaseHardwareMacBlock(entry.getMacAddress());
        }
        return MacBlacklistDto.fromEntity(saved);
    }
}

