package com.example.entity;

import com.example.enums.RuleAction;
import com.example.enums.RuleOperator;
import com.example.enums.RuleType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "underwriting_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnderwritingRule extends BaseEntity {

	@Id
	@GeneratedValue
	@Column(name = "rule_id")
	private UUID ruleId;

	@Column(name = "rule_name", nullable = false, unique = true)
	private String ruleName;

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(name = "rule_type", nullable = false,columnDefinition = "rule_type_enum")
	private RuleType ruleType;

	@Column(name = "field_name", nullable = false)
	private String fieldName; // e.g. credit_score_at_application, debt_burden_ratio

	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(name = "operator", nullable = false,columnDefinition = "rule_operator_enum")
	private RuleOperator operator; // GT, GTE, LT, LTE, EQ, NEQ

	@Column(name = "threshold_value", nullable = false)
	private String thresholdValue; // stored as string, parsed at runtime
	
	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(name = "action", nullable = false,columnDefinition = "rule_action_enum")
	private RuleAction action; // APPROVE, REJECT, SCORE, FLAG_REVIEW

	@Column(name = "score_impact", precision = 5, scale = 2)
	private BigDecimal scoreImpact; // +ve = more risky, -ve = less risky

	@Column(name = "priority", nullable = false)
	private Integer priority; // lower = evaluated first
	
	@Column(name = "rule_group")
	private String ruleGroup;   // nullable — null means no exclusivity

	@Column(name = "is_active", nullable = false)
	private Boolean isActive;
}