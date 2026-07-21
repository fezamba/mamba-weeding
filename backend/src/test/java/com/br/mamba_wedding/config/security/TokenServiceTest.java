package com.br.mamba_wedding.config.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "secret", "test-secret");
        ReflectionTestUtils.setField(tokenService, "issuer", "mamba-wedding");
        ReflectionTestUtils.setField(tokenService, "expirationHours", 2);
    }

    @Test
    void generateAndValidate_ShouldReturnDecodedTokenWithSubjectAndRole() {
        String token = tokenService.generateToken("ABC1234", "ROLE_GUEST");

        DecodedJWT decoded = tokenService.validateAndDecodeToken(token);

        assertNotNull(decoded);
        assertEquals("ABC1234", decoded.getSubject());
        assertEquals("ROLE_GUEST", decoded.getClaim("role").asString());
    }

    @Test
    void validateAndDecodeToken_ShouldReturnNullForInvalidToken() {
        DecodedJWT decoded = tokenService.validateAndDecodeToken("not-a-jwt");

        assertNull(decoded);
    }

    @Test
    void validateAndDecodeToken_ShouldReturnNullWhenIssuerDoesNotMatch() {
        String token = tokenService.generateToken("admin@example.com", "ROLE_ADMIN");

        ReflectionTestUtils.setField(tokenService, "issuer", "another-issuer");

        DecodedJWT decoded = tokenService.validateAndDecodeToken(token);

        assertNull(decoded);
    }
}
