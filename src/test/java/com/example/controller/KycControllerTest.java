package com.example.controller;

import com.example.config.TimezoneInterceptor;
import com.example.config.WebConfig;
import com.example.dto.request.KycStatusUpdateRequest;
import com.example.dto.response.KycResponse;
import com.example.enums.KycStatus;
import com.example.enums.UserRole;
import com.example.security.CustomUserPrincipal;
import com.example.security.JwtFilter;
import com.example.security.JwtUtil;
import com.example.service.KycService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.http.MediaType;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(
        controllers = KycController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        		excludeFilters = { 
        				@Filter(type = FilterType.ASSIGNABLE_TYPE,classes = JwtFilter.class),
        				@Filter(type = FilterType.ASSIGNABLE_TYPE,classes = TimezoneInterceptor.class),
        				@Filter(type = FilterType.ASSIGNABLE_TYPE,classes = WebConfig.class)
        	}
)
@AutoConfigureMockMvc(addFilters = false)
class KycControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KycService kycService;
    
    @MockBean
    private JwtUtil jwtUtil;  

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID userId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    private CustomUserPrincipal principal() {
        return new CustomUserPrincipal(
                userId,
                customerId,
                "test@example.com",
                UserRole.CUSTOMER
        );
    }


    // UPLOAD KYC

    @Test
    void shouldUploadKycSuccessfully() throws Exception {

        Mockito.when(kycService.uploadKyc(any(), anyString(), anyString(), any()))
                .thenReturn("SUBMITTED");

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "pan.jpg",
                        MediaType.IMAGE_JPEG_VALUE,
                        "testdata".getBytes()
                );

        mockMvc.perform(multipart("/api/v1/kyc")
                .file(file)
                .param("documentType","PAN")
                .param("documentNumber","ABCDE1234F")
                .with(authentication(
                        new UsernamePasswordAuthenticationToken(principal(), null)
                )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("KYC submitted successfully"))
                .andExpect(jsonPath("$.data").value("SUBMITTED"))
                .andExpect(jsonPath("$.timestamp").exists());


        Mockito.verify(kycService)
                .uploadKyc(any(), anyString(), anyString(), any());
    }


    // GET KYC STATUS


    @Test
    void shouldGetKycStatus() throws Exception {

        KycResponse kyc =
                new KycResponse(UUID.randomUUID(), KycStatus.PENDING, Instant.now());

        
        Mockito.when(kycService.getKycStatus(any()))
                .thenReturn(kyc);

        mockMvc.perform(get("/api/v1/kyc")
                .with(authentication(
                        new UsernamePasswordAuthenticationToken(principal(), null)
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("KYC status fetched successfully"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.timestamp").exists());

        Mockito.verify(kycService).getKycStatus(any());
    }

    // =============================
    // VERIFY KYC
    // =============================

    @Test
    void shouldVerifyKyc() throws Exception {

        UUID kycId = UUID.randomUUID();

        KycStatusUpdateRequest request =
                new KycStatusUpdateRequest(KycStatus.VERIFIED, null);

        Mockito.when(kycService.updateKycStatus(any(), any(), any()))
                .thenReturn("VERIFIED");

        mockMvc.perform(put("/api/v1/kyc/{kycId}",kycId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(authentication(
                        new UsernamePasswordAuthenticationToken(
                                principal(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        )
                )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("KYC status updated successfully"))
                .andExpect(jsonPath("$.data").value("VERIFIED"))
                .andExpect(jsonPath("$.timestamp").exists());


        Mockito.verify(kycService).updateKycStatus(any(), any(), any());
    }

    // =============================
    // GET PENDING KYC
    // =============================

    @Test
    void shouldGetPendingKyc() throws Exception {

        List<KycResponse> list =
                List.of(new KycResponse(UUID.randomUUID(), KycStatus.PENDING, Instant.now()));

        Mockito.when(kycService.getPendingKyc())
                .thenReturn(list);

        mockMvc.perform(get("/api/v1/kyc/pending"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message")
                .value("Pending KYC records fetched successfully"))
        .andExpect(jsonPath("$.data[0].status").value("PENDING"))
        .andExpect(jsonPath("$.timestamp").exists());
    }
}