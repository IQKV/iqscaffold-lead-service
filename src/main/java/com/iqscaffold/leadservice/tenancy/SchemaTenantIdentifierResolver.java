package com.iqscaffold.leadservice.tenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hibernate Tenant Identifier Resolver for schema-per-tenant multi-tenancy.
 *
 * <p>This resolver is called by Hibernate to determine which database schema to use
 * for the current request. It reads the tenant ID from {@link TenantContext} and
 * converts it to a schema name using {@link SchemaNameResolver}.
 *
 * <h3>Schema Resolution Flow:</h3>
 * <ol>
 *   <li>Get tenant ID from ThreadLocal via TenantContext</li>
 *   <li>Convert tenant ID to schema name (e.g., "default" → "tenant_default")</li>
 *   <li>Return schema name to Hibernate</li>
 *   <li>Hibernate sets PostgreSQL search_path to the schema</li>
 * </ol>
 *
 * <h3>Examples:</h3>
 * <pre>
 * TenantContext: "default"     → Schema: "tenant_default"
 * TenantContext: "acme-corp"   → Schema: "tenant_acme_corp"
 * TenantContext: null          → Schema: "public"
 * </pre>
 *
 * @see TenantContext
 * @see SchemaNameResolver
 */
public class SchemaTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

  private static final Logger logger = LoggerFactory.getLogger(SchemaTenantIdentifierResolver.class);

  private final SchemaNameResolver schemaNameResolver;

  /**
   * Constructor with schema name resolver.
   *
   * @param schemaNameResolver the schema name resolver
   */
  public SchemaTenantIdentifierResolver(final SchemaNameResolver schemaNameResolver) {
    this.schemaNameResolver = schemaNameResolver;
  }

  /**
   * Resolve the current tenant identifier (schema name) for Hibernate.
   *
   * <p>This method is called by Hibernate for every database operation to determine
   * which schema to use. It must be fast and thread-safe.
   *
   * @return the schema name (e.g., "tenant_default") or "public" if no tenant context
   */
  @Override
  public String resolveCurrentTenantIdentifier() {
    var tenantId = TenantContext.getCurrentTenantId();
    var schemaName = schemaNameResolver.toSchema(tenantId);
    
    logger.info("Hibernate resolving tenant: {} → schema: {}", tenantId, schemaName);
    
    return schemaName;
  }

  /**
   * Indicates whether existing sessions should be validated.
   *
   * @return false - we don't validate existing sessions
   */
  @Override
  public boolean validateExistingCurrentSessions() {
    return false;
  }
}
