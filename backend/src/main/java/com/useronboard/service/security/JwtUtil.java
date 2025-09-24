package com.useronboard.service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT utility class for token generation and validation
 * Uses RS256 algorithm with RSA key pair for enhanced security
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${security.jwt.private-key-path}")
    private String privateKeyPath;

    @Value("${security.jwt.public-key-path}")
    private String publicKeyPath;

    @Value("${security.jwt.access-token-expiry:15m}")
    private String accessTokenExpiry;

    @Value("${security.jwt.refresh-token-expiry:7d}")
    private String refreshTokenExpiry;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    /**
     * Initialize RSA keys from file system
     */
    public void initializeKeys() {
        try {
            this.privateKey = loadPrivateKey(privateKeyPath);
            this.publicKey = loadPublicKey(publicKeyPath);
            logger.info("JWT keys loaded successfully from {} and {}", privateKeyPath, publicKeyPath);
        } catch (Exception e) {
            logger.error("Failed to load JWT keys from {} and {}", privateKeyPath, publicKeyPath, e);
            throw new IllegalStateException("Failed to initialize JWT keys", e);
        }
    }

    /**
     * Generate access token for user
     */
    public String generateAccessToken(String userId, String email, String roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        claims.put("roles", roles);
        claims.put("type", "access");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(getExpirationDate(accessTokenExpiry))
                .signWith(getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userId)
                .setIssuedAt(new Date())
                .setExpiration(getExpirationDate(refreshTokenExpiry))
                .signWith(getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Extract user ID from token
     */
    public String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract email from token
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }

    /**
     * Extract roles from token
     */
    public String extractRoles(String token) {
        return extractClaim(token, claims -> claims.get("roles", String.class));
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token, String userId) {
        final String tokenUserId = extractUserId(token);
        return (tokenUserId.equals(userId) && !isTokenExpired(token));
    }

    /**
     * Extract specific claim from token
     */
    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract expiration date from token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract all claims from token
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getPublicKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            logger.error("JWT token is malformed: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            logger.error("JWT signature validation failed: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.error("JWT token compact of handler are invalid: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get expiration date based on duration string (e.g., "15m", "7d")
     */
    private Date getExpirationDate(String duration) {
        LocalDateTime expiration = LocalDateTime.now();

        if (duration.endsWith("m")) {
            int minutes = Integer.parseInt(duration.substring(0, duration.length() - 1));
            expiration = expiration.plusMinutes(minutes);
        } else if (duration.endsWith("h")) {
            int hours = Integer.parseInt(duration.substring(0, duration.length() - 1));
            expiration = expiration.plusHours(hours);
        } else if (duration.endsWith("d")) {
            int days = Integer.parseInt(duration.substring(0, duration.length() - 1));
            expiration = expiration.plusDays(days);
        }

        return Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Load private key from file
     */
    private PrivateKey loadPrivateKey(String keyPath) throws Exception {
        String keyContent = new String(Files.readAllBytes(Paths.get(keyPath)));
        keyContent = keyContent.replaceAll("-----BEGIN PRIVATE KEY-----", "")
                              .replaceAll("-----END PRIVATE KEY-----", "")
                              .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    /**
     * Load public key from file
     */
    private PublicKey loadPublicKey(String keyPath) throws Exception {
        String keyContent = new String(Files.readAllBytes(Paths.get(keyPath)));
        keyContent = keyContent.replaceAll("-----BEGIN PUBLIC KEY-----", "")
                              .replaceAll("-----END PUBLIC KEY-----", "")
                              .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    private PrivateKey getPrivateKey() {
        if (privateKey == null) {
            initializeKeys();
        }
        return privateKey;
    }

    private PublicKey getPublicKey() {
        if (publicKey == null) {
            initializeKeys();
        }
        return publicKey;
    }
}
