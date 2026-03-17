//package com.example.controller;
//
//import com.example.dto.response.ApiResponse;
//import com.example.entity.UnderwritingRule;
//import com.example.repository.UnderwritingRuleRepository;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.Instant;
//import java.util.List;
//import java.util.UUID;
//
///**
// * Admin controller for managing underwriting rules.
// * Allows admins to view, create, update, and toggle rules
// * without touching any code.
// */
//@PreAuthorize("hasRole('ADMIN')")
//@RestController
//@RequestMapping("/api/v1/admin/rules")
//public class UnderwritingRuleController {
//
//    private final UnderwritingRuleRepository repository;
//
//    public UnderwritingRuleController(UnderwritingRuleRepository repository) {
//        this.repository = repository;
//    }
//
//
//    @GetMapping
//    public ResponseEntity<ApiResponse<List<UnderwritingRule>>> getAll() {
//        return ResponseEntity.ok(new ApiResponse<>(
//                Instant.now(), HttpStatus.OK.value(),
//                "Rules fetched successfully",
//                repository.findAllByIsActiveTrueOrderByPriorityAsc()));
//    }
//
//
//    @PostMapping
//    public ResponseEntity<ApiResponse<UnderwritingRule>> create(
//            @RequestBody UnderwritingRule rule) {
//
//        rule.setIsActive(true);
//
//
//        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
//                Instant.now(), HttpStatus.CREATED.value(),
//                "Rule created successfully",
//                repository.save(rule)));
//    }
//
//
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<UnderwritingRule>> update(
//            @PathVariable UUID id,
//            @RequestBody UnderwritingRule updated) {
//
//        UnderwritingRule existing = repository.findById(id)
//                .orElseThrow(() ->
//                        new RuntimeException("Rule with id " + id + " not found"));
//
//        existing.setRuleName(updated.getRuleName());
//        existing.setRuleType(updated.getRuleType());
//        existing.setFieldName(updated.getFieldName());
//        existing.setOperator(updated.getOperator());
//        existing.setThresholdValue(updated.getThresholdValue());
//        existing.setAction(updated.getAction());
//        existing.setScoreImpact(updated.getScoreImpact());
//        existing.setPriority(updated.getPriority());
//
//
//        return ResponseEntity.ok(new ApiResponse<>(
//                Instant.now(), HttpStatus.OK.value(),
//                "Rule updated successfully",
//                repository.save(existing)));
//    }
//
//
//    @PatchMapping("/{id}/toggle")
//    public ResponseEntity<ApiResponse<String>> toggle(@PathVariable UUID id) {
//
//        UnderwritingRule rule = repository.findById(id)
//                .orElseThrow(() ->
//                        new RuntimeException("Rule with id " + id + " not found"));
//
//        rule.setIsActive(!rule.getIsActive());
//        repository.save(rule);
//
//        String state = rule.getIsActive() ? "activated" : "deactivated";
//        return ResponseEntity.ok(new ApiResponse<>(
//                Instant.now(), HttpStatus.OK.value(),
//                "Rule " + state + " successfully",
//                "Rule is now " + state));
//    }
//}