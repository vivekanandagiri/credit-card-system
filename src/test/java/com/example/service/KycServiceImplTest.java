package com.example.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class KycServiceImplTest {

    @Mock private KycRepository kycRepository;
    @Mock private CustomerService customerService;
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
        kycId = UUID.randomUUID();
        adminId = UUID.randomUUID();

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

    // ================= UPLOAD KYC =================

    @Nested
    @DisplayName("Upload KYC Tests")
    class UploadKycTests {

        @Test
        void shouldUploadKycSuccessfully_whenValidRequest() throws Exception {
            // GIVEN
            when(customerService.getCustomer(customerId)).thenReturn(customer);
            when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                    .thenReturn(Optional.empty());
            when(kycRepository.findByCustomerCustomerId(customerId))
                    .thenReturn(List.of());
            when(kycMapper.toEntity(any(), anyString(), anyString(), any()))
                    .thenReturn(kycRecord);

            // WHEN
            String response = kycService.uploadKyc(customerId, "PAN", "ABCDE1234F", file);

            // THEN
            assertThat(response).isEqualTo("SUBMITTED");
            verify(kycRepository).save(kycRecord);
        }

        @Test
        void shouldDeactivateOldRecords_whenUploadingNewKyc() throws Exception {
            // GIVEN
            KycRecord oldRecord = new KycRecord();
            oldRecord.setActive(true);

            when(customerService.getCustomer(customerId)).thenReturn(customer);
            when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                    .thenReturn(Optional.empty());
            when(kycRepository.findByCustomerCustomerId(customerId))
                    .thenReturn(List.of(oldRecord));
            when(kycMapper.toEntity(any(), anyString(), anyString(), any()))
                    .thenReturn(kycRecord);

            // WHEN
            kycService.uploadKyc(customerId, "PAN", "ABCDE1234F", file);

            // THEN
            assertThat(oldRecord.isActive()).isFalse();
            verify(kycRepository).save(kycRecord);
        }

        @Test
        void shouldThrowException_whenCustomerNotFound() {
            // GIVEN
            when(customerService.getCustomer(customerId))
                    .thenThrow(new ResourceNotFoundException("Customer not found"));

            // WHEN + THEN
            assertThatThrownBy(() ->
                    kycService.uploadKyc(customerId, "PAN", "ABCDE1234F", file))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(kycRepository, never()).save(any());
        }

        @Test
        void shouldThrowException_whenKycAlreadySubmitted() {
            // GIVEN
            when(customerService.getCustomer(customerId)).thenReturn(customer);
            when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                    .thenReturn(Optional.of(kycRecord));

            // WHEN + THEN
            assertThatThrownBy(() ->
                    kycService.uploadKyc(customerId, "PAN", "ABCDE1234F", file))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrowException_whenKycAlreadyVerified() {
            // GIVEN
            kycRecord.setStatus(KycStatus.VERIFIED);

            when(customerService.getCustomer(customerId)).thenReturn(customer);
            when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                    .thenReturn(Optional.of(kycRecord));

            // WHEN + THEN
            assertThatThrownBy(() ->
                    kycService.uploadKyc(customerId, "PAN", "ABCDE1234F", file))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    // ================= UPDATE KYC =================

    @Nested
    @DisplayName("Update KYC Tests")
    class UpdateKycTests {

        @Test
        void shouldApproveKyc_whenValidRequest() {
            // GIVEN
            KycStatusUpdateRequest request =
                    new KycStatusUpdateRequest(KycStatus.VERIFIED, null);

            when(kycRepository.findById(kycId)).thenReturn(Optional.of(kycRecord));

            // WHEN
            String response = kycService.updateKycStatus(kycId, adminId, request);

            // THEN
            assertThat(response).isEqualTo("VERIFIED");
            assertThat(kycRecord.getVerifiedBy()).isEqualTo(adminId);
            assertThat(kycRecord.getVerifiedAt()).isNotNull();
        }

        @Test
        void shouldRejectKyc_whenReasonProvided() {
            // GIVEN
            KycStatusUpdateRequest request =
                    new KycStatusUpdateRequest(KycStatus.REJECTED, "Invalid doc");

            when(kycRepository.findById(kycId)).thenReturn(Optional.of(kycRecord));

            // WHEN
            kycService.updateKycStatus(kycId, adminId, request);

            // THEN
            assertThat(kycRecord.getStatus()).isEqualTo(KycStatus.REJECTED);
            assertThat(kycRecord.getRejectionReason()).isEqualTo("Invalid doc");
        }

        @Test
        void shouldThrowException_whenRejectReasonMissing() {
            // GIVEN
            KycStatusUpdateRequest request =
                    new KycStatusUpdateRequest(KycStatus.REJECTED, null);

            when(kycRepository.findById(kycId)).thenReturn(Optional.of(kycRecord));

            // WHEN + THEN
            assertThatThrownBy(() ->
                    kycService.updateKycStatus(kycId, adminId, request))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrowException_whenKycAlreadyFinalized() {
            // GIVEN
            kycRecord.setStatus(KycStatus.VERIFIED);

            KycStatusUpdateRequest request =
                    new KycStatusUpdateRequest(KycStatus.REJECTED, "Late");

            when(kycRepository.findById(kycId)).thenReturn(Optional.of(kycRecord));

            // WHEN + THEN
            assertThatThrownBy(() ->
                    kycService.updateKycStatus(kycId, adminId, request))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrowException_whenKycNotFound() {
            // GIVEN
            when(kycRepository.findById(kycId)).thenReturn(Optional.empty());

            // WHEN + THEN
            assertThatThrownBy(() ->
                    kycService.updateKycStatus(kycId, adminId,
                            new KycStatusUpdateRequest(KycStatus.VERIFIED, null)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ================= GET KYC =================

    @Nested
    @DisplayName("Get KYC Tests")
    class GetKycTests {

        @Test
        void shouldReturnKycStatus_whenRecordExists() {
            // GIVEN
            KycResponse response = new KycResponse();

            when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                    .thenReturn(Optional.of(kycRecord));
            when(kycMapper.toResponse(kycRecord)).thenReturn(response);

            // WHEN
            KycResponse result = kycService.getKycStatus(customerId);

            // THEN
            assertThat(result).isSameAs(response);
        }

        @Test
        void shouldThrowException_whenNoKycExists() {
            // GIVEN
            when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                    .thenReturn(Optional.empty());

            // WHEN + THEN
            assertThatThrownBy(() ->
                    kycService.getKycStatus(customerId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ================= IS VERIFIED =================

    @Nested
    @DisplayName("KYC Verification Check Tests")
    class IsKycVerifiedTests {

        @Test
        void shouldReturnTrue_whenKycVerified() {
            // GIVEN
            when(kycRepository.existsByCustomerCustomerIdAndStatus(
                    customerId, KycStatus.VERIFIED)).thenReturn(true);

            // WHEN
            boolean result = kycService.isKycVerified(customerId);

            // THEN
            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalse_whenKycNotVerified() {
            // GIVEN
            when(kycRepository.existsByCustomerCustomerIdAndStatus(
                    customerId, KycStatus.VERIFIED)).thenReturn(false);

            // WHEN
            boolean result = kycService.isKycVerified(customerId);

            // THEN
            assertThat(result).isFalse();
        }
    }
}