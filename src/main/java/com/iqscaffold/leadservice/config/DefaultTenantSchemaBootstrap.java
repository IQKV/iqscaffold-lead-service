package com.iqscaffold.leadservice.config;

import com.iqscaffold.leadservice.tenancy.TenantLiquibaseRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Bootstrap component that ensures tenant_default schema exists and has migrations applied.
 * 
 * <p>This component runs after SystemLiquibaseInitializer to ensure that any microservice
 * can bootstrap the default tenant schema without requiring the user-service to run first.
 * 
 * <p>Execution order:
 * <ol>
 *   <li>SystemLiquibaseInitializer runs system migrations (public schema)</li>
 *   <li>DefaultTenantSchemaBootstrap creates and migrates tenant_default (this class)</li>
 *   <li>Application startup completes</li>
 * </ol>
 * 
 * <p>Configuration:
 * <ul>
 *   <li>Enable/disable: {@code iqscaffold.bootstrap.default-tenant-schema.enabled}</li>
 *   <li>Schema name: {@code iqscaffold.tenancy.schema.prefix} + {@code iqscaffold.bootstrap.default-tenant-schema.tenant-id}</li>
 * </ul>
 */
@Component
@Order(50) // Run after SystemLiquibaseInitializer (MIN_VALUE) but before application logic
@ConditionalOnProperty(
    name = "iqscaffold.bootstrap.default-tenant-schema.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class DefaultTenantSchemaBootstrap implements InitializingBean {

  private static final Logger logger = LoggerFactory.getLogger(DefaultTenantSchemaBootstrap.class);

  private final TenantLiquibaseRunner liquibaseRunner;
  private final JdbcTemplate jdbcTemplate;

  @Value("${iqscaffold.bootstrap.default-tenant-schema.tenant-id:default}")
  private String defaultTenantId;

  @Value("${iqscaffold.tenancy.schema.prefix:tenant_}")
  private String schemaPrefix;

  public DefaultTenantSchemaBootstrap(
      TenantLiquibaseRunner liquibaseRunner,
      JdbcTemplate jdbcTemplate) {
    this.liquibaseRunner = liquibaseRunner;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void afterPropertiesSet() {
    String defaultSchema = schemaPrefix + defaultTenantId;
    
    logger.info("Checking for default tenant schema: {}", defaultSchema);
    
    try {
      // Check if schema exists
      boolean schemaExists = checkSchemaExists(defaultSchema);
      
      if (!schemaExists) {
        logger.info("Default tenant schema '{}' does not exist. Creating...", defaultSchema);
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + defaultSchema);
      }
      
      // Always run migrations to ensure schema is up-to-date
      logger.info("Running tenant migrations for default schema: {}", defaultSchema);
      liquibaseRunner.runTenantChangelog(defaultSchema);
      
      logger.info("Default tenant schema '{}' is ready", defaultSchema);
      
    } catch (Exception e) {
      logger.error("Failed to bootstrap default tenant schema: {}", defaultSchema, e);
      throw new IllegalStateException("Default tenant schema bootstrap failed", e);
    }
  }

  private boolean checkSchemaExists(String schemaName) {
    try {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?",
          Integer.class,
          schemaName
      );
      return count != null && count > 0;
    } catch (Exception e) {
      logger.warn("Could not check if schema exists: {}", schemaName, e);
      return false;
    }
  }
}
