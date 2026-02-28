package com.iqscaffold.leadservice.config;

import com.iqscaffold.leadservice.security.JwtAuthenticationFilter;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@org.springframework.context.annotation.Profile("!test")
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final IqScaffoldProperties iqScaffoldProperties;

  public SecurityConfig(final JwtAuthenticationFilter jwtAuthenticationFilter,
      final IqScaffoldProperties iqScaffoldProperties) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.iqScaffoldProperties = iqScaffoldProperties;
  }

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  public SecurityFilterChain actuatorSecurityFilterChain(final HttpSecurity http) throws Exception {
    return http
        .securityMatcher(EndpointRequest.toAnyEndpoint())
        .authorizeHttpRequests(authz -> authz
            .requestMatchers(EndpointRequest.to("health", "info", "prometheus")).permitAll()
            .anyRequest().authenticated())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .build();
  }

  @Bean
  public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
    http
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .decoder(jwtDecoder())
                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
        .addFilterAfter(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    var jwtProps = iqScaffoldProperties.lead().security().jwt();

    if (isSymmetricConfigured()) {
      var algorithm = jwtProps.algorithm() != null ? jwtProps.algorithm() : "HS256";
      javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(
          jwtProps.secretKey().getBytes(), "Hmac" + algorithm.substring(2));
      return NimbusJwtDecoder.withSecretKey(key).build();
    }

    var jwkSetUri = jwtProps.jwkSetUri();
    if (jwkSetUri == null || jwkSetUri.isBlank()) {
      throw new IllegalStateException("Neither secret-key nor jwk-set-uri is configured for JWT validation");
    }
    return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
  }

  private boolean isSymmetricConfigured() {
    var secretKey = iqScaffoldProperties.lead().security().jwt().secretKey();
    return secretKey != null && !secretKey.isBlank() && !"change-me-in-production".equals(secretKey);
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthorityPrefix("");
    authoritiesConverter.setAuthoritiesClaimName("roles");

    JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
    authenticationConverter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return authenticationConverter;
  }
}
