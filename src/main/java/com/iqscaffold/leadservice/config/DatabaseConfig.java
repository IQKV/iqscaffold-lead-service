package com.iqscaffold.leadservice.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Database configuration for Lead Service. Configures JPA repositories, entity scanning, and transaction management.
 */
@Configuration
@EnableJpaRepositories(
    basePackages = {
        "com.iqscaffold.leadservice"
    },
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
@EntityScan(basePackages = {
    "com.iqscaffold.leadservice"
})
@EnableTransactionManagement
public class DatabaseConfig {
  // Entities and repositories are organized by domain modules
}
