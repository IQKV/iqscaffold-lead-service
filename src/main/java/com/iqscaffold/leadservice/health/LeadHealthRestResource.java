package com.iqscaffold.leadservice.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Public health check endpoint for lead service.
 * Used by frontend to monitor service availability and display degradation banners.
 * 
 * <p>This endpoint is intentionally public (no authentication required) to allow
 * health monitoring even when authentication services are degraded.</p>
 */
@RestController
@RequestMapping("/api/v1/leads")
@Tag(name = "Lead Health", description = "Public health check endpoint for lead service monitoring")
public class LeadHealthRestRespurce {

  @Operation(
      summary = "Lead service health check",
      description = "Returns health status of the lead management service")
  @ApiResponse(responseCode = "200", description = "Lead service is healthy")
  @GetMapping("/health")
  public ResponseEntity<Map<String, Object>> health() {
    return ResponseEntity.ok(Map.of(
        "status", "UP",
        "service", "leads",
        "timestamp", Instant.now().toString()
    ));
  }
}
