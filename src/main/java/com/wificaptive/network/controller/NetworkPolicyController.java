package com.wificaptive.network.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.network.dto.CreatePolicyRequest;
import com.wificaptive.network.dto.NetworkBandwidthPolicyDto;
import com.wificaptive.network.service.NetworkBandwidthPolicyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/network-policies")
@PreAuthorize("hasRole('ADMIN')")
public class NetworkPolicyController {

    private final NetworkBandwidthPolicyService policyService;

    public NetworkPolicyController(NetworkBandwidthPolicyService policyService) {
        this.policyService = policyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NetworkBandwidthPolicyDto>>> getAllPolicies() {
        return ResponseEntity.ok(ApiResponse.ok(policyService.getAllPolicies()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<NetworkBandwidthPolicyDto>> createPolicy(
            @Valid @RequestBody CreatePolicyRequest req
    ) {
        NetworkBandwidthPolicyDto created = policyService.createPolicy(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<NetworkBandwidthPolicyDto>> togglePolicy(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(policyService.togglePolicy(id)));
    }
}
