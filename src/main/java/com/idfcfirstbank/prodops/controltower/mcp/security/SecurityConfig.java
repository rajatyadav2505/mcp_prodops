package com.idfcfirstbank.prodops.controltower.mcp.security;

import com.idfcfirstbank.prodops.controltower.mcp.config.ProdOpsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, ProdOpsProperties properties)
      throws Exception {
    http.csrf(csrf -> csrf.disable());
    http.sessionManagement(
        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.authorizeHttpRequests(
        authz -> {
          authz.requestMatchers("/actuator/health/**", "/actuator/info").permitAll();
          if (properties.security().jwtEnabled()) {
            authz
                .requestMatchers("/actuator/prometheus")
                .hasAuthority("SCOPE_" + properties.security().requiredScope());
            authz.anyRequest().hasAuthority("SCOPE_" + properties.security().requiredScope());
          } else {
            authz.anyRequest().permitAll();
          }
        });
    if (properties.security().jwtEnabled()) {
      http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    }
    return http.build();
  }

  @Bean
  @ConditionalOnProperty(
      prefix = "prodops.controltower.security",
      name = "jwt-enabled",
      havingValue = "true")
  JwtDecoder jwtDecoder(ProdOpsProperties properties) {
    NimbusJwtDecoder decoder =
        properties.security().jwkSetUri() != null && !properties.security().jwkSetUri().isBlank()
            ? NimbusJwtDecoder.withJwkSetUri(properties.security().jwkSetUri()).build()
            : NimbusJwtDecoder.withIssuerLocation(properties.security().issuerUri()).build();

    OAuth2TokenValidator<Jwt> baseValidator =
        properties.security().issuerUri() != null && !properties.security().issuerUri().isBlank()
            ? JwtValidators.createDefaultWithIssuer(properties.security().issuerUri())
            : JwtValidators.createDefault();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            baseValidator, new JwtAudienceValidator(properties.security().audience())));
    return decoder;
  }
}
