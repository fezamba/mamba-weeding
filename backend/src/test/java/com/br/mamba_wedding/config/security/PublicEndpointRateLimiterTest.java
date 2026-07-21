package com.br.mamba_wedding.config.security;

import com.br.mamba_wedding.common.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicEndpointRateLimiterTest {

    private PublicEndpointRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new PublicEndpointRateLimiter();
    }

    @Test
    void assertAllowed_ShouldAllowRequestsUntilLimit() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertDoesNotThrow(() -> rateLimiter.assertAllowed(request, "auth-login", "guest1", 2, Duration.ofMinutes(1)));
        assertDoesNotThrow(() -> rateLimiter.assertAllowed(request, "auth-login", "guest1", 2, Duration.ofMinutes(1)));
    }

    @Test
    void assertAllowed_ShouldThrowWhenLimitIsExceeded() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        rateLimiter.assertAllowed(request, "auth-login", "guest1", 1, Duration.ofMinutes(1));

        assertThrows(TooManyRequestsException.class,
                () -> rateLimiter.assertAllowed(request, "auth-login", "guest1", 1, Duration.ofMinutes(1)));
    }

    @Test
    void assertAllowed_ShouldUseForwardedHeadersWhenPresent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.1.2.3, 172.16.0.1");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        assertDoesNotThrow(() -> rateLimiter.assertAllowed(request, "rsvp-lookup", "guest2", 1, Duration.ofMinutes(1)));
        assertThrows(TooManyRequestsException.class,
                () -> rateLimiter.assertAllowed(request, "rsvp-lookup", "guest2", 1, Duration.ofMinutes(1)));
    }
}
