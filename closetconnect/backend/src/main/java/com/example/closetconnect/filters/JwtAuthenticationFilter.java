package com.example.closetconnect.filters;

import java.io.IOException;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.closetconnect.services.JwtService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@ConditionalOnProperty(name = "spring.security.enabled", havingValue = "true", matchIfMissing = true)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String requestURI = request.getRequestURI();
        
        // Skip JWT processing for public endpoints that don't require authentication
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.debug("Missing or invalid Authorization header for {} {}", request.getMethod(), requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String token = authHeader.substring(7);
            String tokenPreview = token.length() > 20 ? token.substring(0, 20) + "..." : token;
            
            if (jwtService.isValid(token)) {
                final String email = jwtService.getSubject(token);
                final String roles = jwtService.getRoles(token);
                final Long userId = jwtService.getId(token);
                
                if (email != null) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roles))
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    logger.info("Authenticated path accessed: {} {} - User: {} (ID: {}, Role: {}), Token: {}", 
                            request.getMethod(), requestURI, email, userId, roles, tokenPreview);
                } else {
                    logger.warn("Token validation passed but email is null");
                }
            } else {
                logger.debug("Token validation failed for {}", requestURI);
                SecurityContextHolder.clearContext();
            }
        } catch (Exception e) {
            logger.warn("JWT authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        filterChain.doFilter(request, response);
    }
    
    /**
     * Check if the endpoint is public (doesn't require JWT authentication)
     */
    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/api/auth/") ||
               uri.startsWith("/api/clothing-cards/") ||
               uri.startsWith("/api/requests/check/") ||
               !uri.startsWith("/api/");  // Non-API endpoints are public
    }
}

