package com.example.controller;

import com.example.dto.request.KycVerifyRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.KycResponse;
import com.example.enums.KycStatus;
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
        excludeFilters = @Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class
        )
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
                "CUSTOMER"
        );
    }


    // UPLOAD KYC

    @Test
    void shouldUploadKycSuccessfully() throws Exception {

        ApiResponse<String> response =
                new ApiResponse<>(Instant.now(),201,"KYC uploaded","Uploaded");

        Mockito.when(kycService.uploadKyc(any(), anyString(), anyString(), any()))
                .thenReturn(response);

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
                .andExpect(status().isCreated());

        Mockito.verify(kycService)
                .uploadKyc(any(), anyString(), anyString(), any());
    }


    // GET KYC STATUS


    @Test
    void shouldGetKycStatus() throws Exception {

        KycResponse kyc =
                new KycResponse(UUID.randomUUID(), KycStatus.PENDING, Instant.now());

        ApiResponse<KycResponse> response =
                new ApiResponse<>(Instant.now(),200,"Status fetched",kyc);

        Mockito.when(kycService.getKycStatus(any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/kyc/status")
                .with(authentication(
                        new UsernamePasswordAuthenticationToken(principal(), null)
                )))
                .andExpect(status().isOk());

        Mockito.verify(kycService).getKycStatus(any());
    }

    // =============================
    // VERIFY KYC
    // =============================

    @Test
    void shouldVerifyKyc() throws Exception {

        UUID kycId = UUID.randomUUID();

        KycVerifyRequest request =
                new KycVerifyRequest(true,null);

        ApiResponse<String> response =
                new ApiResponse<>(Instant.now(),200,"Verified","KYC verified");

        Mockito.when(kycService.verifyKyc(any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/kyc/{kycId}",kycId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(authentication(
                        new UsernamePasswordAuthenticationToken(principal(), null)
                )))
                .andExpect(status().isOk());

        Mockito.verify(kycService).verifyKyc(any(), any(), any());
    }

    // =============================
    // GET PENDING KYC
    // =============================

    @Test
    void shouldGetPendingKyc() throws Exception {

        List<KycResponse> list =
                List.of(new KycResponse(UUID.randomUUID(), KycStatus.PENDING, Instant.now()));

        ApiResponse<List<KycResponse>> response =
                new ApiResponse<>(Instant.now(),200,"Pending KYC",list);

        Mockito.when(kycService.getPendingKyc())
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/kyc/pending"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }
}