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

import java.util.List;

/**
 * Bootstrap component that ensures tenant schemas exist and have migrations applied.
 * 
 * <p>This component handles the case where tenant schemas are created externally
 * (e.g., by Helm init scripts) or already exist. It checks each configured tenant 
 * schema and applies all pending migrations on application startup.
 * 
 * <p>This is particularly useful in Kubernetes deployments where:
 * <ul>
 *   <li>Helm charts create empty tenant schemas (tenant_default, tenant_demo, tenant_acme)</li>
 *   <li>The application needs to populate those schemas with tables</li>
 *   <li>Multiple microservices need to provision their own tables in shared tenant schemas</li>
 *   <li>New migrations need to be applied to existing tenant schemas</li>
 * </ul>
 * 
 * <p>Execution order:
 * <ol>
 *   <li>SystemLiquibaseInitializer runs system migrations (public schema)</li>
 *   <li>DefaultTenantSchemaBootstrap creates and migrates tenant schemas (this class)</li>
 *   <li>Application startup completes</li>
 * </ol>
 * 
 * <p>Configuration:
 * <ul>
 *   <li>Enable/disable: {@code iqscaffold.bootstrap.default-tenant-schema.enabled}</li>
 *   <li>Tenant IDs: {@code iqscaffold.bootstrap.default-tenant-schema.tenant-ids} (comma-separated)</li>
 *   <li>Schema prefix: {@code iqscaffold.tenancy.schema.prefix}</li>
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

  @Value("${iqscaffold.bootstrap.default-tenant-schema.tenant-ids:default}")
  private String tenantIds;

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
    logger.info("Checking tenant schemas for missing migrations...");
    
    List<String> tenantIdList = List.of(tenantIds.split(","));
    logger.info("Configured tenant IDs: {}", tenantIdList);
    
    for (String tenantId : tenantIdList) {
      String trimmedTenantId = tenantId.trim();
      if (trimmedTenantId.isEmpty()) {
        continue;
      }
      
      try {
        checkAndMigrateTenantSchema(trimmedTenantId);
      } catch (Exception e) {
        logger.error("Failed to check/migrate schema for tenant: {}", trimmedTenantId, e);
        // Continue with other tenants instead of failing completely
      }
    }
    
    logger.info("Tenant schema migration check completed.");
  }

  private void checkAndMigrateTenantSchema(String tenantId) {
    String schema = schemaPrefix + tenantId;
    
    logger.debug("Checking schema: {} for tenant: {}", schema, tenantId);

    // Check if schema exists
    boolean schemaExists = checkSchemaExists(schema);
    
    if (!schemaExists) {
      logger.info("Schema {} does not exist for tenant: {}. Creating and migrating...", schema, tenantId);
      createSchemaAndMigrate(schema, tenantId);
      return;
    }

    // Schema exists, always run migrations to ensure all changesets are applied
    logger.info("Running migrations for schema: {} (tenant: {})", schema, tenantId);
    runMigrations(schema, tenantId);
  }

  private boolean checkSchemaExists(String schema) {
    try {
      String sql = "SELECT EXISTS(SELECT 1 FROM information_schema.schemata WHERE schema_name = ?)";
      Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, schema);
      return Boolean.TRUE.equals(exists);
    } catch (Exception e) {
      logger.warn("Failed to check if schema exists: {}", schema, e);
      return false;
    }
  }


  private void createSchemaAndMigrate(String schema, String tenantId) {
    try {
      logger.info("Creating schema: {}", schema);
      jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
      logger.info("Schema created: {}", schema);
      
      runMigrations(schema, tenantId);
    } catch (Exception e) {
      logger.error("Failed to create schema and run migrations for tenant: {}", tenantId, e);
      throw new IllegalStateException("Failed to create schema: " + schema, e);
    }
  }

  private void runMigrations(String schema, String tenantId) {
    try {
      logger.info("Running Liquibase migrations for schema: {} (tenant: {})", schema, tenantId);
      liquibaseRunner.runTenantChangelog(schema);
      logger.info("Successfully applied migrations to schema: {} (tenant: {})", schema, tenantId);
    } catch (Exception e) {
      logger.error("Failed to run migrations for schema: {} (tenant: {})", schema, tenantId, e);
      throw new IllegalStateException("Failed to run migrations for schema: " + schema, e);
    }
  }
}
