package com.example.repository;

import com.example.entity.UnderwritingRule;
import com.example.enums.RuleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UnderwritingRuleRepository
        extends JpaRepository<UnderwritingRule, UUID> {

    // Load all active rules sorted by priority
    List<UnderwritingRule> findAllByIsActiveTrueOrderByPriorityAsc();

    // Load active rules by type sorted by priority
    List<UnderwritingRule> findAllByRuleTypeAndIsActiveTrueOrderByPriorityAsc(RuleType ruleType);
}