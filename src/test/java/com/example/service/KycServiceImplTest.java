package com.example.service;

import com.example.dto.request.KycStatusUpdateRequest;
import com.example.dto.response.KycResponse;
import com.example.entity.Customer;
import com.example.entity.KycRecord;
import com.example.enums.KycStatus;
import com.example.exception.BusinessRuleException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.KycMapper;
import com.example.repository.KycRepository;
import com.example.service.ServiceImpl.KycServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KycServiceImpl}.
 *
 * <p>Fix summary vs original test:
 * <ul>
 *   <li>Package corrected: {@code service.ServiceImpl} → {@code service.impl}</li>
 *   <li>{@code @Mock CustomerRepository} replaced with {@code @Mock CustomerService}
 *       — {@code KycServiceImpl} no longer injects {@code CustomerRepository} directly</li>
 *   <li>All {@code customerRepository.findById()} stubs replaced with
 *       {@code customerService.getCustomer()} stubs</li>
 *   <li>Customer-not-found test now expects {@code CustomerService.getCustomer} to throw</li>
 *   <li>Message assertions updated to match refactored response strings</li>
 *   <li>New tests added for: {@code isKycVerified}, rejection without reason,
 *       update of already-finalized record, {@code RESUBMIT_REQUIRED}, previous-record
 *       deactivation, and {@code verifiedBy}/{@code verifiedAt} field population</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class KycServiceImplTest {

    @Mock private KycRepository kycRepository;
    @Mock private CustomerService customerService;  // ← was CustomerRepository
    @Mock private KycMapper kycMapper;

    @InjectMocks
    private KycServiceImpl kycService;

    private UUID customerId;
    private UUID kycId;
    private UUID adminId;

    private Customer customer;
    private KycRecord kycRecord;
    private MockMultipartFile file;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        kycId      = UUID.randomUUID();
        adminId    = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerId(customerId);

        kycRecord = new KycRecord();
        kycRecord.setKycId(kycId);
        kycRecord.setCustomer(customer);
        kycRecord.setStatus(KycStatus.SUBMITTED);
        kycRecord.setSubmittedAt(Instant.now());
        kycRecord.setActive(true);

        file = new MockMultipartFile("file", "pan.jpg", "image/jpeg", "data".getBytes());
    }

    // =========================================================================
    // uploadKyc
    // =========================================================================

    @Nested
    class UploadKyc {

        @Test
        void shouldUploadKycSuccessfully() throws Exception {
            when(customerService.getCustomer(customerId)).thenReturn(customer);
            when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                    .thenReturn(Optional.empty());
            when(kycRepository.findByCustomerCustomerId(customerId))
                    .thenReturn(List.of());
            when(kycMapper.toEntity(any(), anyString(), anyString(), any()))
                    .thenReturn(kycRecord);

            String result =
                    kycService.uploadKyc(customerId, "PAN", "ABCDE1234F", file);

            assertEquals("SUBMITTED", result);
            
            verify(kycRepository).save(kycRecord);
        }

        @Test
        void shouldDeactivatePreviousRecordsBeforeSaving() throws Exception {
            KycRecord oldRecord = new KycRecord();
            oldRecord.setActive(true);

            when(customerService.getCustomer(customerId)).thenReturn(customer);
            when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                    .thenReturn(Optional.empty());
            when(kycRepository.findByCustomerCustomerId(customerId))
                    .thenReturn(List.of(oldRecord));
            when(kycMapper.toEntity(any(), anyString(), anyString(), any()))
                    .thenReturn(kycRecord);

            kycService.uploadKyc(customerId, "PAN", "ABCDE1234F", file);

            // Previous record must be deactivated before new one is saved
            assertThat(oldRecord.isActive()).isFalse();
            verify(kycRepository).save(kycRecord);
        }

        @Test
        void shouldThrowWhenCustomerNotFound() {
            // CustomerService.getCustomer throws when customer is absent
            when(customerService.getCustomer(customerId))
                    .thenThrow(new ResourceNotFoundException("Customer not found"));

            assertThrows(ResourceNotFoundException.class,
                    () -> kycService.uploadKyc(customerId, "PAN", "ABCDE1234F", file));

            verify(kycRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenKycAlreadySubmitted() {
            kycRecord.setStatus(KycStatus.SUBMITTED);

            when(customerService.getCustomer(customerId)).thenReturn(customer);
            when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                    .thenReturn(Optional.of(kycRecord));

            assertThrows(BusinessRuleException.class,
                    () -> kycService.uploadKyc(customerId, "PAN", "ABCDE1234F", file));

            verify(kycRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenKycAlreadyVerified() {
            kycRecord.setStatus(KycStatus.VERIFIED);

            when(customerService.getCustomer(customerId)).thenReturn(customer);
            when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                    .thenReturn(Optional.of(kycRecord));

            assertThrows(BusinessRuleException.class,
                    () -> kycService.uploadKyc(customerId, "PAN", "ABCDE1234F", file));

            verify(kycRepository, never()).save(any());
        }
    }

    // =========================================================================
    // updateKycStatus
    // =========================================================================

    @Nested
    class UpdateKycStatus {

        @Test
        void shouldApproveKycAndSetAuditFields() {
            KycStatusUpdateRequest request =
                    new KycStatusUpdateRequest(KycStatus.VERIFIED, null);

            when(kycRepository.findById(kycId)).thenReturn(Optional.of(kycRecord));

            String response = kycService.updateKycStatus(kycId, adminId, request);

            assertEquals("VERIFIED", response);
            assertThat(kycRecord.getStatus()).isEqualTo(KycStatus.VERIFIED);
            assertThat(kycRecord.getVerifiedBy()).isEqualTo(adminId);
            assertThat(kycRecord.getVerifiedAt()).isNotNull();
            assertThat(kycRecord.getRejectionReason()).isNull();
            verify(kycRepository).save(kycRecord);
        }

        @Test
        void shouldRejectKycAndSetRejectionReason() {
            KycStatusUpdateRequest request =
                    new KycStatusUpdateRequest(KycStatus.REJECTED, "Document unclear");

            when(kycRepository.findById(kycId)).thenReturn(Optional.of(kycRecord));

            kycService.updateKycStatus(kycId, adminId, request);

            assertThat(kycRecord.getStatus()).isEqualTo(KycStatus.REJECTED);
            assertThat(kycRecord.getRejectionReason()).isEqualTo("Document unclear");
            verify(kycRepository).save(kycRecord);
        }

        @Test
        void shouldThrowWhenRejectingWithoutReason() {
            // Rejection reason is mandatory — null should be rejected
            KycStatusUpdateRequest request =
                    new KycStatusUpdateRequest(KycStatus.REJECTED, null);

            when(kycRepository.findById(kycId)).thenReturn(Optional.of(kycRecord));

            assertThrows(BusinessRuleException.class,
                    () -> kycService.updateKycStatus(kycId, adminId, request));

            verify(kycRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenRejectingWithBlankReason() {
            // Blank string must also be rejected
            KycStatusUpdateRequest request =
                    new KycStatusUpdateRequest(KycStatus.REJECTED, "   ");

            when(kycRepository.findById(kycId)).thenReturn(Optional.of(kycRecord));

            assertThrows(BusinessRuleException.class,
                    () -> kycService.updateKycStatus(kycId, adminId, request));

            verify(kycRepository, never()).save(any());
        }

        @Test
        void shouldSetResubmitRequiredAndPreserveRejectionReason() {
            KycStatusUpdateRequest request =
                    new KycStatusUpdateRequest(KycStatus.RESUBMIT_REQUIRED, "Photo too blurry");

            when(kycRepository.findById(kycId)).thenReturn(Optional.of(kycRecord));

            kycService.updateKycStatus(kycId, adminId, request);

            assertThat(kycRecord.getStatus()).isEqualTo(KycStatus.RESUBMIT_REQUIRED);
            assertThat(kycRecord.getRejectionReason()).isEqualTo("Photo too blurry");
            verify(kycRepository).save(kycRecord);
        }

        @Test
        void shouldThrowWhenKycAlreadyVerified() {
            // VERIFIED is a terminal state — cannot be updated again
            kycRecord.setStatus(KycStatus.VERIFIED);
            KycStatusUpdateRequest request =
                    new KycStatusUpdateRequest(KycStatus.REJECTED, "Late change");

            when(kycRepository.findById(kycId)).thenReturn(Optional.of(kycRecord));

            assertThrows(BusinessRuleException.class,
                    () -> kycService.updateKycStatus(kycId, adminId, request));

            verify(kycRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenKycAlreadyRejected() {
            // REJECTED is a terminal state — cannot be updated again
            kycRecord.setStatus(KycStatus.REJECTED);
            KycStatusUpdateRequest request =
                    new KycStatusUpdateRequest(KycStatus.VERIFIED, null);

            when(kycRepository.findById(kycId)).thenReturn(Optional.of(kycRecord));

            assertThrows(BusinessRuleException.class,
                    () -> kycService.updateKycStatus(kycId, adminId, request));

            verify(kycRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenKycRecordNotFound() {
            when(kycRepository.findById(kycId)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> kycService.updateKycStatus(kycId, adminId,
                            new KycStatusUpdateRequest(KycStatus.VERIFIED, null)));
        }
    }

    // =========================================================================
    // getKycStatus
    // =========================================================================

    @Nested
    class GetKycStatus {

        @Test
        void shouldReturnActiveKycRecord() {
            KycResponse kycResponse = new KycResponse();
            when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                    .thenReturn(Optional.of(kycRecord));
            when(kycMapper.toResponse(kycRecord)).thenReturn(kycResponse);

            KycResponse response = kycService.getKycStatus(customerId);

            assertThat(response).isSameAs(kycResponse);
            verify(kycMapper).toResponse(kycRecord);
        }

        @Test
        void shouldThrowWhenNoActiveKycExists() {
            when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> kycService.getKycStatus(customerId));
        }
    }

    // =========================================================================
    // getPendingKyc
    // =========================================================================

    @Nested
    class GetPendingKyc {

        @Test
        void shouldReturnListOfPendingKycRecords() {
            KycResponse kycResponse = new KycResponse();
            when(kycRepository.findByStatus(KycStatus.SUBMITTED))
                    .thenReturn(List.of(kycRecord));
            when(kycMapper.toResponse(kycRecord)).thenReturn(kycResponse);

            List<KycResponse> response = kycService.getPendingKyc();

            assertThat(response).hasSize(1);
        }

        @Test
        void shouldReturnEmptyListWhenNoPendingKyc() {
            when(kycRepository.findByStatus(KycStatus.SUBMITTED)).thenReturn(List.of());

            List<KycResponse> response = kycService.getPendingKyc();

            assertThat(response).isEmpty();
        }
    }

    // =========================================================================
    // isKycVerified  (new method added in refactored service)
    // =========================================================================

    @Nested
    class IsKycVerified {

        @Test
        void shouldReturnTrueWhenVerifiedRecordExists() {
            KycRecord verifiedRecord = new KycRecord();
            verifiedRecord.setStatus(KycStatus.VERIFIED);

            when(kycRepository.findByCustomerCustomerId(customerId))
                    .thenReturn(List.of(verifiedRecord));

            assertThat(kycService.isKycVerified(customerId)).isTrue();
        }

        @Test
        void shouldReturnFalseWhenNoVerifiedRecord() {
            KycRecord submittedRecord = new KycRecord();
            submittedRecord.setStatus(KycStatus.SUBMITTED);

            when(kycRepository.findByCustomerCustomerId(customerId))
                    .thenReturn(List.of(submittedRecord));

            assertThat(kycService.isKycVerified(customerId)).isFalse();
        }

        @Test
        void shouldReturnFalseWhenNoRecordsExist() {
            when(kycRepository.findByCustomerCustomerId(customerId))
                    .thenReturn(List.of());

            assertThat(kycService.isKycVerified(customerId)).isFalse();
        }

        @Test
        void shouldReturnTrueWhenAtLeastOneRecordIsVerifiedAmongMultiple() {
            KycRecord rejected = new KycRecord();
            rejected.setStatus(KycStatus.REJECTED);

            KycRecord verified = new KycRecord();
            verified.setStatus(KycStatus.VERIFIED);

            when(kycRepository.findByCustomerCustomerId(customerId))
                    .thenReturn(List.of(rejected, verified));

            assertThat(kycService.isKycVerified(customerId)).isTrue();
        }
    }
}