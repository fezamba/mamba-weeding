package com.br.mamba_wedding.admin.api;

import com.br.mamba_wedding.common.exception.UnauthorizedException;
import com.br.mamba_wedding.config.security.TokenService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final TokenService tokenService;
    private final GoogleIdTokenVerifier verifier;
    private final List<String> authorizedEmails;

    public AdminAuthController(
            TokenService tokenService,
            @Value("${api.security.google.client-id}") String clientId,
            @Value("${api.security.admin.emails}") String adminEmails
    ) {
        this.tokenService = tokenService;
        this.authorizedEmails = Arrays.stream(adminEmails.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toList());
        
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public record GoogleLoginRequest(@NotBlank(message = "Google token é obrigatório") String googleToken) {}
    public record LoginResponse(String token) {}

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> authenticateGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        GoogleIdToken idToken = verifyGoogleToken(request.googleToken().trim());
        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail() == null ? "" : payload.getEmail().trim();

        if (!authorizedEmails.contains(email)) {
            throw new AccessDeniedException("Acesso negado.");
        }

        String internalToken = tokenService.generateToken(email, "ROLE_ADMIN");
        return ResponseEntity.ok(new LoginResponse(internalToken));
    }

    private GoogleIdToken verifyGoogleToken(String googleToken) {
        try {
            GoogleIdToken idToken = verifier.verify(googleToken);
            if (idToken == null) {
                throw new UnauthorizedException("Token Google inválido ou expirado.");
            }
            return idToken;
        } catch (Exception ex) {
            throw new UnauthorizedException("Token Google inválido ou expirado.");
        }
    }
}
