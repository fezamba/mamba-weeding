package com.br.mamba_wedding.config.security;

import com.br.mamba_wedding.common.api.ApiErrorWriter;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestNotFoundException;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final GuestRepository guestRepository;
    private final ApiErrorWriter apiErrorWriter;

    public SecurityFilter(TokenService tokenService, GuestRepository guestRepository, ApiErrorWriter apiErrorWriter) {
        this.tokenService = tokenService;
        this.guestRepository = guestRepository;
        this.apiErrorWriter = apiErrorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);
        
        if (token != null) {
            var decodedJWT = tokenService.validateAndDecodeToken(token);
            if (decodedJWT != null) {
                String subject = decodedJWT.getSubject();
                String roleString = decodedJWT.getClaim("role").asString();
                
                if (roleString == null || roleString.isBlank()) {
                    filterChain.doFilter(request, response);
                    return;
                }

                List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleString));
                Object principal = null;

                if ("ROLE_GUEST".equals(roleString)) {
                    try {
                        Guest guest = guestRepository.findByRsvpCode(subject)
                                .orElseThrow(GuestNotFoundException::new);
                        principal = guest;
                    } catch (GuestNotFoundException ex) {
                        SecurityContextHolder.clearContext();
                        apiErrorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                                "Token de convidado inválido.");
                        return;
                    }
                    
                } else if ("ROLE_ADMIN".equals(roleString)) {
                    principal = subject;
                }

                if (principal != null) {
                    var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }
}
