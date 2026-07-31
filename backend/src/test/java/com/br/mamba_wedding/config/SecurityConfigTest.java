package com.br.mamba_wedding.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecurityConfigTest {

    @Test
    void corsConfiguration_ShouldAcceptOnlyConfiguredOrigins() {
        SecurityConfig securityConfig = new SecurityConfig(
                null,
                null,
                "http://localhost:5173, https://wedding.example.com");

        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/api/v1/events/1/gifts"));

        assertEquals(
                java.util.List.of("http://localhost:5173", "https://wedding.example.com"),
                configuration.getAllowedOrigins());
        assertEquals(true, configuration.getAllowCredentials());
    }

    @Test
    void constructor_ShouldRejectWildcardOrigin() {
        assertThrows(IllegalArgumentException.class, () -> new SecurityConfig(null, null, "*"));
    }
}
