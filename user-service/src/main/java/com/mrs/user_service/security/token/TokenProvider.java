package com.mrs.user_service.security.token;

import com.mrs.user_service.model.RoleUser;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest; // Ou javax, dependendo da versão do Spring
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public interface TokenProvider {

    String createToken(String email, Collection<? extends GrantedAuthority> grantedAuthorities);

}