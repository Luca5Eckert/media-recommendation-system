package com.mrs.recommendation_service.provider;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org. springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserAuthenticationProvider {

    public UUID getUserId() {
        Jwt jwt = getJwt();
        String userId = jwt.getClaim("userId");
        return UUID. fromString(userId);
    }

    public String getEmail() {
        Jwt jwt = getJwt();
        return jwt.getSubject();
    }

    private Jwt getJwt() {
        return (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }
}