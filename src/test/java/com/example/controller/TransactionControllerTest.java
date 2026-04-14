package com.example.controller;

import com.example.config.TimezoneInterceptor;
import com.example.config.WebConfig;
import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.idempotency.TransactionIdempotencyService;
import com.example.security.CustomUserPrincipal;
import com.example.security.JwtFilter;
import com.example.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = TransactionController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TimezoneInterceptor.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private TransactionIdempotencyService transactionIdempotencyService;
    
    

    

    // ================= COMMON =================

    private CustomUserPrincipal customer() {
        return new CustomUserPrincipal(
                UUID.randomUUID(),
                null,
                "user@test.com",
                com.example.enums.UserRole.CUSTOMER
        );
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor auth(CustomUserPrincipal principal) {
        return authentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                )
        );
    }

    // ================= CREATE =================

@Test
    void shouldGetAccountTransactions() throws Exception {

        UUID accountId = UUID.randomUUID();
        CustomUserPrincipal user = customer();

        Page<TransactionSummaryResponse> page =
                new PageImpl<>(List.of(mock(TransactionSummaryResponse.class)));

        when(transactionService.getAccountTransactions(
                any(), any(), any(), any(), any(), anyInt(), anyInt()
        )).thenReturn(page);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                        .with(auth(user))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(transactionService).getAccountTransactions(
                any(), any(), any(), any(), any(), anyInt(), anyInt()
        );
    }

    // ================= GET BY ID =================

    @Test
    void shouldGetTransactionById() throws Exception {

        UUID accountId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        CustomUserPrincipal user = customer();

        when(transactionService.getAccountTransactionById(any(), any(), any()))
                .thenReturn(mock(TransactionDetailResponse.class));

        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions/{transactionId}",
                        accountId, transactionId)
                        .with(auth(user)))
                .andExpect(status().isOk());

        verify(transactionService)
                .getAccountTransactionById(any(), any(), any());
    }
}