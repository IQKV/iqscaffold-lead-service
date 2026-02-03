package com.iqscaffold.leadservice.tenancy;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service responsible for running Liquibase migrations in a multi-tenant environment.
 * 
 * <p>This service handles both system-wide migrations (public schema) and tenant-specific 
 * migrations (per-tenant schemas). System migrations are typically run once during 
 * application startup, while tenant migrations are run when new tenant schemas are created.
 * 
 * <p>Context filtering is supported to control which changesets run in different environments.
 * Use the {@code iqscaffold.liquibase.contexts} property to specify contexts.
 * 
 * @author iqscaffold
 * @since 1.0
 */
@Service
public class TenantLiquibaseRunner {

  private static final Logger logger = LoggerFactory.getLogger(TenantLiquibaseRunner.class);

  private final DataSource dataSource;
  private final String tenantChangeLog;
  private final String systemChangeLog;
  private final String contexts;

  public TenantLiquibaseRunner(
      final DataSource dataSource,
      @Value("${iqscaffold.liquibase.tenantChangeLog:classpath:db/changelog/tenant/master.xml}") final String tenantChangeLog,
      @Value("${iqscaffold.liquibase.systemChangeLog:classpath:db/changelog/system/master.xml}") final String systemChangeLog,
      @Value("${iqscaffold.liquibase.contexts:}") final String contexts) {
    this.dataSource = dataSource;
    this.tenantChangeLog = tenantChangeLog;
    this.systemChangeLog = systemChangeLog;
    this.contexts = contexts;
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
    logger.info("Running system changelog with contexts: {}", contexts);
    
    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setDefaultSchema("public");
    liquibase.setLiquibaseSchema("public");
    liquibase.setChangeLog(systemChangeLog);
    
    if (StringUtils.hasText(contexts)) {
      liquibase.setContexts(contexts);
    }
    
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
    logger.info("Running tenant changelog for schema '{}' with contexts: {}", schema, contexts);
    
    var liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setDefaultSchema(schema);
    liquibase.setLiquibaseSchema(schema);
    liquibase.setChangeLog(tenantChangeLog);
    
    if (StringUtils.hasText(contexts)) {
      liquibase.setContexts(contexts);
    }
    
    liquibase.afterPropertiesSet();
  }
}