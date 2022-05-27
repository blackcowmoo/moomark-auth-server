package com.blackcowmoo.moomark.auth.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;

import com.blackcowmoo.moomark.auth.model.AuthProvider;
import com.blackcowmoo.moomark.auth.model.Role;
import com.blackcowmoo.moomark.auth.model.dto.TokenResponse;
import com.blackcowmoo.moomark.auth.model.oauth2.Token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Data;

@Service
public class TokenService {

  @Value("${jwt.secret}")
  private String jwtSecret = null;
  private SecretKey key = null;

  private final long tokenPeriod = 1000L * 60L * 60L;
  private final long refreshPeriod = 1000L * 60L * 60L * 24L * 30L * 3L;
  private final String PROVIDER_KEY = "provider";
  private final String ROKE_KEY = "role";
  private final String TOKEN_KEY = "token";
  private final String REFRESH_TOKEN_VALUE = "refresh";

  @PostConstruct
  private void init() {
    key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  // @Override
  public Token generateToken(String id, AuthProvider provider, Role role) {
    Claims claims = Jwts.claims();
    claims.setSubject(id);
    claims.put(PROVIDER_KEY, provider);
    claims.put(ROKE_KEY, role);

    Date now = new Date();

    String token = Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(now)
        .setExpiration(new Date(now.getTime() + tokenPeriod))
        .signWith(key)
        .compact();

    claims.put(TOKEN_KEY, REFRESH_TOKEN_VALUE);

    String refreshToken = Jwts.builder()
        .setClaims(claims)
        .setIssuedAt(now)
        .setExpiration(new Date(now.getTime() + refreshPeriod))
        .signWith(key)
        .compact();

    return new Token(token, refreshToken);
  }

  // @Override
  public boolean verifyToken(String token) {
    try {
      Jws<Claims> claims = Jwts.parserBuilder()
          .setSigningKey(key)
          .build()
          .parseClaimsJws(token);
      return claims.getBody()
          .getExpiration()
          .after(new Date());
    } catch (Exception e) {
      return false;
    }
  }

  public TokenResponse verifyRefreshToken(String refreshToken) {
    try {
      Jws<Claims> claims = Jwts.parserBuilder()
          .setSigningKey(key)
          .build()
          .parseClaimsJws(refreshToken);
      Claims claimsBody = claims.getBody();
      if (claimsBody
          .getExpiration()
          .after(new Date()) && claimsBody.get(TOKEN_KEY, String.class).equals(REFRESH_TOKEN_VALUE)) {
        TokenResponse response = new TokenResponse();
        response.setId(claimsBody.getSubject());
        response.setProvider(claimsBody.get(PROVIDER_KEY, AuthProvider.class));
        response.setRole(claimsBody.get(ROKE_KEY, Role.class));
        return response;
      } else {
        return null;
      }
    } catch (Exception e) {
      return null;
    }
  }

  // @Override
  public String getUid(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }

  public AuthProvider getProvider(String token) {
    return AuthProvider.getAuthProviderValue((String) Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .get("provider"));
  }

  public String getTokenType(String token) {
    return (String) Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .get("token");
  }
}
