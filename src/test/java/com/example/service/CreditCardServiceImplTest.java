package com.example.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import com.example.dto.request.CreditCardIssuanceRequest;
import com.example.dto.request.CreditCardStatusUpdateRequest;
import com.example.dto.response.CreditCardResponse;
import com.example.entity.*;
import com.example.enums.*;
import com.example.exception.*;
import com.example.mapper.CreditCardMapper;
import com.example.repository.CreditCardRepository;
import com.example.security.CustomUserPrincipal;
import com.example.service.ServiceImpl.CreditCardServiceImpl;
import com.example.util.MaskedCardNumberGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceImplTest {

    @Mock private CreditCardRepository cardRepository;
    @Mock private CreditAccountService creditAccountService;
    @Mock private CardProductService cardProductService;
    @Mock private CreditCardMapper cardMapper;
    @Mock private MaskedCardNumberGenerator maskedCardNumberGenerator;
    @Mock private CustomerService customerService;
    @Mock private CustomerAddressService customerAddressService;

    @InjectMocks
    private CreditCardServiceImpl service;

    private UUID userId;
    private UUID accountId;
    private UUID cardId;

    private Customer customer;
    private CreditAccount account;
    private CreditCard card;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        cardId = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerId(UUID.randomUUID());

        account = new CreditAccount();
        account.setAccountId(accountId);
        account.setCustomer(customer);
        account.setAccountStatus(AccountStatus.ACTIVE);

        card = new CreditCard();
        card.setCardId(cardId);
        card.setCreditAccount(account);
        card.setCardStatus(CardStatus.PENDING_ACTIVATION);
    }

    // ================= ISSUE CARD =================

    @Test
    void issueCard_success() {
        CreditCardIssuanceRequest request = new CreditCardIssuanceRequest();
        request.setCardFormat(CardFormat.VIRTUAL);

        CreditCardProduct product = new CreditCardProduct();
        product.setCardValidityYears(3);
        product.setOnlineTransactionsAllowed(true);
        product.setAtmWithdrawalAllowed(true);
        product.setInternationalUsageAllowed(true);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditAccountService.getAccountEntity(accountId)).thenReturn(account);
        when(cardProductService.getActiveCardProductEntity(any())).thenReturn(product);
        when(cardRepository.countByCreditAccountAccountIdAndCardStatusIn(any(), any())).thenReturn(0);
        when(cardRepository.existsByCreditAccountAccountIdAndCardFormatAndCardStatusIn(any(), any(), any()))
                .thenReturn(false);
        when(maskedCardNumberGenerator.generate(any())).thenReturn("411111XXXXXX1234");
        when(cardRepository.save(any())).thenReturn(card);
        when(cardMapper.toResponse(any())).thenReturn(new CreditCardResponse());

        CreditCardResponse response = service.issueCard(userId, accountId, request);

        assertNotNull(response);
    }

    @Test
    void issueCard_accountOwnershipFail_shouldThrow() {
        Customer otherCustomer = new Customer();
        otherCustomer.setCustomerId(UUID.randomUUID());
        account.setCustomer(otherCustomer);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditAccountService.getAccountEntity(accountId)).thenReturn(account);

        assertThrows(AccessDeniedException.class,
                () -> service.issueCard(userId, accountId, new CreditCardIssuanceRequest()));
    }

    @Test
    void issueCard_accountNotActive_shouldThrow() {
        account.setAccountStatus(AccountStatus.BLOCKED);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditAccountService.getAccountEntity(accountId)).thenReturn(account);

        assertThrows(BusinessRuleException.class,
                () -> service.issueCard(userId, accountId, new CreditCardIssuanceRequest()));
    }

    @Test
    void issueCard_virtualCardAlreadyExists_shouldThrow() {
        CreditCardIssuanceRequest request = new CreditCardIssuanceRequest();
        request.setCardFormat(CardFormat.VIRTUAL);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditAccountService.getAccountEntity(accountId)).thenReturn(account);
        when(cardProductService.getActiveCardProductEntity(any())).thenReturn(new CreditCardProduct());
        when(cardRepository.countByCreditAccountAccountIdAndCardStatusIn(any(), any())).thenReturn(0);
        when(cardRepository.existsByCreditAccountAccountIdAndCardFormatAndCardStatusIn(any(), any(), any()))
                .thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> service.issueCard(userId, accountId, request));
    }

    @Test
    void issueCard_physicalCardWithoutAddress_shouldThrow() {
        CreditCardIssuanceRequest request = new CreditCardIssuanceRequest();
        request.setCardFormat(CardFormat.PHYSICAL);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditAccountService.getAccountEntity(accountId)).thenReturn(account);
        when(cardProductService.getActiveCardProductEntity(any())).thenReturn(new CreditCardProduct());
        when(customerAddressService.hasAddress(customer.getCustomerId())).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service.issueCard(userId, accountId, request));
    }

    @Test
    void validateMaxCardLimit_shouldThrow() {
        CreditCardIssuanceRequest request = new CreditCardIssuanceRequest();
        request.setCardFormat(CardFormat.VIRTUAL);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditAccountService.getAccountEntity(accountId)).thenReturn(account);
        when(cardProductService.getActiveCardProductEntity(any())).thenReturn(new CreditCardProduct());
        when(cardRepository.countByCreditAccountAccountIdAndCardStatusIn(any(), any()))
                .thenReturn(5);

        assertThrows(BusinessRuleException.class,
                () -> service.issueCard(userId, accountId, request));
    }

    // ================= GET CARD =================

    @Test
    void getCardById_success() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(new CreditCardResponse());

        CreditCardResponse response = service.getCardById(cardId);

        assertNotNull(response);
    }

    @Test
    void getCardById_notFound_shouldThrow() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getCardById(cardId));
    }

    // ================= UPDATE STATUS =================

    @Test
    void updateCardStatus_admin_validTransition() {
        CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
        when(principal.getRole()).thenReturn(UserRole.ADMIN);

        CreditCardStatusUpdateRequest request = new CreditCardStatusUpdateRequest();
        request.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenReturn(card);
        when(cardMapper.toIssueResponse(any())).thenReturn(null);

        service.updateCardStatusForUser(principal, accountId, cardId, request);

        assertEquals(CardStatus.ACTIVE, card.getCardStatus());
        assertNotNull(card.getActivatedAt());
    }

    @Test
    void updateCardStatus_invalidTransition_shouldThrow() {
        CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
        when(principal.getRole()).thenReturn(UserRole.ADMIN);

        card.setCardStatus(CardStatus.CANCELLED);

        CreditCardStatusUpdateRequest request = new CreditCardStatusUpdateRequest();
        request.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThrows(BusinessRuleException.class,
                () -> service.updateCardStatusForUser(principal, accountId, cardId, request));
    }

    @Test
    void updateCardStatus_sameStatus_shouldThrow() {
        CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
        when(principal.getRole()).thenReturn(UserRole.ADMIN);

        CreditCardStatusUpdateRequest request = new CreditCardStatusUpdateRequest();
        request.setStatus(CardStatus.PENDING_ACTIVATION);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThrows(ConflictException.class,
                () -> service.updateCardStatusForUser(principal, accountId, cardId, request));
    }

    @Test
    void updateCardStatus_wrongAccount_shouldThrow() {

        CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
        // ❌ No stubbing needed

        UUID differentAccountId = UUID.randomUUID();

        CreditCardStatusUpdateRequest request = new CreditCardStatusUpdateRequest();
        request.setStatus(CardStatus.ACTIVE);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThrows(AccessDeniedException.class,
                () -> service.updateCardStatusForUser(
                        principal,
                        differentAccountId,
                        cardId,
                        request
                ));
    }

    @Test
    void updateCardStatus_customerInvalidStatus_shouldThrow() {
        CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
        when(principal.getRole()).thenReturn(UserRole.CUSTOMER);
        when(principal.getUserId()).thenReturn(userId);

        CreditCardStatusUpdateRequest request = new CreditCardStatusUpdateRequest();
        request.setStatus(CardStatus.CANCELLED);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);

        assertThrows(BusinessRuleException.class,
                () -> service.updateCardStatusForUser(principal, accountId, cardId, request));
    }

    @Test
    void updateCardStatus_shouldSetBlockedAt() {
        CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
        when(principal.getRole()).thenReturn(UserRole.ADMIN);
        card.setCardStatus(CardStatus.ACTIVE);

        CreditCardStatusUpdateRequest request = new CreditCardStatusUpdateRequest();
        request.setStatus(CardStatus.BLOCKED);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenReturn(card);
        when(cardMapper.toIssueResponse(any())).thenReturn(null);

        service.updateCardStatusForUser(principal, accountId, cardId, request);

        assertNotNull(card.getBlockedAt());
    }

    // ================= GET CARDS =================

    @Test
    void getCardsByAccount_customer_success() {
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditAccountService.getAccountEntity(accountId)).thenReturn(account);
        when(cardRepository.findAllByCreditAccountAccountId(accountId)).thenReturn(List.of(card));
        when(cardMapper.toResponse(any())).thenReturn(new CreditCardResponse());

        List<CreditCardResponse> result = service.getCardsByAccount(userId, accountId);

        assertEquals(1, result.size());
    }

    @Test
    void getCardsByStatusForUser_admin_shouldCallAdminFlow() {
        CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
        when(principal.getRole()).thenReturn(UserRole.ADMIN);

        when(cardRepository.findAllByCardStatus(CardStatus.ACTIVE)).thenReturn(List.of(card));
        when(cardMapper.toIssueResponse(any())).thenReturn(null);

        List<?> result = service.getCardsByStatusForUser(principal, CardStatus.ACTIVE);

        assertNotNull(result);
    }
}