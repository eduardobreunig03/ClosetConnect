package com.example.closetconnect.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.closetconnect.entities.User;
import com.example.closetconnect.repositories.UserRepository;
import com.example.closetconnect.services.JwtService;
import com.example.closetconnect.services.TokenBlacklistService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthController(UserRepository userRepo,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authManager,
                          JwtService jwt,
                          TokenBlacklistService tokenBlacklistService) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.authManager = authManager;
        this.jwt = jwt;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    // ========= REGISTER =========
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest body) {
        System.out.println("\n════════════════════════════════════════");
        System.out.println("📝 [AuthController] Registration Request");
        System.out.println("   📧 Email: " + body.email());
        System.out.println("   👤 Name: " + body.name());
        
        if (userRepo.existsByEmail(body.email())) {
            System.out.println("   ❌ Registration failed: Email already in use");
            System.out.println("════════════════════════════════════════\n");
            return ResponseEntity.badRequest().body(Map.of("error", "Email already in use"));
        }
        // Making a new user if it does not exist yet
        User u = new User();
        u.setEmail(body.email());
        u.setPassword(passwordEncoder.encode(body.password()));
        u.setUserName(body.name());
        u.setRating(0); // Default rating for new users
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());

        userRepo.save(u);
        System.out.println("   ✅ User created with ID: " + u.getUserId());

        String token = jwt.generateToken(u.getEmail(), u.getUserId(), "USER");
        System.out.println("   🎉 Registration successful! Token generated.");
        System.out.println("User " + u.getUserName() + " signed in with token");
        System.out.println("════════════════════════════════════════\n");
        return ResponseEntity.ok(new AuthResponse(token, u.getUserId(), u.getEmail(), u.getUserName()));
    }

    // ========= LOGIN =========
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {

        
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(body.email(), body.password())
            );

            var principalEmail = auth.getName(); // email/username
            User u = userRepo.findByEmail(principalEmail)
                    .orElseThrow(() -> new RuntimeException("User not found after auth"));
            
            System.out.println("   👤 User found: " + u.getUserName() + " (ID: " + u.getUserId() + ")");

            String token = jwt.generateToken(u.getEmail(), u.getUserId(), "USER");
            return ResponseEntity.ok(new AuthResponse(token, u.getUserId(), u.getEmail(), u.getUserName()));
        } catch (Exception e) {
            throw e;
        }
    }

    // ========= LOGOUT =========
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization", required = false) String authHeader) {
        System.out.println("\n════════════════════════════════════════");
        System.out.println("🚪 [AuthController] Logout Request");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("   ⚠️  No token provided, but logout still processed");
            System.out.println("════════════════════════════════════════\n");
            return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
        }
        
        String token = authHeader.substring(7);
        System.out.println("   🔑 Token extracted (first 20 chars): " + (token.length() > 20 ? token.substring(0, 20) + "..." : token));
        
        // Blacklist the token
        tokenBlacklistService.blacklistToken(token);
        
        System.out.println("   ✅ Token blacklisted successfully");
        System.out.println("   🎉 Logout successful!");
        System.out.println("════════════════════════════════════════\n");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // ========= WHOAMI (requires bearer token) =========
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(name = "Authorization", required = false) String authHeader) {
        System.out.println("\n════════════════════════════════════════");
        System.out.println("👤 [AuthController] /me endpoint called");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("   ❌ Missing or invalid Authorization header");
            System.out.println("════════════════════════════════════════\n");
            return ResponseEntity.status(401).body(Map.of("error", "Missing token"));
        }
        
        String token = authHeader.substring(7);
        System.out.println("   🔑 Token extracted (first 20 chars): " + (token.length() > 20 ? token.substring(0, 20) + "..." : token));
        
        if (!jwt.isValid(token)) {
            System.out.println("   ❌ Token validation failed in /me endpoint");
            System.out.println("════════════════════════════════════════\n");
            return ResponseEntity.status(401).body(Map.of("error", "Invalid/expired token"));
        }
        
        String email = jwt.getSubject(token);
        User u = userRepo.findByEmail(email).orElse(null);
        
        if (u == null) {
            System.out.println("   ❌ User not found in database");
            System.out.println("════════════════════════════════════════\n");
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        
        System.out.println("   ✅ Profile retrieved successfully");
        System.out.println("   👤 User: " + u.getUserName() + " (" + u.getEmail() + ")");
        System.out.println("════════════════════════════════════════\n");
        return ResponseEntity.ok(new ProfileResponse(u.getUserId(), u.getEmail(), u.getUserName(), "USER"));
    }

    // ======== DTOs (keep it simple) ========
    public record RegisterRequest(String email, String password, String name) {}
    public record LoginRequest(String email, String password) {}
    public record AuthResponse(String token, Long id, String email, String name) {}
    public record ProfileResponse(Long id, String email, String name, String roles) {}
}
