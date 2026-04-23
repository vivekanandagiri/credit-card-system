package com.example.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.testutil.TestFixtures;
import com.example.util.MaskedCardNumberGenerator;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import com.example.dto.request.*;
import com.example.dto.response.*;
import com.example.entity.*;
import com.example.enums.*;
import com.example.exception.*;
import com.example.mapper.CreditCardMapper;
import com.example.repository.CreditCardRepository;
import com.example.security.CustomUserPrincipal;
import com.example.service.ServiceImpl.CreditCardServiceImpl;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditCardServiceImplTest {

    @Mock private CreditCardRepository cardRepository;
    @Mock private CreditAccountService accountService;
    @Mock private CardProductService cardProductService;
    @Mock private CreditCardMapper mapper;
    @Mock private MaskedCardNumberGenerator generator;
    @Mock private CustomerService customerService;
    @Mock private CustomerAddressService addressService;

    @InjectMocks
    private CreditCardServiceImpl service;

    private UUID userId;
    private UUID accountId;
    private UUID cardId;

    private Customer customer;
    private CreditAccount account;
    private CreditCardProduct product;
    private CreditCard card;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        cardId = UUID.randomUUID();
        
        

        customer = TestFixtures.validCustomer();
        account = TestFixtures.validCreditAccount(customer, null, TestFixtures.validCreditProductEntity());
        account.setAccountId(accountId);  
        account.setAccountStatus(AccountStatus.ACTIVE);

        product = TestFixtures.validCreditCardProduct();

        card = new CreditCard();
        card.setCardId(cardId);
        card.setCreditAccount(account);
        card.setCardStatus(CardStatus.ACTIVE);
    }

    // ================= ISSUE CARD =================

    @Nested
    class IssueCard {

        @Test
        void shouldIssueCard_success() {
            CreditCardIssuanceRequest req = mock(CreditCardIssuanceRequest.class);

            when(req.getCardProductId()).thenReturn(UUID.randomUUID());
            when(req.getCardFormat()).thenReturn(CardFormat.VIRTUAL);
            when(req.getIssuanceReason()).thenReturn(CardIssuanceReason.NEW_CARD);

            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(accountService.getAccountEntity(accountId)).thenReturn(account);
            when(cardProductService.getActiveCardProductEntity(any())).thenReturn(product);
            when(generator.generate(any())).thenReturn("4111XXXXXX1234");
            when(cardRepository.countByCreditAccountAccountIdAndCardStatusIn(any(), any()))
                    .thenReturn(0);
            when(cardRepository.existsByCreditAccountAccountIdAndCardFormatAndCardStatusIn(any(), any(), any()))
                    .thenReturn(false);
            when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(mock(CreditCardResponse.class));

            CreditCardResponse response = service.issueCard(userId, accountId, req);

            assertThat(response).isNotNull();
        }

        @Test
        void shouldIssueCardByAdmin_success() {
            CreditCardIssuanceRequest req = mock(CreditCardIssuanceRequest.class);

            when(req.getCardProductId()).thenReturn(UUID.randomUUID());
            when(req.getCardFormat()).thenReturn(CardFormat.VIRTUAL);
            when(req.getIssuanceReason()).thenReturn(CardIssuanceReason.NEW_CARD);

            when(accountService.getAccountEntity(accountId)).thenReturn(account);
            when(cardProductService.getActiveCardProductEntity(any())).thenReturn(product);
            when(generator.generate(any())).thenReturn("4111XXXX");
            when(cardRepository.countByCreditAccountAccountIdAndCardStatusIn(any(), any())).thenReturn(0);
            when(cardRepository.existsByCreditAccountAccountIdAndCardFormatAndCardStatusIn(any(), any(), any()))
                    .thenReturn(false);
            when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(mock(CreditCardResponse.class));

            CreditCardResponse res = service.issueCardByAdmin(accountId, req);

            assertThat(res).isNotNull();
        }
        @Test
        void shouldThrow_whenAccountNotOwned() {
            Customer another = new Customer();
            another.setCustomerId(UUID.randomUUID());

            account.setCustomer(another);

            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(accountService.getAccountEntity(accountId)).thenReturn(account);

            assertThatThrownBy(() ->
                    service.issueCard(userId, accountId, mock(CreditCardIssuanceRequest.class)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void shouldThrow_whenAccountNotActive() {
            account.setAccountStatus(AccountStatus.BLOCKED);

            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(accountService.getAccountEntity(accountId)).thenReturn(account);

            assertThatThrownBy(() ->
                    service.issueCard(userId, accountId, mock(CreditCardIssuanceRequest.class)))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrow_whenVirtualCardAlreadyExists() {
            CreditCardIssuanceRequest req = mock(CreditCardIssuanceRequest.class);

            when(req.getCardProductId()).thenReturn(UUID.randomUUID());
            when(req.getCardFormat()).thenReturn(CardFormat.VIRTUAL);

            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(accountService.getAccountEntity(accountId)).thenReturn(account);
            when(cardProductService.getActiveCardProductEntity(any())).thenReturn(product);
            when(cardRepository.countByCreditAccountAccountIdAndCardStatusIn(any(), any()))
                    .thenReturn(0);
            when(cardRepository.existsByCreditAccountAccountIdAndCardFormatAndCardStatusIn(any(), any(), any()))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    service.issueCard(userId, accountId, req))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrow_whenPhysicalCardWithoutAddress() {
            CreditCardIssuanceRequest req = mock(CreditCardIssuanceRequest.class);

            when(req.getCardProductId()).thenReturn(UUID.randomUUID());
            when(req.getCardFormat()).thenReturn(CardFormat.PHYSICAL);

            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(accountService.getAccountEntity(accountId)).thenReturn(account);
            when(cardProductService.getActiveCardProductEntity(any())).thenReturn(product);
            when(addressService.hasAddress(any())).thenReturn(false);

            assertThatThrownBy(() ->
                    service.issueCard(userId, accountId, req))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }
    @Test
    void shouldThrow_whenMaxCardLimitReached() {
        CreditCardIssuanceRequest req = mock(CreditCardIssuanceRequest.class);

        when(req.getCardProductId()).thenReturn(UUID.randomUUID());
        when(req.getCardFormat()).thenReturn(CardFormat.VIRTUAL);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(cardProductService.getActiveCardProductEntity(any())).thenReturn(product);

        when(cardRepository.countByCreditAccountAccountIdAndCardStatusIn(any(), any()))
                .thenReturn(5); // 🔥 limit hit

        assertThatThrownBy(() ->
                service.issueCard(userId, accountId, req))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ================= GET CARD =================

    @Test
    void shouldGetCardById() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(mapper.toResponse(card)).thenReturn(mock(CreditCardResponse.class));

        CreditCardResponse res = service.getCardById(cardId);

        assertThat(res).isNotNull();
    }

    @Test
    void shouldThrow_whenCardNotFound() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCardById(cardId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
    
    @Test
    void shouldGetCardsByAccount_customer() {
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(accountService.getAccountEntity(accountId)).thenReturn(account);

        when(cardRepository.findAllByCreditAccountAccountId(accountId))
                .thenReturn(List.of(card));
        when(mapper.toResponse(any())).thenReturn(mock(CreditCardResponse.class));

        List<CreditCardResponse> res =
                service.getCardsByAccount(userId, accountId);

        assertThat(res).hasSize(1);
    }
    @Test
    void shouldGetCardsByAccount_admin() {
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(cardRepository.findAllByCreditAccountAccountId(accountId))
                .thenReturn(List.of(card));
        when(mapper.toResponse(any())).thenReturn(mock(CreditCardResponse.class));

        List<CreditCardResponse> res = service.getCardsByAccount(accountId);

        assertThat(res).hasSize(1);
    }
    @Test
    void shouldGetCardById_customer_success() {
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(mapper.toResponse(any())).thenReturn(mock(CreditCardResponse.class));

        CreditCardResponse res =
                service.getCardById(userId, accountId, cardId);

        assertThat(res).isNotNull();
    }
    @Test
    void shouldThrow_whenCardNotBelongToAccount() {
        CreditAccount another = new CreditAccount();
        another.setAccountId(UUID.randomUUID());
        card.setCreditAccount(another);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() ->
                service.getCardById(userId, accountId, cardId))
                .isInstanceOf(AccessDeniedException.class);
    }
    
    

    // ================= UPDATE STATUS =================

    @Nested
    class UpdateStatus {

        @Test
        void shouldUpdateStatus_admin() {
            CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
            CreditCardStatusUpdateRequest req = mock(CreditCardStatusUpdateRequest.class);

            when(principal.getRole()).thenReturn(UserRole.ADMIN);
            when(req.getStatus()).thenReturn(CardStatus.BLOCKED);

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
            when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.toIssueResponse(any())).thenReturn(mock(CreditCardIssuanceResponse.class));

            CreditCardIssuanceResponse res =
                    service.updateCardStatusForUser(principal, accountId, cardId, req);

            assertThat(res).isNotNull();
        }

        @Test
        void shouldThrow_whenInvalidTransition() {
            CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
            CreditCardStatusUpdateRequest req = mock(CreditCardStatusUpdateRequest.class);

            when(principal.getRole()).thenReturn(UserRole.ADMIN);
            when(req.getStatus()).thenReturn(CardStatus.PENDING_ACTIVATION); // invalid

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

            assertThatThrownBy(() ->
                    service.updateCardStatusForUser(principal, accountId, cardId, req))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrow_whenSameStatus() {
            CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
            CreditCardStatusUpdateRequest req = mock(CreditCardStatusUpdateRequest.class);

            when(principal.getRole()).thenReturn(UserRole.ADMIN);
            when(req.getStatus()).thenReturn(CardStatus.ACTIVE);

            when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

            assertThatThrownBy(() ->
                    service.updateCardStatusForUser(principal, accountId, cardId, req))
                    .isInstanceOf(ConflictException.class);
        }
    }

    // ================= GET CARDS BY STATUS =================

    @Test
    void shouldReturnCardsByStatus_admin() {
        when(cardRepository.findAllByCardStatus(CardStatus.ACTIVE))
                .thenReturn(List.of(card));
        when(mapper.toIssueResponse(any()))
                .thenReturn(mock(CreditCardIssuanceResponse.class));

        List<CreditCardIssuanceResponse> res =
                service.getCardsByStatus(CardStatus.ACTIVE);

        assertThat(res).hasSize(1);
    }
    
    @Test
    void shouldSetAllTimestamps() {
        CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
        CreditCardStatusUpdateRequest req = mock(CreditCardStatusUpdateRequest.class);

        when(principal.getRole()).thenReturn(UserRole.ADMIN);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toIssueResponse(any())).thenReturn(mock(CreditCardIssuanceResponse.class));

        // ACTIVE
        card.setCardStatus(CardStatus.PENDING_ACTIVATION);
        when(req.getStatus()).thenReturn(CardStatus.ACTIVE);
        service.updateCardStatusForUser(principal, accountId, cardId, req);
        assertThat(card.getActivatedAt()).isNotNull();

        // BLOCKED
        when(req.getStatus()).thenReturn(CardStatus.BLOCKED);
        service.updateCardStatusForUser(principal, accountId, cardId, req);
        assertThat(card.getBlockedAt()).isNotNull();

        // CANCELLED
        when(req.getStatus()).thenReturn(CardStatus.CANCELLED);
        service.updateCardStatusForUser(principal, accountId, cardId, req);
        assertThat(card.getCancelledAt()).isNotNull();
    }
    
    @Test
    void shouldThrow_whenCustomerInvalidStatusChange() {
        CustomUserPrincipal principal = mock(CustomUserPrincipal.class);
        CreditCardStatusUpdateRequest req = mock(CreditCardStatusUpdateRequest.class);

        when(principal.getRole()).thenReturn(UserRole.CUSTOMER);
        when(principal.getUserId()).thenReturn(userId);

        when(req.getStatus()).thenReturn(CardStatus.CANCELLED);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);

        assertThatThrownBy(() ->
                service.updateCardStatusForUser(principal, accountId, cardId, req))
                .isInstanceOf(BusinessRuleException.class);
    }
    @Test
    void shouldReturnCardEntity() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        CreditCard res = service.getCardEntity(cardId);

        assertThat(res).isNotNull();
    }
}