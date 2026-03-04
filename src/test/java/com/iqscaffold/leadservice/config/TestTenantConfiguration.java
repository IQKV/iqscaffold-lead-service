package com.iqscaffold.leadservice.config;

import javax.sql.DataSource;

import com.iqscaffold.leadservice.tenancy.SchemaNameResolver;
import com.iqscaffold.leadservice.tenancy.SchemaPerTenantConnectionProvider;
import com.iqscaffold.leadservice.tenancy.SchemaTenantIdentifierResolver;
import com.iqscaffold.leadservice.tenancy.TenantLiquibaseRunner;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Test configuration for multi-tenancy support in tests.
 * Provides beans needed for tenant-aware data access in test environment.
 */
@TestConfiguration
@Profile("test")
public class TestTenantConfiguration {

  @Bean
  public SchemaNameResolver schemaNameResolver() {
    return new SchemaNameResolver("tenant_", "default");
  }

  @Bean
  public CurrentTenantIdentifierResolver currentTenantIdentifierResolver(
      final SchemaNameResolver schemaNameResolver) {
    return new SchemaTenantIdentifierResolver(schemaNameResolver);
  }

  @Bean
  public MultiTenantConnectionProvider multiTenantConnectionProvider(DataSource dataSource) {
    return new SchemaPerTenantConnectionProvider(dataSource);
  }

  @Bean
  public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(
      final CurrentTenantIdentifierResolver tenantResolver,
      final MultiTenantConnectionProvider connectionProvider) {

    return hibernateProperties -> {
      hibernateProperties.put("hibernate.multiTenancy", "SCHEMA");
      hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
      hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantResolver);
      hibernateProperties.put(AvailableSettings.USE_SQL_COMMENTS, true);
    };
  }

  @Bean
  public TenantLiquibaseRunner tenantLiquibaseRunner() {
    return org.mockito.Mockito.mock(TenantLiquibaseRunner.class);
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }
}
