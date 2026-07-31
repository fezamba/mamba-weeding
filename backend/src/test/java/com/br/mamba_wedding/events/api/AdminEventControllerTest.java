package com.br.mamba_wedding.events.api;

import com.br.mamba_wedding.config.security.SecurityFilter;
import com.br.mamba_wedding.events.api.dto.EventResponse;
import com.br.mamba_wedding.events.api.dto.EventUpdateRequest;
import com.br.mamba_wedding.events.application.EventService;
import com.br.mamba_wedding.events.domain.EventType;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AdminEventController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
)
@Import(AdminEventControllerTest.TestSecurityConfig.class)
class AdminEventControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
                            .anyRequest().authenticated())
                    .build();
        }
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean EventService eventService;

    @Test
    void list_ShouldReturnConfiguredEventsToAdmin() throws Exception {
        when(eventService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/admin/events").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("WEDDING"))
                .andExpect(jsonPath("$[0].venueName").value("Espaço Jardim"));
    }

    @Test
    void update_ShouldValidateAndReturnEvent() throws Exception {
        when(eventService.update(eq(1L), any(EventUpdateRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/v1/admin/events/1")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mapUrl").value("https://maps.example.com/casamento"));

        verify(eventService).update(eq(1L), any(EventUpdateRequest.class));
    }

    @Test
    void update_ShouldRejectInvalidMapUrl() throws Exception {
        mockMvc.perform(put("/api/v1/admin/events/1")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody().replace("https://maps.example.com/casamento", "maps-invalido")))
                .andExpect(status().isBadRequest());

        verify(eventService, never()).update(any(), any());
    }

    @Test
    void endpoints_ShouldRejectGuestRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events")
                        .with(user("guest").authorities(new SimpleGrantedAuthority("ROLE_GUEST"))))
                .andExpect(status().isForbidden());
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private EventResponse response() {
        return new EventResponse(
                1L,
                "casamento",
                EventType.WEDDING,
                "Casamento",
                "Cerimônia e recepção",
                LocalDateTime.of(2027, 5, 15, 16, 30),
                "Espaço Jardim",
                "Rua das Flores, 100",
                "https://maps.example.com/casamento",
                "Esporte fino"
        );
    }

    private String validBody() {
        return """
                {
                  "eventDateTime": "2027-05-15T16:30:00",
                  "venueName": "Espaço Jardim",
                  "address": "Rua das Flores, 100",
                  "mapUrl": "https://maps.example.com/casamento",
                  "description": "Cerimônia e recepção",
                  "dressCode": "Esporte fino"
                }
                """;
    }
}
