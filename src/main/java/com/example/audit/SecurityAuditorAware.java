package com.example.audit;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.security.CustomUserPrincipal;
/**
 * Provides the current auditor (user) for JPA auditing.
 *
 * <p>This implementation retrieves the currently authenticated user
 * from Spring Security context and returns a unique identifier
 * (email in this case).
 *
 * <p><b>Behavior:</b>
 * <ul>
 *     <li>If user is authenticated → returns user email</li>
 *     <li>If anonymous or unauthenticated → returns "SELF_REGISTRATION"</li>
 *     <li>If principal type is unexpected → returns "SYSTEM"</li>
 * </ul>
 *
 * <p><b>Used by:</b>
 * {@code @EnableJpaAuditing(auditorAwareRef = "auditorProvider")}
 *
 * <p><b>Typical use cases:</b>
 * <ul>
 *     <li>Tracking createdBy / updatedBy fields</li>
 *     <li>Auditing user actions</li>
 *     <li>System-level operations</li>
 * </ul>
 */
@Component("auditorProvider")
public class SecurityAuditorAware implements AuditorAware<String> {

    /**
     * Returns the current auditor (user identifier).
     *
     * @return Optional containing user email or fallback value
     */
    @Override
    public Optional<String> getCurrentAuditor() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        // Case 1: No authentication OR anonymous user
        if (authentication == null ||
            !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken) {

            return Optional.of("SELF_REGISTRATION");
        }
        Object principal = authentication.getPrincipal();

        // Case 2: Custom authenticated user
        if (principal instanceof CustomUserPrincipal user) {
            return Optional.of(user.getEmail());//here i used email 
            // Alternative:
            // return Optional.of(user.getUserId().toString());
        }

        // Case 3: Unexpected principal (e.g., system/internal calls)
        return Optional.empty();
    }
}