package com.br.mamba_wedding.gifts.api;

import com.br.mamba_wedding.config.security.SecurityFilter;
import com.br.mamba_wedding.gifts.application.GiftService;
import com.br.mamba_wedding.guests.domain.Guest;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = GiftController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
)
@Import(GuestGiftControllerTest.TestSecurityConfig.class)
class GuestGiftControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }

    @Autowired MockMvc mockMvc;
    @MockitoBean GiftService giftService;

    @Test
    void reserve_ShouldUseAuthenticatedGuestId() throws Exception {
        mockMvc.perform(post("/api/v1/events/5/gifts/3/reserve")
                        .with(authentication(guestAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quotas\":2}"))
                .andExpect(status().isNoContent());

        verify(giftService).reserve(5L, 3L, 9L, 2);
    }

    @Test
    void reserve_ShouldRejectNonPositiveQuota() throws Exception {
        mockMvc.perform(post("/api/v1/events/5/gifts/3/reserve")
                        .with(authentication(guestAuthentication()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quotas\":0}"))
                .andExpect(status().isBadRequest());

        verify(giftService, never()).reserve(anyLong(), anyLong(), anyLong(), anyInt());
    }

    @Test
    void cancel_ShouldUseAuthenticatedGuestId() throws Exception {
        mockMvc.perform(delete("/api/v1/events/5/gifts/3/reserve").with(authentication(guestAuthentication())))
                .andExpect(status().isNoContent());

        verify(giftService).cancelReserve(5L, 3L, 9L);
    }

    @Test
    void buy_ShouldUseAuthenticatedGuestId() throws Exception {
        mockMvc.perform(post("/api/v1/events/5/gifts/3/buy").with(authentication(guestAuthentication())))
                .andExpect(status().isNoContent());

        verify(giftService).buy(5L, 3L, 9L);
    }

    private UsernamePasswordAuthenticationToken guestAuthentication() {
        Guest guest = Guest.builder().id(9L).fullName("Convidado").build();
        return new UsernamePasswordAuthenticationToken(
                guest, null, List.of(new SimpleGrantedAuthority("ROLE_GUEST")));
    }
}
