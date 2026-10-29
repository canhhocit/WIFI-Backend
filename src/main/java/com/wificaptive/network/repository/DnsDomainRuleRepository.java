package com.wificaptive.network.repository;

import com.wificaptive.network.entity.DnsDomainRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DnsDomainRuleRepository extends JpaRepository<DnsDomainRule, Long> {
    Optional<DnsDomainRule> findByDomainName(String domainName);
    boolean existsByDomainName(String domainName);

    List<DnsDomainRule> findByRuleTypeAndActiveTrue(String ruleType);
    Page<DnsDomainRule> findByRuleType(String ruleType, Pageable pageable);
    Page<DnsDomainRule> findByDomainNameContainingIgnoreCase(String domainName, Pageable pageable);
    Page<DnsDomainRule> findByRuleTypeAndDomainNameContainingIgnoreCase(String ruleType, String domainName, Pageable pageable);
}
