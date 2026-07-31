package com.br.mamba_wedding.gifts.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.br.mamba_wedding.config.security.SecurityFilter;
import com.br.mamba_wedding.common.exception.NotFoundException;
import com.br.mamba_wedding.events.domain.Event;
import com.br.mamba_wedding.events.domain.EventType;
import com.br.mamba_wedding.gifts.api.dto.GiftCreated;
import com.br.mamba_wedding.gifts.application.GiftService;
import com.br.mamba_wedding.gifts.domain.Gift;

@WebMvcTest(
    controllers = {GiftController.class, AdminGiftController.class},
    excludeFilters = {
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityFilter.class)
    }
)

@Import(GiftControllerTest.TestSecurityConfig.class)
class GiftControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
                            .anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GiftService giftService;

    String json = """
                {
                "name": "Geladeira",
                "description": "Geladeira duas portas preta",
                "value": 2500.00,
                "totalQuotas": 10,
                "imageUrl": "url-geladeira-duas-portas",
                "purchaseLink": "url-compra-geladeira"
                }
                """;

    private Gift sampleGift() {
        return Gift.builder()
                .id(1L)
                .event(Event.builder().id(5L).type(EventType.WEDDING).title("Casamento").build())
                .name("Geladeira")
                .description("Geladeira duas portas preta")
                .value(new BigDecimal("2500.00"))
                .imageUrl("url-geladeira-duas-portas")
                .purchaseLink("url-compra-geladeira")
                .totalQuotas(10)
                .build();
    }

    @Test
    void list_ShouldReturnStablePageEnvelopeAndApplyNameFilter() throws Exception {
        when(giftService.listAll(eq(5L), eq("gela"), any())).thenReturn(
                new PageImpl<>(List.of(sampleGift()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/events/5/gifts")
                        .param("name", "gela")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Geladeira"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void adminList_ShouldUseRequestedEvent() throws Exception {
        when(giftService.listAll(eq(5L), eq(null), any())).thenReturn(
                new PageImpl<>(List.of(sampleGift()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/events/5/gifts")
                        .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].eventId").value(5));

        verify(giftService).listAll(eq(5L), eq(null), any());
    }

    @Test
    void list_ShouldRejectPageSizeAboveLimit() throws Exception {
        mockMvc.perform(get("/api/v1/events/5/gifts").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(giftService, never()).listAll(anyLong(), any(), any());
    }

    @Test
    void register_ShouldAllowAdmin() throws Exception {
        when(giftService.register(eq(5L), any())).thenReturn(new GiftCreated(sampleGift()));

        mockMvc.perform(post("/api/v1/admin/events/5/gifts/register")
                .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated());

        verify(giftService).register(eq(5L), any());
    }

    @Test
    void register_ShouldReturnForbidden_WhenUserIsGuest() throws Exception {
        mockMvc.perform(post("/api/v1/admin/events/5/gifts/register")
                .with(user("guest").authorities(new SimpleGrantedAuthority("ROLE_GUEST")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden());

        verify(giftService, never()).register(anyLong(), any());
    }

    @Test
    void register_ShouldReturnForbidden_WhenUserIsAnonymous() throws Exception {
        mockMvc.perform(post("/api/v1/admin/events/5/gifts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden());

        verify(giftService, never()).register(anyLong(), any());
    }

    @Test
    void delete_ShouldAllowAdmin() throws Exception {
        doNothing().when(giftService).delete(5L, 1L);

        mockMvc.perform(delete("/api/v1/admin/events/{eventId}/gifts/{id}/delete", 5L, 1L)
                .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNoContent());

        verify(giftService).delete(5L, 1L);
    }

    @Test
    void delete_ShouldReturnForbidden_WhenUserIsGuest() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/events/{eventId}/gifts/{id}/delete", 5L, 1L)
                .with(user("guest").authorities(new SimpleGrantedAuthority("ROLE_GUEST"))))
                .andExpect(status().isForbidden());

        verify(giftService, never()).delete(5L, 1L);
    }

    @Test
    void delete_ShouldReturnForbidden_WhenUserIsAnonymous() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/events/{eventId}/gifts/{id}/delete", 5L, 1L))
                .andExpect(status().isForbidden());

        verify(giftService, never()).delete(5L, 1L);
    }

    @Test
    void delete_ShouldReturnNotFound_WhenGiftDoesNotExistForEvent() throws Exception {
        doThrow(new NotFoundException("Presente não encontrado para este evento"))
            .when(giftService).delete(5L, 99L);

        mockMvc.perform(delete("/api/v1/admin/events/{eventId}/gifts/{id}/delete", 5L, 99L)
            .with(user("admin").authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
            .andExpect(status().isNotFound());
    }
}
