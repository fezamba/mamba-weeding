package com.br.mamba_wedding.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration(proxyBeanMethods = false)
@Profile("!test")
@ConditionalOnProperty(
        name = "app.persistence.mongo-repositories.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableMongoRepositories(basePackages = "com.br.mamba_wedding.messages.infrastructure")
public class MongoRepositoryConfig {
}
