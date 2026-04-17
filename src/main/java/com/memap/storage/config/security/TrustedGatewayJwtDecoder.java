package com.memap.storage.config.security;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class TrustedGatewayJwtDecoder implements JwtDecoder {

  @Override
  public Jwt decode(String token) throws BadJwtException {
    try {
      SignedJWT signedJWT = SignedJWT.parse(token);
      Map<String, Object> headers = signedJWT.getHeader().toJSONObject();
      Map<String, Object> claims = new LinkedHashMap<>(signedJWT.getJWTClaimsSet().getClaims());
      // Spring Security 6.5+ requires timestamp claims as Instant, not Date
      claims.forEach((key, value) -> {
        if (value instanceof Date date) {
          claims.put(key, date.toInstant());
        }
      });
      return Jwt.withTokenValue(token)
          .headers(h -> h.putAll(headers))
          .claims(c -> c.putAll(claims))
          .build();
    } catch (ParseException e) {
      throw new BadJwtException("Failed to parse JWT: " + e.getMessage(), e);
    }
  }
}
