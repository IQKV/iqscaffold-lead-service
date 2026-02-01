package com.iqscaffold.leadservice.tenancy;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service responsible for running Liquibase migrations in a multi-tenant environment.
 * 
 * <p>This service handles both system-wide migrations (public schema) and tenant-specific 
 * migrations (per-tenant schemas). System migrations are typically run once during 
 * application startup, while tenant migrations are run when new tenant schemas are created.
 * 
 * @author iqscaffold
 * @since 1.0
 */
@Service
public class TenantLiquibaseRunner {

  private final DataSource dataSource;
  private final String tenantChangeLog;
  private final String systemChangeLog;

  public TenantLiquibaseRunner(
      final DataSource dataSource,
      @Value("${iqscaffold.liquibase.tenantChangeLog:classpath:db/changelog/tenant/master.xml}") final String tenantChangeLog,
      @Value("${iqscaffold.liquibase.systemChangeLog:classpath:db/changelog/system/master.xml}") final String systemChangeLog) {
    this.dataSource = dataSource;
    this.tenantChangeLog = tenantChangeLog;
    this.systemChangeLog = systemChangeLog;
  }

  /**
   * Runs system-wide migrations against the public schema.
   * 
   * <p>System migrations typically include shared infrastructure like tenant management
   * tables, authorities, and other cross-tenant resources.
   * 
   * @throws Exception if migration fails
   */
  public void runSystemChangelog() throws Exception {
    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setDefaultSchema("public");
    liquibase.setLiquibaseSchema("public");
    liquibase.setChangeLog(systemChangeLog);
    liquibase.afterPropertiesSet();
  }

  /**
   * Runs tenant-specific migrations against a specific tenant schema.
   * 
   * <p>Tenant migrations include lead-specific tables, indexes, and data
   * that are isolated per tenant.
   * 
   * @param schema the tenant schema name to run migrations against
   * @throws Exception if migration fails
   */
  public void runTenantChangelog(String schema) throws Exception {
    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setDefaultSchema(schema);
    liquibase.setLiquibaseSchema(schema);
    liquibase.setChangeLog(tenantChangeLog);
    liquibase.afterPropertiesSet();
  }
}