package com.example.entity;

import com.example.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

@Entity
@Table(name = "kyc_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KycRecord extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "kyc_id")
    private UUID kycId;

    @OneToOne
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "document_type",nullable = false)
    private String documentType;

    @Column(name = "document_number",nullable = false)
    private String documentNumber;

    @Column(name = "document_file", nullable = false, columnDefinition = "BYTEA")
    private byte[] documentFile;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "kyc_status")
    private KycStatus status;
    
    private String rejectionReason;

    private Instant submittedAt;
    private Instant verifiedAt;
    private UUID verifiedBy;
    private boolean isActive;
}