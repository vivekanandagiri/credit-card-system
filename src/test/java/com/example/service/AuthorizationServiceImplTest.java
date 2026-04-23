package com.example.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import com.example.entity.Authorization;
import com.example.entity.CreditAccount;
import com.example.enums.AuthStatus;
import com.example.exception.BadRequestException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.AuthorizationRepository;
import com.example.service.ServiceImpl.AuthorizationServiceImpl;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    @Mock private AuthorizationRepository repo;
    @Mock private CreditAccountService accountService;
    @Mock private LedgerService ledgerService;

    @InjectMocks
    private AuthorizationServiceImpl service;

    private UUID accountId;
    private UUID cardId;
    private UUID authId;

    private CreditAccount account;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        cardId = UUID.randomUUID();
        authId = UUID.randomUUID();

        account = new CreditAccount();
        account.setAccountId(accountId);
        account.setCreditLimit(BigDecimal.valueOf(10000));
    }

    // ================= AUTHORIZE =================

    @Test
    void shouldAuthorize_success() {
        when(repo.findByNetworkReference("REF")).thenReturn(Optional.empty());
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(ledgerService.getBalance(accountId)).thenReturn(BigDecimal.ZERO);
        when(repo.sumActiveHolds(accountId)).thenReturn(BigDecimal.ZERO);

        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Authorization res =
                service.authorize(accountId, cardId, BigDecimal.valueOf(1000), "REF");

        assertThat(res).isNotNull();
        assertThat(res.getStatus()).isEqualTo(AuthStatus.AUTHORIZED);
    }

    @Test
    void shouldReturnExistingAuthorization_idempotency() {
        Authorization existing = new Authorization();

        when(repo.findByNetworkReference("REF"))
                .thenReturn(Optional.of(existing));

        Authorization res =
                service.authorize(accountId, cardId, BigDecimal.valueOf(1000), "REF");

        assertThat(res).isEqualTo(existing);
        verify(repo, never()).save(any());
    }

    @Test
    void shouldThrow_whenAmountInvalid() {
        assertThatThrownBy(() ->
                service.authorize(accountId, cardId, BigDecimal.ZERO, "REF"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenInsufficientLimit() {
        when(repo.findByNetworkReference("REF")).thenReturn(Optional.empty());
        when(accountService.getAccountEntity(accountId)).thenReturn(account);

        when(ledgerService.getBalance(accountId))
                .thenReturn(BigDecimal.valueOf(9000)); // outstanding
        when(repo.sumActiveHolds(accountId))
                .thenReturn(BigDecimal.valueOf(2000)); // holds

        assertThatThrownBy(() ->
                service.authorize(accountId, cardId, BigDecimal.valueOf(1000), "REF"))
                .isInstanceOf(BadRequestException.class);
    }

    // ================= CAPTURE =================

    @Test
    void shouldCapture_success() {
        Authorization auth = new Authorization();
        auth.setId(authId);
        auth.setStatus(AuthStatus.AUTHORIZED);
        auth.setExpiresAt(Instant.now().plusSeconds(60));

        when(repo.findById(authId)).thenReturn(Optional.of(auth));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        Authorization res = service.capture(authId);

        assertThat(res.getStatus()).isEqualTo(AuthStatus.CAPTURED);
    }

    @Test
    void shouldThrow_whenAuthorizationNotFound_capture() {
        when(repo.findById(authId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.capture(authId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldExpireAndThrow_whenAuthorizationExpired() {
        Authorization auth = new Authorization();
        auth.setStatus(AuthStatus.AUTHORIZED);
        auth.setExpiresAt(Instant.now().minusSeconds(10));

        when(repo.findById(authId)).thenReturn(Optional.of(auth));

        assertThatThrownBy(() -> service.capture(authId))
                .isInstanceOf(BadRequestException.class);

        assertThat(auth.getStatus()).isEqualTo(AuthStatus.EXPIRED);
        verify(repo).save(auth);
    }

    @Test
    void shouldThrow_whenInvalidStateForCapture() {
        Authorization auth = new Authorization();
        auth.setStatus(AuthStatus.CAPTURED); // not AUTHORIZED

        when(repo.findById(authId)).thenReturn(Optional.of(auth));

        assertThatThrownBy(() -> service.capture(authId))
                .isInstanceOf(BadRequestException.class);
    }

    // ================= EXPIRE =================

    @Test
    void shouldExpire_whenAuthorized() {
        Authorization auth = new Authorization();
        auth.setStatus(AuthStatus.AUTHORIZED);

        when(repo.findById(authId)).thenReturn(Optional.of(auth));

        service.expire(authId);

        assertThat(auth.getStatus()).isEqualTo(AuthStatus.EXPIRED);
        verify(repo).save(auth);
    }

    @Test
    void shouldNotExpire_whenAlreadyExpired() {
        Authorization auth = new Authorization();
        auth.setStatus(AuthStatus.EXPIRED);

        when(repo.findById(authId)).thenReturn(Optional.of(auth));

        service.expire(authId);

        verify(repo, never()).save(any());
    }

    @Test
    void shouldThrow_whenAuthorizationNotFound_expire() {
        when(repo.findById(authId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.expire(authId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ================= REVERSE =================

    @Test
    void shouldReverse_success() {
        Authorization auth = new Authorization();
        auth.setStatus(AuthStatus.AUTHORIZED);

        when(repo.findById(authId)).thenReturn(Optional.of(auth));

        service.reverse(authId);

        assertThat(auth.getStatus()).isEqualTo(AuthStatus.REVERSED);
        verify(repo).save(auth);
    }

    @Test
    void shouldThrow_whenReverseInvalidState() {
        Authorization auth = new Authorization();
        auth.setStatus(AuthStatus.CAPTURED);

        when(repo.findById(authId)).thenReturn(Optional.of(auth));

        assertThatThrownBy(() -> service.reverse(authId))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenAuthorizationNotFound_reverse() {
        when(repo.findById(authId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reverse(authId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}