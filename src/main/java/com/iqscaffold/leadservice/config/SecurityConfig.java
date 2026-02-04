package com.iqscaffold.leadservice.config;

import com.iqscaffold.leadservice.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final IqScaffoldProperties iqScaffoldProperties;

  public SecurityConfig(final JwtAuthenticationFilter jwtAuthenticationFilter,
      final IqScaffoldProperties iqScaffoldProperties) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.iqScaffoldProperties = iqScaffoldProperties;
  }

  @Bean
  public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
    http
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(authz -> authz
            .requestMatchers("/actuator/**").permitAll()
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
