package com.kcm.msp.dev.app2.development.prototype.kafka.consumer;

import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public final class JwtUtil {

  private JwtUtil() {
    throw new UnsupportedOperationException();
  }

  public static String generateJwt(
      PrivateKey privateKey, String subject, Map<String, Object> claims, long expirationInSec) {
    // var claims = Map.of("aud", audience);
    final var now = Instant.now();
    return Jwts.builder()
        .subject(subject)
        .claims(claims)
        .issuedAt(Date.from(now))
        .expiration(java.sql.Date.from(Instant.now().plusSeconds(expirationInSec)))
        .signWith(privateKey) // RSA Signature
        .compact();
  }

  public static Map<String, Object> generateJwks(PublicKey publicKey) {
    Map<String, Object> jwks = new HashMap<>();
    jwks.put("kty", "RSA");
    jwks.put("alg", "RS256");
    jwks.put("use", "sig");
    jwks.put("n", java.util.Base64.getUrlEncoder().encodeToString(publicKey.getEncoded()));
    jwks.put("e", "AQAB"); // Standard exponent for RSA keys
    return jwks;
  }

  public static KeyPair generateRsaKeyPair() throws Exception {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    return keyPairGenerator.generateKeyPair();
  }
}
