package com.br.mamba_wedding.guests.api;

import com.br.mamba_wedding.common.exception.TooManyRequestsException;
import com.br.mamba_wedding.config.security.PublicEndpointRateLimiter;
import com.br.mamba_wedding.config.security.SecurityFilter;
import com.br.mamba_wedding.config.security.TokenService;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestNotFoundException;
import com.br.mamba_wedding.guests.infrastructure.GuestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = {
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
        }
)
@Import(AuthControllerTest.TestSecurityConfig.class)
class AuthControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuestRepository guestRepository;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private PublicEndpointRateLimiter rateLimiter;

    @Test
    void login_ShouldReturnTokenForExistingGuest() throws Exception {
        Guest guest = Guest.builder()
                .rsvpCode("ABC1234")
                .fullName("Convidado Teste")
                .rsvpStatus(com.br.mamba_wedding.guests.domain.GuestStatus.PENDING)
                .build();

        when(guestRepository.findByRsvpCode("ABC1234")).thenReturn(Optional.of(guest));
        when(tokenService.generateToken("ABC1234", "ROLE_GUEST")).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rsvpCode": "ABC1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.fullName").value("Convidado Teste"))
                .andExpect(jsonPath("$.rsvpStatus").value("PENDING"));

        verify(rateLimiter).assertAllowed(any(), eq("auth-login"), eq("ABC1234"), eq(10), eq(Duration.ofMinutes(1)));
        verify(tokenService).generateToken("ABC1234", "ROLE_GUEST");
    }

    @Test
    void login_ShouldReturnNotFoundWhenGuestDoesNotExist() throws Exception {
        when(guestRepository.findByRsvpCode("MISSING")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rsvpCode": "MISSING"
                                }
                                """))
                                                                .andExpect(status().isNotFound())
                                                                .andExpect(jsonPath("$.status").value(404))
                                                                .andExpect(jsonPath("$.error").value("Not Found"))
                                                                .andExpect(jsonPath("$.path").value("/api/auth/login"));

        verify(tokenService, never()).generateToken(any(), any());
    }

    @Test
    void login_ShouldReturnBadRequestForInvalidInput() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rsvpCode": ""
                                }
                                """))
                                                                .andExpect(status().isBadRequest())
                                                                .andExpect(jsonPath("$.status").value(400))
                                                                .andExpect(jsonPath("$.error").value("Bad Request"))
                                                                .andExpect(jsonPath("$.path").value("/api/auth/login"));

        verify(guestRepository, never()).findByRsvpCode(any());
    }

    @Test
    void login_ShouldReturnTooManyRequestsWhenRateLimited() throws Exception {
        when(guestRepository.findByRsvpCode("ABC1234")).thenReturn(Optional.of(Guest.builder().rsvpCode("ABC1234").build()));
        when(tokenService.generateToken("ABC1234", "ROLE_GUEST")).thenReturn("jwt-token");

        org.mockito.Mockito.doThrow(new TooManyRequestsException("Muitas tentativas."))
                .when(rateLimiter)
                .assertAllowed(any(), eq("auth-login"), eq("ABC1234"), eq(10), eq(Duration.ofMinutes(1)));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rsvpCode": "ABC1234"
                                }
                                """))
                                                                .andExpect(status().isTooManyRequests())
                                                                .andExpect(jsonPath("$.status").value(429))
                                                                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                                                                .andExpect(jsonPath("$.path").value("/api/auth/login"));

        verify(tokenService, never()).generateToken(any(), any());
    }

    @Test
    void login_ShouldReturnNotFoundFromRepositoryException() throws Exception {
        when(guestRepository.findByRsvpCode("UNKNOWN")).thenThrow(new GuestNotFoundException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rsvpCode": "UNKNOWN"
                                }
                                """))
                                                                .andExpect(status().isNotFound())
                                                                .andExpect(jsonPath("$.status").value(404))
                                                                .andExpect(jsonPath("$.error").value("Not Found"))
                                                                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }
}
