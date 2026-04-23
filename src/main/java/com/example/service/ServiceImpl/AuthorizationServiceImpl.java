package com.example.service.ServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entity.Authorization;
import com.example.entity.CreditAccount;
import com.example.enums.AuthStatus;
import com.example.exception.BadRequestException;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.AuthorizationRepository;
import com.example.service.AuthorizationService;
import com.example.service.CreditAccountService;
import com.example.service.LedgerService;

import lombok.RequiredArgsConstructor;
/**
 * Service responsible for handling card authorization lifecycle.
 *
 * <p><b>Authorization Flow:</b></p>
 * <ul>
 *     <li><b>AUTHORIZE</b> → Reserve credit (creates hold)</li>
 *     <li><b>CAPTURE</b> → Finalize transaction (convert hold to charge)</li>
 *     <li><b>REVERSE</b> → Release hold before capture</li>
 *     <li><b>EXPIRE</b> → Auto-expire unused authorization</li>
 * </ul>
 *
 * <p><b>Key Rules:</b></p>
 * <ul>
 *     <li>Authorizations reduce available credit but do not impact ledger balance</li>
 *     <li>Captures are the point where financial liability is confirmed</li>
 *     <li>Authorizations are time-bound (default: 1 day)</li>
 *     <li>Idempotency is enforced via network reference</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthorizationServiceImpl implements AuthorizationService {

    private final AuthorizationRepository repo;
    private final CreditAccountService accountService;
    private final LedgerService ledgerService;

    /**
     * Creates a new authorization (credit hold) for a transaction.
     *
     * <p>This method:
     * <ul>
     *     <li>Validates amount</li>
     *     <li>Ensures idempotency using network reference</li>
     *     <li>Checks available credit (limit - outstanding - holds)</li>
     *     <li>Creates a time-bound authorization</li>
     * </ul>
     *
     * @param accountId   credit account ID
     * @param cardId      card identifier
     * @param amount      transaction amount (must be > 0)
     * @param networkRef  unique network reference for idempotency
     * @return created or existing {@link Authorization}
     * @throws BadRequestException if amount invalid or insufficient credit
     */
    @Override
    public Authorization authorize(UUID accountId, UUID cardId,
                                   BigDecimal amount, String networkRef) {

        //  Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }

        // Idempotency (network reference)
        Optional<Authorization> existing =
                repo.findByNetworkReference(networkRef);

        if (existing.isPresent()) {
            return existing.get();
        }

        CreditAccount account = accountService.getAccountEntity(accountId);

        //  Outstanding from ledger (source of truth)
        BigDecimal outstanding = ledgerService.getBalance(accountId).abs();

        // Active authorization holds
        BigDecimal holds = repo.sumActiveHolds(accountId);
        // Available credit calculation
        BigDecimal available = account.getCreditLimit()
                .subtract(outstanding)
                .subtract(holds);

        if (available.compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient credit limit");
        }

        Authorization auth = new Authorization();
        auth.setId(UUID.randomUUID());
        auth.setAccountId(accountId);
        auth.setCardId(cardId);
        auth.setAmount(amount);
        auth.setStatus(AuthStatus.AUTHORIZED);
        auth.setNetworkReference(networkRef);
        auth.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));

        return repo.save(auth);
    }
    /**
     * Captures a previously authorized transaction.
     *
     * <p>Rules:
     * <ul>
     *     <li>Authorization must exist</li>
     *     <li>Must not be expired</li>
     *     <li>Status must be AUTHORIZED</li>
     * </ul>
     *
     * <p>On success, status transitions to <b>CAPTURED</b>.</p>
     *
     * @param authId authorization ID
     * @return updated {@link Authorization}
     * @throws ResourceNotFoundException if authorization not found
     * @throws BadRequestException if expired or invalid state
     */
    @Override
    public Authorization capture(UUID authId) {

        Authorization auth = repo.findById(authId)
                .orElseThrow(() -> new ResourceNotFoundException("Authorization not found"));

        //  Check expiry
        if (auth.getExpiresAt() != null &&
                auth.getExpiresAt().isBefore(Instant.now())) {

            auth.setStatus(AuthStatus.EXPIRED);
            repo.save(auth);

            throw new BadRequestException("Authorization expired");
        }

        //  Only AUTHORIZED → CAPTURED allowed (Validate state)
        if (auth.getStatus() != AuthStatus.AUTHORIZED) {
            throw new BadRequestException("Authorization cannot be captured in current state");
        }

        auth.setStatus(AuthStatus.CAPTURED);
        return repo.save(auth);
    }
    /**
     * Expires an active authorization.
     *
     * <p>This is typically triggered by a scheduled job when the authorization
     * validity window lapses.</p>
     *
     * @param authId authorization ID
     * @throws ResourceNotFoundException if authorization not found
     */
    @Override
    public void expire(UUID authId) {

        Authorization auth = repo.findById(authId)
                .orElseThrow(() -> new ResourceNotFoundException("Authorization not found"));

        // Only expire if still active
        if (auth.getStatus() == AuthStatus.AUTHORIZED) {
            auth.setStatus(AuthStatus.EXPIRED);
            repo.save(auth);
        }
    }

    /**
     * Reverses an authorization (releases hold).
     *
     * <p>This is used when a merchant cancels a transaction before capture.</p>
     *
     * <p>Rules:
     * <ul>
     *     <li>Only AUTHORIZED transactions can be reversed</li>
     * </ul>
     *
     * @param authId authorization ID
     * @throws ResourceNotFoundException if authorization not found
     * @throws BadRequestException if invalid state
     */
    public void reverse(UUID authId) {

        Authorization auth = repo.findById(authId)
                .orElseThrow(() -> new ResourceNotFoundException("Authorization not found"));

        if (auth.getStatus() != AuthStatus.AUTHORIZED) {
            throw new BadRequestException("Only active authorization can be reversed");
        }

        auth.setStatus(AuthStatus.REVERSED);
        repo.save(auth);
    }
}