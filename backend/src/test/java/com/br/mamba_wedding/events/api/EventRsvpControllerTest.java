package com.br.mamba_wedding.events.api;

import com.br.mamba_wedding.common.api.PageResponse;
import com.br.mamba_wedding.config.security.SecurityFilter;
import com.br.mamba_wedding.events.api.dto.AdminRsvpResponse;
import com.br.mamba_wedding.events.api.dto.MyInvitationResponse;
import com.br.mamba_wedding.events.api.dto.RsvpResponse;
import com.br.mamba_wedding.events.api.dto.RsvpSummaryResponse;
import com.br.mamba_wedding.events.application.EventRsvpService;
import com.br.mamba_wedding.events.domain.EventType;
import com.br.mamba_wedding.events.domain.RsvpStatus;
import com.br.mamba_wedding.guests.domain.Guest;
import com.br.mamba_wedding.guests.domain.GuestSide;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {EventRsvpController.class, AdminEventRsvpController.class},
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
)
@Import(EventRsvpControllerTest.TestSecurityConfig.class)
class EventRsvpControllerTest {

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
    @MockitoBean EventRsvpService eventRsvpService;

    @Test
    void myInvitations_ShouldUseAuthenticatedGuest() throws Exception {
        when(eventRsvpService.findInvitations(7L)).thenReturn(List.of(
                new MyInvitationResponse(
                        1L, "casamento", EventType.WEDDING, "Casamento", null,
                        null, null, null, null, null, RsvpStatus.CONFIRMED, null),
                new MyInvitationResponse(
                        2L, "cha-de-panelas", EventType.BRIDAL_SHOWER, "Chá de panelas", null,
                        null, null, null, null, null, RsvpStatus.PENDING, null)
        ));

        mockMvc.perform(get("/api/v1/events/my-invitations")
                        .with(authentication(guestAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("WEDDING"))
                .andExpect(jsonPath("$[0].rsvpStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$[1].type").value("BRIDAL_SHOWER"))
                .andExpect(jsonPath("$[1].rsvpStatus").value("PENDING"));

        verify(eventRsvpService).findInvitations(7L);
    }

    @Test
    void me_ShouldSelectEventAndAuthenticatedGuest() throws Exception {
        when(eventRsvpService.findCurrent(2L, 7L)).thenReturn(
                new RsvpResponse(
                        2L, "cha-de-panelas", "Chá de panelas", "Convidada",
                        RsvpStatus.PENDING, null, "guest@mail.com", "21999999999", null));

        mockMvc.perform(get("/api/v1/events/2/rsvp/me")
                        .with(authentication(guestAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(2))
                .andExpect(jsonPath("$.rsvpStatus").value("PENDING"));

        verify(eventRsvpService).findCurrent(2L, 7L);
    }

    @Test
    void confirm_ShouldUpdateOnlyRequestedEvent() throws Exception {
        mockMvc.perform(post("/api/v1/events/2/rsvp/confirm")
                        .with(authentication(guestAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validActionBody()))
                .andExpect(status().isNoContent());

        verify(eventRsvpService).confirm(2L, 7L, "guest@mail.com", "21999999999", "Até lá");
    }

    @Test
    void adminList_ShouldApplyFiltersAndRequireAdmin() throws Exception {
        var item = new AdminRsvpResponse(
                7L, "Convidada", GuestSide.BRIDE, "guest@mail.com", "21999999999",
                RsvpStatus.CONFIRMED, null, "Sem glúten");
        when(eventRsvpService.search(
                eq(1L),
                eq("convidada"),
                eq(RsvpStatus.CONFIRMED),
                eq(GuestSide.BRIDE),
                any()))
                .thenReturn(new PageResponse<>(List.of(item), 0, 20, 1, 1, true, true));

        mockMvc.perform(get("/api/v1/admin/events/1/rsvps")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .param("name", "convidada")
                        .param("status", "CONFIRMED")
                        .param("side", "BRIDE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Convidada"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/admin/events/1/rsvps")
                        .with(authentication(guestAuthentication())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSummary_ShouldReturnCounts() throws Exception {
        when(eventRsvpService.summary(1L)).thenReturn(
                new RsvpSummaryResponse(1L, "Casamento", 10, 4, 5, 1));

        mockMvc.perform(get("/api/v1/admin/events/1/rsvps/summary")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.pending").value(4))
                .andExpect(jsonPath("$.confirmed").value(5))
                .andExpect(jsonPath("$.rejected").value(1));
    }

    @Test
    void confirm_ShouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/v1/events/1/rsvp/confirm")
                        .with(authentication(guestAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"123\",\"email\":\"guest@mail.com\"}"))
                .andExpect(status().isBadRequest());

        verify(eventRsvpService, never()).confirm(any(), any(), any(), any(), any());
    }

    private UsernamePasswordAuthenticationToken guestAuthentication() {
        Guest guest = Guest.builder().id(7L).fullName("Convidada").build();
        return new UsernamePasswordAuthenticationToken(
                guest, null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
    }

    private String validActionBody() {
        return """
                {"phone":"21999999999","email":"guest@mail.com","notes":"Até lá"}
                """;
    }
}
