package com.example.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.example.config.TimezoneInterceptor;
import com.example.config.WebConfig;
import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.dto.response.CreditCardApplicationSummaryResponse;
import com.example.enums.EmploymentType;
import com.example.enums.UserRole;
import com.example.security.CustomUserPrincipal;
import com.example.security.JwtFilter;
import com.example.security.JwtUtil;
import com.example.service.CreditAccountApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(
        controllers = CreditAccountApplicationController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TimezoneInterceptor.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class CreditAccountApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreditAccountApplicationService service;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    // ========================= APPLY =========================
    @Test
    @WithMockUser(roles = "CUSTOMER")
    void shouldApply() throws Exception {

        UUID userId = UUID.randomUUID();

        CreditCardApplicationRequest request = validApplicationRequest();

        CreditCardApplicationSummaryResponse response =
                Mockito.mock(CreditCardApplicationSummaryResponse.class);

        Mockito.when(service.apply(Mockito.any(), Mockito.any()))
                .thenReturn(response);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "test@test.com", null, UserRole.CUSTOMER);

        mockMvc.perform(post("/api/v1/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("principal", principal))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message")
                        .value("Application submitted successfully"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ========================= GET APPLICATIONS =========================
    @Test
    void shouldGetApplicationsForCustomer() throws Exception {

        UUID userId = UUID.randomUUID();

        List<CreditCardApplicationSummaryResponse> responses =
                List.of(Mockito.mock(CreditCardApplicationSummaryResponse.class));

        Mockito.when(service.getCustomerApplications(Mockito.any()))
                .thenReturn(responses);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "test@test.com", null, UserRole.CUSTOMER);

        mockMvc.perform(get("/api/v1/applications")
                        .requestAttr("principal", principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Applications fetched successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldGetApplicationsForAdmin() throws Exception {

        UUID userId = UUID.randomUUID();

        List<CreditCardApplicationSummaryResponse> responses =
                List.of(Mockito.mock(CreditCardApplicationSummaryResponse.class));

        Mockito.when(service.getAllApplications())
                .thenReturn(responses);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "test@test.com", null, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/applications")
                        .requestAttr("principal", principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Applications fetched successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ========================= GET BY ID =========================
    @Test
    void shouldGetApplicationByIdForCustomer() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        CreditCardApplicationResponse response =
                Mockito.mock(CreditCardApplicationResponse.class);

        Mockito.when(service.getCustomerApplicationById(Mockito.any(), Mockito.any()))
                .thenReturn(response);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, userId, "test@test.com", null, UserRole.CUSTOMER);

        mockMvc.perform(get("/api/v1/applications/{id}", applicationId)
                        .requestAttr("principal", principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Application fetched successfully"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldGetApplicationByIdForAdmin() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        CreditCardApplicationResponse response =
                Mockito.mock(CreditCardApplicationResponse.class);

        Mockito.when(service.getApplicationById(Mockito.any()))
                .thenReturn(response);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "admin@test.com", null, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/applications/{id}", applicationId)
                        .requestAttr("principal", principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Application fetched successfully"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ========================= DECIDE =========================
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDecideApplication() throws Exception {

        UUID applicationId = UUID.randomUUID();

        ApplicationDecisionRequest request = validDecisionRequest();

        CreditCardApplicationResponse response =
                Mockito.mock(CreditCardApplicationResponse.class);

        Mockito.when(service.decide(Mockito.any(), Mockito.any()))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/applications/{id}", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Application decision processed successfully"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldGetApplicationsByStatusForAdmin() throws Exception {

        UUID userId = UUID.randomUUID();

        List<CreditCardApplicationSummaryResponse> responses =
                List.of(Mockito.mock(CreditCardApplicationSummaryResponse.class));

        Mockito.when(service.getApplicationsByStatus(Mockito.eq("APPROVED")))
                .thenReturn(responses);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "admin@test.com", null, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/applications")
                        .param("status", "APPROVED") // 🔥 THIS triggers red line
                        .requestAttr("principal", principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Applications fetched successfully"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
    
    @Test
    void shouldGetApplicationsByStatusForCustomer() throws Exception {

        UUID userId = UUID.randomUUID();

        List<CreditCardApplicationSummaryResponse> responses =
                List.of(Mockito.mock(CreditCardApplicationSummaryResponse.class));

        Mockito.when(service.getCustomerApplicationsByStatus(Mockito.any(), Mockito.any()))
                .thenReturn(responses);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "test@test.com", null, UserRole.CUSTOMER);

        mockMvc.perform(get("/api/v1/applications")
                        .param("status", "APPROVED") // 🔥 important
                        .requestAttr("principal", principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Applications fetched successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.timestamp").exists());
    }
    // ========================= HELPERS =========================

    private CreditCardApplicationRequest validApplicationRequest() {
        CreditCardApplicationRequest req = new CreditCardApplicationRequest();

        req.setCreditProductId(101L); // Long (not UUID ❗)

        req.setEmploymentType(EmploymentType.SALARIED); // Enum ❗
        req.setEmployerName("TCS"); // optional but good practice

        req.setMonthlyIncome(new BigDecimal("75000")); // BigDecimal ❗
        req.setExistingLiabilities(new BigDecimal("15000"));

        req.setCreditScoreAtApplication(750);

        req.setRequestedCreditLimit(new BigDecimal("200000"));

        return req;
    }

    private ApplicationDecisionRequest validDecisionRequest() {
        ApplicationDecisionRequest req = new ApplicationDecisionRequest();

        req.setApproved(true); // important

        req.setDecisionReason("Income verified and credit score acceptable");

        req.setApprovedCreditLimit(new BigDecimal("200000"));
        req.setApprovedApr(new BigDecimal("14.5"));

        return req;
    }
}