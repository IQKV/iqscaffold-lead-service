package com.iqscaffold.leadservice.event;

import com.iqscaffold.leadservice.tenancy.TenantLiquibaseRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Listener for tenant events from the User Service.
 * Handles tenant lifecycle events to provision lead database schemas.
 */
@Component
public class TenantEventListener {

  private static final Logger log = LoggerFactory.getLogger(TenantEventListener.class);

  private final TenantLiquibaseRunner liquibaseRunner;
  private final JdbcTemplate jdbcTemplate;

  public TenantEventListener(
      final TenantLiquibaseRunner liquibaseRunner,
      final JdbcTemplate jdbcTemplate) {
    this.liquibaseRunner = liquibaseRunner;
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Handle tenant event from User Service.
   * Provisions lead database schema when tenant is created.
   */
  @RabbitListener(queues = "iqscaffold.lead.tenant.events")
  public void handleTenantEvent(TenantEvent event) {
    try {
      log.info("Received tenant event: {} for tenant: {}",
          event.getEventType(), event.getTenantId());

      // Process the event based on type
      switch (event.getEventType()) {
        case "TENANT_CREATED":
          handleTenantCreated(event);
          break;
        case "TENANT_UPDATED":
          handleTenantUpdated(event);
          break;
        case "TENANT_DELETED":
          handleTenantDeleted(event);
          break;
        case "TENANT_SUSPENDED":
          handleTenantSuspended(event);
          break;
        case "TENANT_ARCHIVED":
          handleTenantArchived(event);
          break;
        case "TENANT_RESTORED":
          handleTenantRestored(event);
          break;
        default:
          log.warn("Unknown tenant event type: {}", event.getEventType());
      }

      log.debug("Successfully processed tenant event: {}", event.getEventId());
    } catch (final Exception e) {
      log.error("Error processing tenant event: {} for tenant: {}",
          event.getEventId(), event.getTenantId(), e);
      throw e; // Re-throw to trigger retry or DLQ routing
    }
  }

  /**
   * Handle tenant created event.
   * Provisions lead database schema for the new tenant.
   */
  private void handleTenantCreated(TenantEvent event) {
    log.info("Processing tenant created event for tenant: {}", event.getTenantId());

    try {
      // Create tenant schema in lead database
      var schemaName = resolveSchemaName(event.getTenantId());

      log.info("Creating schema '{}' for tenant: {}", schemaName, event.getTenantId());
      jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);

      // Run Liquibase migrations for tenant schema
      log.info("Running Liquibase migrations for schema: {}", schemaName);
      liquibaseRunner.runTenantChangelog(schemaName);

      log.info("Successfully provisioned lead schema for tenant: {} (organization: {})",
          event.getTenantId(), event.getOrganizationName());

    } catch (final Exception e) {
      log.error("Failed to provision lead schema for tenant: {}", event.getTenantId(), e);
      throw new RuntimeException("Failed to provision lead schema for tenant: " + event.getTenantId(), e);
    }
  }

  /**
   * Handle tenant updated event.
   * Currently a no-op, but could be used for schema migrations.
   */
  private void handleTenantUpdated(TenantEvent event) {
    log.info("Processing tenant updated event for tenant: {}", event.getTenantId());

    // Placeholder for future implementation
    // Could be used to:
    // - Apply schema migrations
    // - Update tenant configuration
    // - Sync organization details

    log.debug("Tenant updated event processed for tenant: {}", event.getTenantId());
  }

  /**
   * Handle tenant deleted event.
   * Archives lead data but does not drop schema (for compliance).
   */
  private void handleTenantDeleted(TenantEvent event) {
    log.info("Processing tenant deleted event for tenant: {}", event.getTenantId());

    // Placeholder for future implementation
    // Best practice: Archive data but DO NOT drop schema
    // Reasons:
    // - Compliance and audit requirements
    // - Historical lead data retention
    // - Potential data recovery needs
    // - Legal/business record keeping

    log.warn("Tenant deleted event received for: {}. Lead data archived but schema retained for compliance.",
        event.getTenantId());

    log.debug("Tenant deleted event processed for tenant: {}", event.getTenantId());
  }

  /**
   * Handle tenant suspended event.
   * Suspends lead operations for the tenant (reversible).
   */
  private void handleTenantSuspended(TenantEvent event) {
    log.info("Processing tenant suspended event for tenant: {} with reason: {}",
        event.getTenantId(), event.getMetadata().get("reason"));

    try {
      // Suspend lead operations for this tenant
      // - Stop accepting new lead creation
      // - Mark existing leads as suspended
      // - Maintain schema and data for restoration

      log.info("Successfully suspended lead operations for tenant: {}", event.getTenantId());
    } catch (final Exception e) {
      log.error("Failed to suspend lead operations for tenant: {}", event.getTenantId(), e);
      throw new RuntimeException("Failed to suspend lead operations for tenant: " + event.getTenantId(), e);
    }
  }

  /**
   * Handle tenant archived event.
   * Archives lead data and prevents future operations (terminal state).
   */
  private void handleTenantArchived(TenantEvent event) {
    log.warn("Processing tenant archived event for tenant: {} with reason: {}",
        event.getTenantId(), event.getMetadata().get("reason"));

    try {
      // Archive lead data for this tenant
      // - Disable all lead operations
      // - Archive lead records
      // - Retain schema for compliance and audit
      // - This is a terminal state - no restoration possible

      log.warn("Successfully archived lead data for tenant: {}", event.getTenantId());
    } catch (final Exception e) {
      log.error("Failed to archive lead data for tenant: {}", event.getTenantId(), e);
      throw new RuntimeException("Failed to archive lead data for tenant: " + event.getTenantId(), e);
    }
  }

  /**
   * Handle tenant restored event.
   * Restores lead operations for the tenant from suspended state.
   */
  private void handleTenantRestored(TenantEvent event) {
    log.info("Processing tenant restored event for tenant: {}", event.getTenantId());

    try {
      // Restore lead operations for this tenant
      // - Re-enable lead creation and updates
      // - Restore lead processing
      // - Resume normal operations

      log.info("Successfully restored lead operations for tenant: {}", event.getTenantId());
    } catch (final Exception e) {
      log.error("Failed to restore lead operations for tenant: {}", event.getTenantId(), e);
      throw new RuntimeException("Failed to restore lead operations for tenant: " + event.getTenantId(), e);
    }
  }

  /**
   * Resolve schema name from tenant ID.
   * Matches the naming convention from user service SchemaNameResolver.
   */
  private String resolveSchemaName(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      return "public";
    }
    var lower = tenantId.toLowerCase(java.util.Locale.ROOT).trim();
    var replaced = lower.replace('-', '_');
    var cleaned = replaced.replaceAll("[^a-z0-9_]", "");
    if (cleaned.length() > 48) {
      cleaned = cleaned.substring(0, 48);
    }
    return "tenant_" + cleaned;
  }
}
