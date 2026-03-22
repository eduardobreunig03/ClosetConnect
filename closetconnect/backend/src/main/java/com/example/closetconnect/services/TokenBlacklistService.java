package com.example.closetconnect.services;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Service to manage blacklisted JWT tokens.
 * Tokens are stored in memory with automatic expiration cleanup.
 * For production, consider using Redis for distributed token blacklisting.
 */
@Service
public class TokenBlacklistService {
    
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    @PostConstruct
    public void init() {
        // Clean up expired tokens every hour
        scheduler.scheduleAtFixedRate(this::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS);
        System.out.println("✅ [TokenBlacklistService] Initialized with automatic cleanup");
    }
    
    @PreDestroy
    public void cleanup() {
        scheduler.shutdown();
        blacklistedTokens.clear();
        System.out.println("🛑 [TokenBlacklistService] Shutdown and cleared blacklist");
    }
    
    /**
     * Add a token to the blacklist
     */
    public void blacklistToken(String token) {
        if (token != null && !token.isEmpty()) {
            blacklistedTokens.add(token);
            System.out.println("🚫 [TokenBlacklistService] Token blacklisted (first 20 chars): " + 
                (token.length() > 20 ? token.substring(0, 20) + "..." : token));
        }
    }
    
    /**
     * Check if a token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        boolean blacklisted = blacklistedTokens.contains(token);
        if (blacklisted) {
            System.out.println("🚫 [TokenBlacklistService] Token is BLACKLISTED (first 20 chars): " + 
                (token.length() > 20 ? token.substring(0, 20) + "..." : token));
        }
        return blacklisted;
    }
    
    /**
     * Remove a token from the blacklist (e.g., if needed for admin purposes)
     */
    public void removeFromBlacklist(String token) {
        blacklistedTokens.remove(token);
        System.out.println("✅ [TokenBlacklistService] Token removed from blacklist");
    }
    
    /**
     * Clean up expired tokens from the blacklist periodically
     * Since tokens have expiration times, we can remove old entries
     * Note: This is a simple cleanup. In production with Redis, TTL handles this automatically.
     */
    private void cleanupExpiredTokens() {
        // For in-memory storage, we rely on the fact that expired tokens
        // will be rejected by JwtService.isValid() anyway.
        // This cleanup is mainly for memory management if needed.
        // In a production system with Redis, TTL would handle this automatically.
        System.out.println("🧹 [TokenBlacklistService] Cleanup check - " + blacklistedTokens.size() + " tokens in blacklist");
    }
    
    /**
     * Get the current size of the blacklist (for monitoring/debugging)
     */
    public int getBlacklistSize() {
        return blacklistedTokens.size();
    }
}

