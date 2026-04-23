package com.example.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(
        controllers = KycController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TimezoneInterceptor.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class KycControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private KycService kycService;
    @MockBean private JwtUtil jwtUtil;

    private final UUID userId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    private CustomUserPrincipal principal() {
        return new CustomUserPrincipal(
                userId,
                customerId,
                "test@example.com",
                null,
                UserRole.CUSTOMER
        );
    }

    // ================= UPLOAD KYC =================

    @Nested
    @DisplayName("Upload KYC API Tests")
    class UploadKycTests {

        @Test
        void shouldReturnCreated_whenValidRequest() throws Exception {
            // GIVEN
            when(kycService.uploadKyc(any(), anyString(), anyString(), any()))
                    .thenReturn("SUBMITTED");

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "pan.jpg",
                    MediaType.IMAGE_JPEG_VALUE,
                    "testdata".getBytes()
            );

            // WHEN + THEN
            mockMvc.perform(multipart("/api/v1/kyc")
                    .file(file)
                    .param("documentType", "PAN")
                    .param("documentNumber", "ABCDE1234F")
                    .with(authentication(
                            new UsernamePasswordAuthenticationToken(principal(), null)
                    )))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message")
                            .value("KYC submitted successfully"))
                    .andExpect(jsonPath("$.data").value("SUBMITTED"));
        }
    }

    // ================= GET KYC STATUS =================

    @Nested
    @DisplayName("Get KYC Status API Tests")
    class GetKycStatusTests {

        @Test
        void shouldReturnOk_whenKycExists() throws Exception {
            // GIVEN
            KycResponse response = new KycResponse(
                    UUID.randomUUID(),
                    KycStatus.PENDING,
                    Instant.now()
            );

            when(kycService.getKycStatus(any())).thenReturn(response);

            // WHEN + THEN
            mockMvc.perform(get("/api/v1/kyc")
                    .with(authentication(
                            new UsernamePasswordAuthenticationToken(principal(), null)
                    )))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.message")
                            .value("KYC status fetched successfully"));
        }
    }

    // ================= UPDATE KYC =================

    @Nested
    @DisplayName("Update KYC API Tests")
    class UpdateKycTests {

        @Test
        void shouldReturnOk_whenAdminVerifiesKyc() throws Exception {
            // GIVEN
            UUID kycId = UUID.randomUUID();

            KycStatusUpdateRequest request =
                    new KycStatusUpdateRequest(KycStatus.VERIFIED, null);

            when(kycService.updateKycStatus(any(), any(), any()))
                    .thenReturn("VERIFIED");

            // WHEN + THEN
            mockMvc.perform(put("/api/v1/kyc/{kycId}", kycId)
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
                    .andExpect(jsonPath("$.data").value("VERIFIED"))
                    .andExpect(jsonPath("$.message")
                            .value("KYC status updated successfully"));
        }
    }

    // ================= GET PENDING KYC =================

    @Nested
    @DisplayName("Get Pending KYC API Tests")
    class GetPendingKycTests {

        @Test
        void shouldReturnOk_whenPendingKycExists() throws Exception {
            // GIVEN
            List<KycResponse> list = List.of(
                    new KycResponse(UUID.randomUUID(), KycStatus.PENDING, Instant.now())
            );

            when(kycService.getPendingKyc()).thenReturn(list);

            // WHEN + THEN
            mockMvc.perform(get("/api/v1/kyc/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                    .andExpect(jsonPath("$.message")
                            .value("Pending KYC records fetched successfully"));
        }
    }

    // ================= HELPER =================

    @SuppressWarnings("unused")
	private ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
}