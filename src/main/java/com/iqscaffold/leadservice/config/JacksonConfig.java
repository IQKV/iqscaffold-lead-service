package com.iqscaffold.leadservice.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson configuration for JSON serialization/deserialization.
 * 
 * <p>This configuration ensures:
 * <ul>
 *   <li>No Java type information in JSON output (clean RFC 9457 ProblemDetail)</li>
 *   <li>Proper date/time handling with JavaTimeModule</li>
 *   <li>Consistent JSON formatting across the application</li>
 *   <li>Null value handling configuration</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

  /**
   * Configure the primary ObjectMapper bean for the application.
   * 
   * <p>Key configurations:
   * <ul>
   *   <li><strong>No Default Typing</strong> - Prevents Java type information in JSON</li>
   *   <li><strong>Date/Time Handling</strong> - ISO-8601 format (not timestamps)</li>
   *   <li><strong>Non-null Inclusion</strong> - Excludes null values from JSON output</li>
   *   <li><strong>Indented Output</strong> - Pretty-printed JSON for readability</li>
   * </ul>
   * 
   * <p>Note: JavaTimeModule is automatically registered by Spring Boot's auto-configuration.
   * 
   * @param builder Jackson2ObjectMapperBuilder for configuration
   * @return Configured ObjectMapper instance
   */
  @Bean
  @Primary
  public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
    // Configure the builder before building the ObjectMapper
    // This ensures proper integration with Spring Boot's auto-configuration
    return builder
        // Disable default typing to prevent Java type information in JSON
        .defaultTyping(null)
        // Configure serialization features
        .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .featuresToEnable(SerializationFeature.INDENT_OUTPUT)
        // Exclude null values from JSON output
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        // Build the ObjectMapper with all configurations applied
        .build();
  }
}
