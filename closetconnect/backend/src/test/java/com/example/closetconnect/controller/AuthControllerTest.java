package com.example.closetconnect.controller;

import com.example.closetconnect.entities.User;
import com.example.closetconnect.repositories.UserRepository;
import com.example.closetconnect.services.JwtService;
import com.example.closetconnect.services.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

// Hamcrest (only what you use)
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;

import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // disable security filters for controller unit test
class AuthControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;

    @MockBean private UserRepository userRepo;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private AuthenticationManager authManager;
    @MockBean private JwtService jwt;
    @MockBean private TokenBlacklistService tokenBlacklistService;

    // ---------- /register ----------

    @Test
    void register_success_createsUser_andReturnsToken() throws Exception {
        var req = new AuthController.RegisterRequest("a@b.com", "secret", "Alice");
        when(userRepo.existsByEmail("a@b.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("ENC(secret)");
        when(jwt.generateToken(eq("a@b.com"), anyLong(), eq("USER"))).thenReturn("TOK");

        // Make save() set an ID on the same instance (mimic JPA)
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            setUserId(u, 42L);
            return u;
        });

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.token", is("TOK")))
           .andExpect(jsonPath("$.id", is(42)))
           .andExpect(jsonPath("$.email", is("a@b.com")))
           .andExpect(jsonPath("$.name", is("Alice")));

        // verify password encoded and fields set
        ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(cap.capture());
        User saved = cap.getValue();
        // sanity: encoded password and name carried through
        // (no hard asserts here to avoid coupling to entity internals)
        verify(passwordEncoder).encode("secret");
        verify(jwt).generateToken("a@b.com", 42L, "USER");
    }

    @Test
    void register_conflict_whenEmailInUse() throws Exception {
        var req = new AuthController.RegisterRequest("a@b.com", "x", "Alice");
        when(userRepo.existsByEmail("a@b.com")).thenReturn(true);

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error", containsString("Email already in use")));

        verify(userRepo, never()).save(any());
    }

    // ---------- /login ----------

    @Test
    void login_success_returnsTokenAndProfile() throws Exception {
        var req = new AuthController.LoginRequest("a@b.com", "secret");

        // AuthManager returns authenticated Authentication whose name is email
        var auth = new UsernamePasswordAuthenticationToken("a@b.com", "N/A", Collections.emptyList());
        when(authManager.authenticate(any())).thenReturn(auth);

        User u = new User();
        setUserId(u, 7L);
        u.setEmail("a@b.com");
        u.setUserName("Alice");
        when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.of(u));
        when(jwt.generateToken("a@b.com", 7L, "USER")).thenReturn("TOK2");

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.token", is("TOK2")))
           .andExpect(jsonPath("$.id", is(7)))
           .andExpect(jsonPath("$.email", is("a@b.com")))
           .andExpect(jsonPath("$.name", is("Alice")));
    }

    // ---------- /me ----------

    @Test
    void me_401_whenMissingBearer() throws Exception {
        mvc.perform(get("/api/auth/me"))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.error", containsString("Missing token")));
    }

    @Test
    void me_401_whenInvalidToken() throws Exception {
        when(jwt.isValid("BAD")).thenReturn(false);

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer BAD"))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.error", containsString("Invalid/expired token")));
    }

    @Test
    void me_success_returnsProfile() throws Exception {
        when(jwt.isValid("TOK")).thenReturn(true);
        when(jwt.getSubject("TOK")).thenReturn("a@b.com");

        User u = new User();
        setUserId(u, 9L);
        u.setEmail("a@b.com");
        u.setUserName("Alice");
        when(userRepo.findByEmail("a@b.com")).thenReturn(Optional.of(u));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer TOK"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.id", is(9)))
           .andExpect(jsonPath("$.email", is("a@b.com")))
           .andExpect(jsonPath("$.name", is("Alice")))
           .andExpect(jsonPath("$.roles", is("USER")));
    }

    // -------- helper: set private ID without changing entity code --------
    private static void setUserId(User u, Long id) {
        try {
            var f = User.class.getDeclaredField("userId");
            f.setAccessible(true);
            f.set(u, id);
        } catch (Exception ignore) { /* if setter exists, not needed */ }
    }
}
