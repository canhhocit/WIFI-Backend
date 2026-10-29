package com.wificaptive.network.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.common.dto.PageResponse;
import com.wificaptive.network.dto.CreateBlacklistRequest;
import com.wificaptive.network.dto.MacBlacklistDto;
import com.wificaptive.network.service.MacBlacklistService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/admin/blacklist", "/v1/admin/blacklist"})
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SOTTTT_ADMIN')")
public class MacBlacklistController {

    private final MacBlacklistService blacklistService;

    public MacBlacklistController(MacBlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MacBlacklistDto>>> getBlacklist(
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<MacBlacklistDto> result = blacklistService.getBlacklist(
                active,
                PageRequest.of(page, size, Sort.by("blockedAt").descending())
        );
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(result)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MacBlacklistDto>> addToBlacklist(
            @Valid @RequestBody CreateBlacklistRequest req,
            Authentication authentication
    ) {
        String adminUser = authentication != null ? authentication.getName() : "admin";
        MacBlacklistDto entry = blacklistService.addToBlacklist(req, adminUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(entry));
    }

    @PutMapping("/{id}/unblock")
    public ResponseEntity<ApiResponse<MacBlacklistDto>> unblock(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(blacklistService.unblock(id)));
    }
}

