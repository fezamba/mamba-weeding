package com.br.mamba_wedding.guests.api;

import com.br.mamba_wedding.config.security.SecurityFilter;
import com.br.mamba_wedding.guests.api.dto.RsvpResponse;
import com.br.mamba_wedding.guests.application.GuestRsvpService;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = GuestRsvpController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
)
@Import(GuestRsvpControllerTest.TestSecurityConfig.class)
class GuestRsvpControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                    .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                            (request, response, exception) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuestRsvpService guestRsvpService;

    private final Guest loggedGuest = Guest.builder()
            .id(7L)
            .fullName("Convidado Teste")
            .rsvpCode("CONV1234")
            .rsvpStatus(GuestStatus.PENDING)
            .build();

    @Test
    void me_ShouldReturnOnlyAuthenticatedGuestData() throws Exception {
        when(guestRsvpService.findCurrent(7L)).thenReturn(
                new RsvpResponse("Convidado Teste", GuestStatus.PENDING, "guest@mail.com", "21999999999", null));

        mockMvc.perform(get("/api/rsvp/me").with(authentication(guestAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Convidado Teste"))
                .andExpect(jsonPath("$.rsvpStatus").value("PENDING"))
                .andExpect(jsonPath("$.rsvpCode").doesNotExist());

        verify(guestRsvpService).findCurrent(7L);
    }

    @Test
    void confirm_ShouldUseAuthenticatedGuestId() throws Exception {
        mockMvc.perform(post("/api/rsvp/confirm")
                        .with(authentication(guestAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validActionBody("Até lá")))
                .andExpect(status().isNoContent());

        verify(guestRsvpService).confirm(7L, "guest@mail.com", "21999999999", "Até lá");
    }

    @Test
    void decline_ShouldUseAuthenticatedGuestId() throws Exception {
        mockMvc.perform(post("/api/rsvp/decline")
                        .with(authentication(guestAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validActionBody("Não poderei ir")))
                .andExpect(status().isNoContent());

        verify(guestRsvpService).decline(7L, "guest@mail.com", "21999999999", "Não poderei ir");
    }

    @Test
    void confirm_ShouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/rsvp/confirm")
                        .with(authentication(guestAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"123","email":"guest@mail.com","notes":"teste"}
                                """))
                .andExpect(status().isBadRequest());

        verify(guestRsvpService, never()).confirm(any(), any(), any(), any());
    }

    @Test
    void me_ShouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/rsvp/me"))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken guestAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                loggedGuest,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
    }

    private String validActionBody(String notes) {
        return """
                {"phone":"21999999999","email":"guest@mail.com","notes":"%s"}
                """.formatted(notes);
    }
}
