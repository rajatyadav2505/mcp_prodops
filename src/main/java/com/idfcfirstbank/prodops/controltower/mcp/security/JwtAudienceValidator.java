package com.idfcfirstbank.prodops.controltower.mcp.security;

import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

  private final String audience;

  JwtAudienceValidator(String audience) {
    this.audience = audience;
  }

  @Override
  public OAuth2TokenValidatorResult validate(Jwt token) {
    List<String> audienceValues = token.getAudience();
    if (audienceValues != null && audienceValues.contains(audience)) {
      return OAuth2TokenValidatorResult.success();
    }
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error("invalid_token", "JWT audience does not include required audience.", null));
  }
}
