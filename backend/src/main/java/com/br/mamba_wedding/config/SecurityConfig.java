package com.br.mamba_wedding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

import com.br.mamba_wedding.config.security.SecurityFilter;
import com.br.mamba_wedding.common.api.ApiErrorWriter;
import com.br.mamba_wedding.common.api.ApiPaths;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final ApiErrorWriter apiErrorWriter;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            SecurityFilter securityFilter,
            ApiErrorWriter apiErrorWriter,
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        this.securityFilter = securityFilter;
        this.apiErrorWriter = apiErrorWriter;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        if (this.allowedOrigins.isEmpty() || this.allowedOrigins.contains("*")) {
            throw new IllegalArgumentException(
                    "app.cors.allowed-origins deve informar ao menos uma origem explícita e não aceita '*'.");
        }
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())

            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                    apiErrorWriter.write(request, response, org.springframework.http.HttpStatus.UNAUTHORIZED,
                            "Autenticação necessária."))
                .accessDeniedHandler((request, response, exception) ->
                    apiErrorWriter.write(request, response, org.springframework.http.HttpStatus.FORBIDDEN,
                            "Acesso negado.")))

            .authorizeHttpRequests(auth -> auth
                .requestMatchers(ApiPaths.V1 + "/auth/login").permitAll()
                .requestMatchers(ApiPaths.V1 + "/admin/auth/google").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, ApiPaths.V1 + "/messages").permitAll()

                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()

                .requestMatchers(ApiPaths.V1 + "/admin/**").hasAuthority("ROLE_ADMIN")

                .requestMatchers(ApiPaths.V1 + "/rsvp/**").hasAuthority("ROLE_GUEST")
                .requestMatchers(org.springframework.http.HttpMethod.POST, ApiPaths.V1 + "/messages")
                    .hasAuthority("ROLE_GUEST")
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                    ApiPaths.V1 + "/gifts/*/reserve",
                    ApiPaths.V1 + "/gifts/*/buy")
                    .hasAuthority("ROLE_GUEST")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, ApiPaths.V1 + "/gifts/*/reserve")
                    .hasAuthority("ROLE_GUEST")

                .anyRequest().authenticated()
            )

            .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()
        ));
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.ACCEPT
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
