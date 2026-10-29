package com.wificaptive.network.service;

import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.network.dto.CreatePolicyRequest;
import com.wificaptive.network.dto.NetworkBandwidthPolicyDto;
import com.wificaptive.network.entity.NetworkBandwidthPolicy;
import com.wificaptive.network.repository.NetworkBandwidthPolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NetworkBandwidthPolicyService {

    private final NetworkBandwidthPolicyRepository policyRepository;

    public NetworkBandwidthPolicyService(NetworkBandwidthPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    @Transactional(readOnly = true)
    public List<NetworkBandwidthPolicyDto> getAllPolicies() {
        return policyRepository.findAll().stream()
                .map(NetworkBandwidthPolicyDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NetworkBandwidthPolicyDto> getActivePolicies() {
        return policyRepository.findByActiveTrueOrderByPriorityDesc().stream()
                .map(NetworkBandwidthPolicyDto::fromEntity)
                .toList();
    }

    public NetworkBandwidthPolicyDto createPolicy(CreatePolicyRequest req) {
        NetworkBandwidthPolicy policy = new NetworkBandwidthPolicy();
        policy.setName(req.name());
        policy.setDescription(req.description());
        policy.setDownloadSpeedKbps(req.downloadSpeedKbps());
        policy.setUploadSpeedKbps(req.uploadSpeedKbps());
        policy.setMaxSessionSeconds(req.maxSessionSeconds());
        policy.setQuotaMb(req.quotaMb() != null ? req.quotaMb() : 0);
        policy.setPriority(req.priority() != null ? req.priority() : 1);
        policy.setActive(req.active() != null ? req.active() : true);

        return NetworkBandwidthPolicyDto.fromEntity(policyRepository.save(policy));
    }

    public NetworkBandwidthPolicyDto togglePolicy(Long id) {
        NetworkBandwidthPolicy policy = policyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Policy", "id", id));
        policy.setActive(!policy.getActive());
        return NetworkBandwidthPolicyDto.fromEntity(policyRepository.save(policy));
    }
}
