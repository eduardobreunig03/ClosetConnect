package com.example.closetconnect.services;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    
    @Autowired(required = false)
    private TokenBlacklistService tokenBlacklistService;
    
    @Value("${jwt.secret:MySecretKeyForJWTTokenGenerationThatShouldBeAtLeast256BitsLongForHS256Algorithm}")
    private String secretKey;
    
    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email, Long id, String roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", id);
        claims.put("roles", roles);
        String token = createToken(claims, email);
        
        logger.info("Token generated for user: {} (ID: {}, Role: {})", email, id, roles);
        return token;
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        var builder = Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate);
        
        // Add custom claims
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }
        
        return builder.signWith(getSigningKey())
                .compact();
    }

    public Boolean isValid(String token) {
        try {
            if (tokenBlacklistService != null && tokenBlacklistService.isTokenBlacklisted(token)) {
                logger.debug("Token validation failed: token is blacklisted");
                return false;
            }
            
            Claims claims = extractAllClaims(token);
            final String email = claims.getSubject();
            Date expiry = claims.getExpiration();
            boolean expired = expiry.before(new Date());
            boolean valid = email != null && !expired;
            
            if (!valid) {
                if (email == null) {
                    logger.debug("Token validation failed: email is null");
                }
                if (expired) {
                    logger.debug("Token validation failed: token expired at {}", expiry);
                }
            }
            
            return valid;
        } catch (Exception e) {
            logger.debug("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String getSubject(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.getSubject();
        } catch (Exception e) {
            logger.warn("Failed to extract subject from token: {}", e.getMessage());
            throw e;
        }
    }

    public Long getId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object idObj = claims.get("id");
            if (idObj instanceof Number) {
                return ((Number) idObj).longValue();
            }
            logger.warn("User ID not found or invalid type in token");
            return null;
        } catch (Exception e) {
            logger.warn("Failed to extract user ID from token: {}", e.getMessage());
            throw e;
        }
    }

    public String getRoles(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object rolesObj = claims.get("roles");
            return rolesObj != null ? rolesObj.toString() : null;
        } catch (Exception e) {
            logger.warn("Failed to extract roles from token: {}", e.getMessage());
            throw e;
        }
    }

    private Date extractExpiration(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getExpiration();
    }

    private Claims extractAllClaims(String token) {
        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build();
            return parser.parseClaimsJws(token).getBody();
        } catch (Exception e) {
            logger.debug("Failed to parse token claims: {}", e.getMessage());
            throw e;
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
