package com.iqscaffold.leadservice.tenancy;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

/**
 * Hibernate Tenant Identifier Resolver for schema-per-tenant multi-tenancy.
 *
 * <p>Resolves tenant ID to schema name (e.g., "default" → "tenant_default").
 *
 * @see SchemaNameResolver
 * @see TenantContext
 */
@Component
public class SchemaTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

  private static final Logger logger = LoggerFactory.getLogger(SchemaTenantIdentifierResolver.class);

  private final SchemaNameResolver schemaNameResolver;

  public SchemaTenantIdentifierResolver(final SchemaNameResolver schemaNameResolver) {
    this.schemaNameResolver = schemaNameResolver;
  }

  @Override
  public String resolveCurrentTenantIdentifier() {
    String tenantId = TenantContext.getCurrentTenantId();
    String schemaName = schemaNameResolver.toSchema(tenantId);
    
    logger.info("Hibernate resolving tenant: {} → schema: {}", tenantId, schemaName);
    
    return schemaName;
  }

  @Override
  public boolean validateExistingCurrentSessions() {
    return true;
  }

  @Override
  public void customize(final Map<String, Object> hibernateProperties) {
    hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
  }
}
