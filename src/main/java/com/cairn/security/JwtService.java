package com.cairn.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** WHY: Handles creation and verification of JWTs for secure, stateless API authentication. */
@Service
public class JwtService {

  private final JwtProperties jwtProperties;
  private final Algorithm algorithm;
  private final JWTVerifier verifier;

  public JwtService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    // HMAC256 requires a secret of sufficient length
    this.algorithm = Algorithm.HMAC256(jwtProperties.getSecret());
    this.verifier = JWT.require(algorithm).withIssuer("cairn-ai").build();
  }

  /**
   * WHY: Generates a signed token for a user that will be returned to the client upon login.
   *
   * @param user The authenticated user
   * @return The JWT token string
   */
  public String generateToken(User user) {
    return JWT.create()
        .withIssuer("cairn-ai")
        .withSubject(user.getId().toString())
        .withClaim("role", user.getRole().name())
        .withClaim("githubId", user.getGithubId())
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + jwtProperties.getExpirationMs()))
        .sign(algorithm);
  }

  /**
   * WHY: Validates an incoming token and parses the claims.
   *
   * @param token The raw token string
   * @return DecodedJWT if valid, null if invalid
   */
  public DecodedJWT verifyToken(String token) {
    try {
      return verifier.verify(token);
    } catch (JWTVerificationException e) {
      // Invalid signature/claims
      return null;
    }
  }

  /** WHY: Extracts the user ID from the validated token. */
  public UUID getUserIdFromToken(DecodedJWT decodedJWT) {
    return UUID.fromString(decodedJWT.getSubject());
  }
}
