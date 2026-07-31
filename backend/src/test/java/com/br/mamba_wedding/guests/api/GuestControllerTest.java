package com.br.mamba_wedding.guests.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.br.mamba_wedding.config.security.SecurityFilter;
import com.br.mamba_wedding.config.security.PublicEndpointRateLimiter;
import com.br.mamba_wedding.guests.api.dto.GuestCreated;
import com.br.mamba_wedding.guests.application.GuestRsvpService;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestNotFoundException;
import com.br.mamba_wedding.guests.domain.GuestSide;
import com.br.mamba_wedding.guests.domain.GuestStatus;

@WebMvcTest(
    controllers = {GuestRsvpController.class, AdminGuestController.class},
    excludeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
    }
)

@Import(GuestControllerTest.TestSecurityConfig.class)
class GuestControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/admin/guests/**").hasAuthority("ROLE_ADMIN")
                            .anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuestRsvpService guestRsvpService;
    
    @MockitoBean
    private PublicEndpointRateLimiter publicEndpointRateLimiter;

    String json =   """
                        {
                        "fullName": "Convidado Teste",
                        "rsvpCode": "CONV1234",
                        "rsvpStatus": "PENDING",
                        "side": "BRIDE",
                        "email": "convidado.teste@gmail.com",
                        "phone": "21999999999"
                        }
                    """;

    private Guest sampleGuest() {
        return Guest.builder()
                .fullName("Convidado Teste")
                .rsvpCode("CONV1234")
                .rsvpStatus(GuestStatus.PENDING)
                .side(GuestSide.BRIDE)
                .email("convidado.teste@gmail.com")
                .phone("21999999999")
                .build();
    }

    @Test
    void register_ShouldAllowAdmin() throws Exception {
        when(guestRsvpService.register(any())).thenReturn(new GuestCreated(sampleGuest()));

        mockMvc.perform(post("/api/v1/admin/guests/register")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        verify(guestRsvpService).register(any());
    }

    @Test
    void register_ShouldReturnForbidden_WhenUserIsGuest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/guests/register")
                .with(user("guest").authorities(new SimpleGrantedAuthority("ROLE_GUEST")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden());

        verify(guestRsvpService, never()).register(any());
    }

    @Test
    void register_ShouldReturnForbidden_WhenUserIsAnonymous() throws Exception {
        mockMvc.perform(post("/api/v1/admin/guests/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden());

        verify(guestRsvpService, never()).register(any());
    }

    @Test
    void delete_ShouldAllowAdmin() throws Exception {
        doNothing().when(guestRsvpService).delete(1L);

        mockMvc.perform(delete("/api/v1/admin/guests/{id}/delete", 1L)
                .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(guestRsvpService).delete(1L);
    }

    @Test
    void delete_ShouldReturnForbidden_WhenUserIsGuest() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/guests/{id}/delete", 1L)
                .with(user("guest").authorities(new SimpleGrantedAuthority("ROLE_GUEST"))))
                .andExpect(status().isForbidden());

        verify(guestRsvpService, never()).delete(1L);
    }

    @Test
    void delete_ShouldReturnForbidden_WhenUserIsAnonymous() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/guests/{id}/delete", 1L))
                .andExpect(status().isForbidden());

        verify(guestRsvpService, never()).delete(1L);
    }

    @Test
    void delete_ShouldReturnNotFound_WhenGuestDoesNotExist() throws Exception {
        doThrow(new GuestNotFoundException())
            .when(guestRsvpService).delete(99L);

        mockMvc.perform(delete("/api/v1/admin/guests/{id}/delete", 99L)
            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isNotFound());
    }
}
