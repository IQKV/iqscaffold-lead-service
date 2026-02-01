package com.iqscaffold.leadservice.tenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Component responsible for initializing system-wide Liquibase migrations on application startup.
 * 
 * <p>This component listens for the ApplicationReadyEvent and triggers the execution of
 * system-wide database migrations. System migrations are run against the public schema
 * and typically include shared infrastructure like tenant management tables.
 * 
 * @author iqscaffold
 * @since 1.0
 */
@Component
public class SystemLiquibaseInitializer {

  private final TenantLiquibaseRunner runner;
  private static final Logger logger = LoggerFactory.getLogger(SystemLiquibaseInitializer.class);

  public SystemLiquibaseInitializer(final TenantLiquibaseRunner runner) {
    this.runner = runner;
  }

  /**
   * Executes system-wide Liquibase migrations when the application is ready.
   * 
   * <p>This method is triggered by the ApplicationReadyEvent, ensuring that
   * system migrations are run after the application context is fully initialized.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    logger.info("Starting system Liquibase migrations for lead service");
    try {
      runner.runSystemChangelog();
      logger.info("Successfully completed system Liquibase migrations");
    } catch (final Exception e) {
      logger.error("Failed to run system Liquibase changelog", e);
      throw new RuntimeException("System migration failure", e);
    }
  }
}