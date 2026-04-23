package com.example.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import com.example.config.TimezoneInterceptor;
import com.example.config.WebConfig;
import com.example.dto.request.*;
import com.example.dto.response.*;
import com.example.enums.*;
import com.example.security.CustomUserPrincipal;
import com.example.security.JwtFilter;
import com.example.security.JwtUtil;
import com.example.service.CreditCardService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(
        controllers = CreditCardController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TimezoneInterceptor.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class CreditCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreditCardService cardService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    // ================= ISSUE CARD =================

    @Test
    void shouldIssueCardForCustomer() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        CreditCardIssuanceRequest request = new CreditCardIssuanceRequest();
        request.setCardProductId(UUID.randomUUID());
        request.setCardFormat(CardFormat.VIRTUAL);
        request.setIssuanceReason(CardIssuanceReason.NEW_CARD);

        Mockito.when(cardService.issueCard(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mockito.mock(CreditCardResponse.class));

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "test@test.com", null, UserRole.CUSTOMER);

        mockMvc.perform(post("/api/v1/accounts/{accountId}/cards", accountId)
                        .principal(new UsernamePasswordAuthenticationToken(principal, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

//    @Test
//    void shouldIssueCardForAdmin() throws Exception {
//
//        UUID accountId = UUID.randomUUID();
//
//        CreditCardIssuanceRequest request = new CreditCardIssuanceRequest();
//        request.setCardProductId(UUID.randomUUID());
//        request.setCardFormat(CardFormat.VIRTUAL);
//        request.setIssuanceReason(CardIssuanceReason.NEW_CARD);
//
//        Mockito.when(cardService.issueCardByAdmin(Mockito.any(), Mockito.any()))
//                .thenReturn(Mockito.mock(CreditCardResponse.class));
//
//        CustomUserPrincipal principal =
//                new CustomUserPrincipal(UUID.randomUUID(), null, "admin@test.com", UserRole.ADMIN);
//
//        mockMvc.perform(post("/api/v1/accounts/{accountId}/cards", accountId)
//                .with(req -> {
//                    req.setUserPrincipal(
//                        new UsernamePasswordAuthenticationToken(
//                            principal,
//                            null,
//                            principal.getAuthorities()
//                        )
//                    );
//                    return req;
//                })
//                .contentType(MediaType.APPLICATION_JSON)
//                .content(objectMapper.writeValueAsString(request)))
//                .andExpect(status().isCreated());
//
//        Mockito.verify(cardService).issueCardByAdmin(Mockito.any(), Mockito.any());
//    }
    // ================= GET CARDS =================

    @Test
    void shouldGetCardsForCustomer() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        Mockito.when(cardService.getCardsByAccount(Mockito.any(), Mockito.any()))
                .thenReturn(List.of(Mockito.mock(CreditCardResponse.class)));

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "test@test.com", null, UserRole.CUSTOMER);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/cards", accountId)
                        .principal(new UsernamePasswordAuthenticationToken(principal, null)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetCardsForAdmin() throws Exception {

        UUID accountId = UUID.randomUUID();

        Mockito.when(cardService.getCardsByAccount(Mockito.any()))
                .thenReturn(List.of(Mockito.mock(CreditCardResponse.class)));

        CustomUserPrincipal principal =
                new CustomUserPrincipal(UUID.randomUUID(), null, "admin@test.com", null, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/cards", accountId)
                        .principal(new UsernamePasswordAuthenticationToken(principal, null)))
                .andExpect(status().isOk());
    }

    // ================= GET CARD =================

    @Test
    void shouldGetCardByIdForCustomer() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        Mockito.when(cardService.getCardById(Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mockito.mock(CreditCardResponse.class));

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "test@test.com", null, UserRole.CUSTOMER);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/cards/{cardId}", accountId, cardId)
                        .principal(new UsernamePasswordAuthenticationToken(principal, null)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldGetCardByIdForAdmin() throws Exception {

        UUID cardId = UUID.randomUUID();

        Mockito.when(cardService.getCardById(Mockito.any()))
                .thenReturn(Mockito.mock(CreditCardResponse.class));

        CustomUserPrincipal principal =
                new CustomUserPrincipal(UUID.randomUUID(), null, "admin@test.com", null, UserRole.ADMIN);

        mockMvc.perform(get("/api/v1/{cardId}", cardId)
                        .principal(new UsernamePasswordAuthenticationToken(principal, null)))
                .andExpect(status().isOk());
    }

    // ================= UPDATE =================

    @Test
    void shouldUpdateCardStatus() throws Exception {

        UUID accountId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();

        CreditCardStatusUpdateRequest request = new CreditCardStatusUpdateRequest();
        request.setStatus(CardStatus.ACTIVE);
        request.setReason("Valid");

        Mockito.when(cardService.updateCardStatusForUser(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mockito.mock(CreditCardIssuanceResponse.class));

        CustomUserPrincipal principal =
                new CustomUserPrincipal(UUID.randomUUID(), null, "test@test.com", null, UserRole.CUSTOMER);

        mockMvc.perform(patch("/api/v1/accounts/{accountId}/cards/{cardId}", accountId, cardId)
                        .principal(new UsernamePasswordAuthenticationToken(principal, null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}