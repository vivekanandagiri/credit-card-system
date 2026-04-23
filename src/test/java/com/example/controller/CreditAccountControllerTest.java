package com.example.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

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
import org.springframework.test.web.servlet.MockMvc;

import com.example.config.TimezoneInterceptor;
import com.example.config.WebConfig;
import com.example.dto.request.CreditAccountStatusUpdateRequest;
import com.example.dto.response.CreditAccountResponse;
import com.example.enums.AccountStatus;
import com.example.enums.UserRole;
import com.example.security.CustomUserPrincipal;
import com.example.security.JwtFilter;
import com.example.security.JwtUtil;
import com.example.service.CreditAccountService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(
        controllers = CreditAccountController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TimezoneInterceptor.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class CreditAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreditAccountService accountService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    // ================= GET ACCOUNTS =================

    @Test
    void shouldGetAccountsForCustomer() throws Exception {

        UUID userId = UUID.randomUUID();

        List<CreditAccountResponse> responses =
                List.of(Mockito.mock(CreditAccountResponse.class));

        Mockito.when(accountService.getAccounts(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(responses);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "test@test.com", null, UserRole.CUSTOMER);

        mockMvc.perform(get("/api/v1/accounts")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Accounts fetched successfully"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldGetAccountsForAdminWithStatus() throws Exception {

        UUID userId = UUID.randomUUID();

        List<CreditAccountResponse> responses =
                List.of(Mockito.mock(CreditAccountResponse.class));

        Mockito.when(accountService.getAccounts(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(responses);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "admin@test.com", null, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/accounts")
                        .param("status", "ACTIVE")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Accounts fetched successfully"));
    }

    // ================= GET ACCOUNT BY ID =================

    @Test
    void shouldGetAccountByIdForCustomer() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        CreditAccountResponse response =
                Mockito.mock(CreditAccountResponse.class);

        Mockito.when(accountService.getAccountById(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(response);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "test@test.com", null, UserRole.CUSTOMER);

        mockMvc.perform(get("/api/v1/accounts/{id}", accountId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Account fetched successfully"));
    }

    @Test
    void shouldGetAccountByIdForAdmin() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        CreditAccountResponse response =
                Mockito.mock(CreditAccountResponse.class);

        Mockito.when(accountService.getAccountById(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(response);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "admin@test.com", null, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/accounts/{id}", accountId)
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Account fetched successfully"));
    }

    // ================= UPDATE ACCOUNT STATUS =================

    @Test
    void shouldUpdateAccountStatus() throws Exception {

        UUID accountId = UUID.randomUUID();

        CreditAccountStatusUpdateRequest request = new CreditAccountStatusUpdateRequest();
        request.setStatus(AccountStatus.ACTIVE);
        request.setReason("Valid reason");

        CreditAccountResponse response =
                Mockito.mock(CreditAccountResponse.class);

        Mockito.when(accountService.updateAccountStatus(Mockito.any(), Mockito.any()))
                .thenReturn(response);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(
                        UUID.randomUUID(), null, "admin@test.com", null, UserRole.ADMIN);

        mockMvc.perform(patch("/api/v1/accounts/{id}", accountId)
                        .with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Account status updated successfully"));
    }
}