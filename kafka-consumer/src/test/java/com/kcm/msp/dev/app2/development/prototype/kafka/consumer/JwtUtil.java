package com.kcm.msp.dev.app2.development.prototype.kafka.consumer;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.jackson.io.JacksonSerializer;
import io.jsonwebtoken.security.JwkSet;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PublicJwk;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public final class JwtUtil {

  private JwtUtil() {
    throw new UnsupportedOperationException();
  }

  public static KeyPair generateRsaKeyPair() {
    return Jwts.SIG.RS512.keyPair().build();
  }

  public static PublicJwk<PublicKey> generateJwk(final String id, PublicKey publicKey) {
    return Jwks.builder().id(id).key(publicKey).algorithm("RS512").build();
  }

  public static String generateJwksJson(PublicJwk<PublicKey> jwk) {
    JwkSet set = Jwks.set().add(jwk).build();
    byte[] utf8Bytes = new JacksonSerializer().serialize(set);
    return new String(utf8Bytes, StandardCharsets.UTF_8);
  }

  public static String generateJwt(
      final PrivateKey privateKey,
      final String subject,
      final Map<String, Object> claims,
      long expirationInSec) {
    final var now = Instant.now();
    return Jwts.builder()
        .header()
        .add(Map.of("typ", "JWT"))
        .and()
        .subject(subject)
        .claims(claims)
        .issuedAt(Date.from(now))
        .expiration(Date.from(Instant.now().plusSeconds(expirationInSec)))
        .signWith(privateKey)
        .compact();
  }
}
