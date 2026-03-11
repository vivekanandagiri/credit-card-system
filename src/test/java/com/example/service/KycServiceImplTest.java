package com.example.service;

import com.example.dto.request.KycVerifyRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.KycResponse;
import com.example.entity.Customer;
import com.example.entity.KycRecord;
import com.example.enums.KycStatus;
import com.example.exception.BusinessRuleException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.KycMapper;
import com.example.repository.CustomerRepository;
import com.example.repository.KycRepository;
import com.example.service.ServiceImpl.KycServiceImpl;

import org.junit.jupiter.api.BeforeEach;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KycServiceImplTest {

    @Mock
    private KycRepository kycRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private KycMapper kycMapper;
    
    @InjectMocks
    private KycServiceImpl kycService;

    private UUID customerId;
    private UUID kycId;

    private Customer customer;
    private KycRecord kycRecord;

    @BeforeEach
    void setup() {
        customerId = UUID.randomUUID();
        kycId = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerId(customerId);

        kycRecord = new KycRecord();
        kycRecord.setKycId(kycId);
        kycRecord.setCustomer(customer);
        kycRecord.setStatus(KycStatus.SUBMITTED);
        kycRecord.setSubmittedAt(Instant.now());
        kycRecord.setActive(true);
    }

    // UPLOAD KYC SUCCESS

    @Test
    void shouldUploadKycSuccessfully() throws Exception {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "pan.jpg",
                        "image/jpeg",
                        "data".getBytes()
                );

        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));

        when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                .thenReturn(Optional.empty());

        when(kycRepository.findByCustomerCustomerId(customerId))
                .thenReturn(List.of());

        when(kycMapper.toEntity(any(), anyString(), anyString(), any()))
                .thenReturn(kycRecord);

        ApiResponse<String> response =
                kycService.uploadKyc(customerId, "PAN", "ABCDE1234F", file);

        assertEquals("KYC submitted successfully", response.getMessage());

        verify(kycRepository).save(any(KycRecord.class));
    }


    // CUSTOMER NOT FOUND

    @Test
    void shouldThrowCustomerNotFound() {

        MockMultipartFile file =
                new MockMultipartFile("file","pan.jpg","image/jpeg","data".getBytes());

        when(customerRepository.findById(customerId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> kycService.uploadKyc(customerId,"PAN","ABCDE1234F",file));
    }

    // KYC ALREADY SUBMITTED

    @Test
    void shouldThrowIfKycAlreadySubmitted() {

        MockMultipartFile file =
                new MockMultipartFile("file","pan.jpg","image/jpeg","data".getBytes());

        kycRecord.setStatus(KycStatus.SUBMITTED);

        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));

        when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                .thenReturn(Optional.of(kycRecord));

        assertThrows(BusinessRuleException.class,
                () -> kycService.uploadKyc(customerId,"PAN","ABCDE1234F",file));
    }


    // KYC ALREADY VERIFIED

    @Test
    void shouldThrowIfKycAlreadyVerified() {

        MockMultipartFile file =
                new MockMultipartFile("file","pan.jpg","image/jpeg","data".getBytes());

        kycRecord.setStatus(KycStatus.VERIFIED);

        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));

        when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                .thenReturn(Optional.of(kycRecord));

        assertThrows(BusinessRuleException.class,
                () -> kycService.uploadKyc(customerId,"PAN","ABCDE1234F",file));
    }

    // VERIFY KYC APPROVED

    @Test
    void shouldVerifyKycApproved() {

        UUID adminId = UUID.randomUUID();

        KycVerifyRequest request = new KycVerifyRequest(true,null);

        when(kycRepository.findById(kycId))
                .thenReturn(Optional.of(kycRecord));

        ApiResponse<String> response =
                kycService.verifyKyc(kycId,adminId,request);

        assertEquals(KycStatus.VERIFIED, kycRecord.getStatus());
        assertEquals("KYC verification processed", response.getMessage());

        verify(kycRepository).save(kycRecord);
    }

    // VERIFY KYC REJECTED

    @Test
    void shouldRejectKyc() {

        UUID adminId = UUID.randomUUID();

        KycVerifyRequest request =
                new KycVerifyRequest(false,"Document unclear");

        when(kycRepository.findById(kycId))
                .thenReturn(Optional.of(kycRecord));

        kycService.verifyKyc(kycId,adminId,request);

        assertEquals(KycStatus.RESUBMIT_REQUIRED, kycRecord.getStatus());
    }

    // VERIFY KYC NOT FOUND

    @Test
    void shouldThrowIfKycNotFound() {

        when(kycRepository.findById(kycId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> kycService.verifyKyc(
                        kycId,
                        UUID.randomUUID(),
                        new KycVerifyRequest(true,null)
                ));
    }
    
    // GET STATUS SUCCESS

    @Test
    void shouldGetKycStatus() {

        when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                .thenReturn(Optional.of(kycRecord));

        ApiResponse<KycResponse> response =
                kycService.getKycStatus(customerId);

        assertEquals("KYC status fetched successfully", response.getMessage());
    }
    // NO ACTIVE KYC
    @Test
    void shouldThrowIfNoActiveKyc() {

        when(kycRepository.findByCustomerCustomerIdAndIsActiveTrue(customerId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> kycService.getKycStatus(customerId));
    }


    // GET PENDING LIST
    @Test
    void shouldGetPendingKycList() {

        when(kycRepository.findByStatus(KycStatus.SUBMITTED))
                .thenReturn(List.of(kycRecord));

        ApiResponse<List<KycResponse>> response =
                kycService.getPendingKyc();

        assertEquals(1, response.getData().size());
    }
}