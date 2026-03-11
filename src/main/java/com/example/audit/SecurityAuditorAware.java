package com.example.audit;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.security.CustomUserPrincipal;

@Component("auditorProvider")
public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        // no authentication OR anonymous login
        if (authentication == null ||
            !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken) {

            return Optional.of("SELF_REGISTRATION");
        }
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserPrincipal user) {
            //return Optional.of(user.getUserId().toString());  
            return Optional.of(user.getEmail());
        }

        return Optional.empty();
    }
}