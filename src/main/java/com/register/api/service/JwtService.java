package com.register.api.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.register.api.dto.response.TokenResponse;
import com.register.api.model.User;

@Service
public class JwtService {

  private final JwtEncoder encoder;
  private final long expirationMinutes;
  public JwtService(JwtEncoder encoder,
    @Value("${app.jwt.expiration-minutes}")  long expirationMinutes) {
    this.encoder = encoder;
    this.expirationMinutes = expirationMinutes;
  }

  public TokenResponse generate(User user) {
      Instant now = Instant.now();
      JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer("user-registration-app")
        .issuedAt(now)
        .expiresAt(now.plus(expirationMinutes, ChronoUnit.MINUTES))
        .subject(user.getId())
        .claim("email", user.getEmail())
        .claim("roles", user.getRoles())
        .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new TokenResponse(token, "Bearer", expirationMinutes * 60);
  }
  
}
