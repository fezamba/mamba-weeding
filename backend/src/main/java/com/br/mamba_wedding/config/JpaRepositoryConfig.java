package com.br.mamba_wedding.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@Profile("!test")
@ConditionalOnProperty(
        name = "app.persistence.jpa-repositories.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableJpaRepositories(basePackages = {
        "com.br.mamba_wedding.events.infrastructure",
        "com.br.mamba_wedding.gifts.infrastructure",
        "com.br.mamba_wedding.guests.infrastructure"
})
public class JpaRepositoryConfig {
}
