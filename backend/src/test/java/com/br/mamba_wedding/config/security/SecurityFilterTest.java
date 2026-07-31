package com.br.mamba_wedding.config.security;

import com.br.mamba_wedding.common.api.ApiErrorWriter;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SecurityFilterTest {

    private TokenService tokenService;
    private GuestRepository guestRepository;
    private SecurityFilter securityFilter;

    @BeforeEach
    void setUp() {
        tokenService = mock(TokenService.class);
        guestRepository = mock(GuestRepository.class);
        securityFilter = new SecurityFilter(tokenService, guestRepository, new ApiErrorWriter(new ObjectMapper()));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_ShouldContinueWhenAuthorizationHeaderIsMissing() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        securityFilter.doFilter(request, response, chain);

        verifyNoInteractions(tokenService);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_ShouldAuthenticateGuestFromToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        Claim roleClaim = mock(Claim.class);
        Guest guest = Guest.builder().rsvpCode("ABC1234").fullName("Guest User").build();

        when(tokenService.validateAndDecodeToken("valid-token")).thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn("ABC1234");
        when(decodedJWT.getClaim("role")).thenReturn(roleClaim);
        when(roleClaim.asString()).thenReturn("ROLE_GUEST");
        when(guestRepository.findByRsvpCode("ABC1234")).thenReturn(Optional.of(guest));

        securityFilter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("ROLE_GUEST", authentication.getAuthorities().iterator().next().getAuthority());
        assertEquals(guest, authentication.getPrincipal());
    }

    @Test
    void doFilterInternal_ShouldAuthenticateAdminAsSubject() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer admin-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        Claim roleClaim = mock(Claim.class);

        when(tokenService.validateAndDecodeToken("admin-token")).thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn("admin@example.com");
        when(decodedJWT.getClaim("role")).thenReturn(roleClaim);
        when(roleClaim.asString()).thenReturn("ROLE_ADMIN");

        securityFilter.doFilter(request, response, chain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(authentication);
        assertEquals("admin@example.com", authentication.getPrincipal());
        assertEquals("ROLE_ADMIN", authentication.getAuthorities().iterator().next().getAuthority());
        verifyNoInteractions(guestRepository);
    }

    @Test
    void doFilterInternal_ShouldReturnUnauthorizedForUnknownGuest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer guest-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        Claim roleClaim = mock(Claim.class);

        when(tokenService.validateAndDecodeToken("guest-token")).thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn("UNKNOWN");
        when(decodedJWT.getClaim("role")).thenReturn(roleClaim);
        when(roleClaim.asString()).thenReturn("ROLE_GUEST");
        when(guestRepository.findByRsvpCode("UNKNOWN")).thenReturn(Optional.empty());

        securityFilter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        org.assertj.core.api.Assertions.assertThat(response.getContentType())
                .startsWith("application/json");
        org.assertj.core.api.Assertions.assertThat(response.getContentAsString())
                .contains("Token de convidado inválido.", "\"status\":401");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(guestRepository).findByRsvpCode("UNKNOWN");
    }

    @Test
    void doFilterInternal_ShouldIgnoreTokenWithBlankRole() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer blank-role-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        Claim roleClaim = mock(Claim.class);

        when(tokenService.validateAndDecodeToken("blank-role-token")).thenReturn(decodedJWT);
        when(decodedJWT.getClaim("role")).thenReturn(roleClaim);
        when(roleClaim.asString()).thenReturn(" ");

        securityFilter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
