package com.iqscaffold.leadservice.tenancy;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Schema Name Resolver for multi-tenant database schema management.
 *
 * <p>Converts tenant IDs to valid PostgreSQL schema names following these rules:
 * <ul>
 *   <li>Adds configurable prefix (default: "tenant_")</li>
 *   <li>Converts to lowercase</li>
 *   <li>Replaces hyphens with underscores</li>
 *   <li>Removes illegal characters</li>
 *   <li>Truncates to 48 characters max</li>
 *   <li>Caches results for performance</li>
 * </ul>
 *
 * <h3>Examples:</h3>
 * <pre>
 * "default"     → "tenant_default"
 * "acme-corp"   → "tenant_acme_corp"
 * "ACME-Corp"   → "tenant_acme_corp"
 * "test@123"    → "tenant_test123"
 * null/blank    → "public"
 * </pre>
 *
 * @see SchemaTenantIdentifierResolver
 */
@Component
public class SchemaNameResolver {

  private final String prefix;
  private final String defaultTenantId;
  private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
  private static final Pattern ILLEGAL = Pattern.compile("[^a-z0-9_]");

  public SchemaNameResolver(
      @Value("${iqscaffold.tenancy.schema.prefix:tenant_}") final String prefix,
      @Value("${iqscaffold.bootstrap.default-tenant-schema.tenant-id:default}") final String defaultTenantId) {
    this.prefix = prefix;
    this.defaultTenantId = defaultTenantId;
  }

  public String toSchema(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      // Use default tenant instead of public schema
      // Special case: if default tenant is "PUBLIC", return it as-is without prefix
      if ("PUBLIC".equalsIgnoreCase(defaultTenantId)) {
        return "PUBLIC";
      }
      return cache.computeIfAbsent(defaultTenantId, this::normalize);
    }
    // Special case: if tenant is explicitly "PUBLIC", return it as-is
    if ("PUBLIC".equalsIgnoreCase(tenantId)) {
      return "PUBLIC";
    }
    return cache.computeIfAbsent(tenantId, this::normalize);
  }

  private String normalize(String tenantId) {
    var lower = tenantId.toLowerCase(Locale.ROOT).trim();
    var replaced = lower.replace('-', '_');
    var cleaned = ILLEGAL.matcher(replaced).replaceAll("");
    if (cleaned.length() > 48) {
      cleaned = cleaned.substring(0, 48);
    }
    return prefix + cleaned;
  }

  public String getPrefix() {
    return prefix;
  }

  public void clearCache() {
    cache.clear();
  }
}
