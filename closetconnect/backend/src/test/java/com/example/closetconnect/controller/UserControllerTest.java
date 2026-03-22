package com.example.closetconnect.controller;

import com.example.closetconnect.entities.User;
import com.example.closetconnect.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;

// Mockito matchers
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;

import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;

    @MockBean private UserService userService;

    // --------- GET /api/profile/test ----------
    @Test
    void testEndpoint_ok() throws Exception {
        mvc.perform(get("/api/profile/test"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.message", is("UserController is working")))
           .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    // --------- GET /api/profile/{id} ----------
    @Test
    void getProfile_found() throws Exception {
        User u = user(10L, "alice@example.com", "Alice");
        when(userService.getUserById(10L)).thenReturn(Optional.of(u));

        mvc.perform(get("/api/profile/10"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.userId", is(10)))
           .andExpect(jsonPath("$.email", is("alice@example.com")))
           .andExpect(jsonPath("$.userName", is("Alice")));
    }
    
    // --------- PUT /api/profile/{id} ----------
    @Test
    void updateProfile_ok() throws Exception {
        User existing = user(5L, "bob@example.com", "Bob");
        when(userService.getUserById(5L)).thenReturn(Optional.of(existing));

        // service returns saved user with updated fields
        User saved = user(5L, "bob@example.com", "Bob");
        saved.setBio("new bio");
        saved.setAddress("123 Street");
        when(userService.saveUser(any(User.class))).thenReturn(saved);

        var body = Map.of("bio", "new bio", "address", "123 Street");

        mvc.perform(put("/api/profile/5")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.userId", is(5)))
           .andExpect(jsonPath("$.bio", is("new bio")))
           .andExpect(jsonPath("$.address", is("123 Street")));
    }

    // --------- POST /api/profile/{id}/profile-image (multipart) ----------
    @Test
    void uploadProfileImage_ok() throws Exception {
        User u = user(7L, "eve@example.com", "Eve");
        when(userService.getUserById(7L)).thenReturn(Optional.of(u));

        // saveUser returns the same user (image set inside controller before save)
        when(userService.saveUser(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        byte[] bytes = new byte[] {1,2,3,4};
        MockMultipartFile file = new MockMultipartFile(
                "file", "pic.jpg", "image/jpeg", bytes);

        mvc.perform(multipart("/api/profile/7/profile-image").file(file))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.message", is("Profile image uploaded successfully")))
           .andExpect(jsonPath("$.userId", is(7)));
    }

    @Test
    void uploadProfileImage_emptyFile_400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[] {});

        mvc.perform(multipart("/api/profile/7/profile-image").file(file))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error", is("File is empty")));
    }

    @Test
    void uploadProfileImage_tooLarge_400() throws Exception {
        byte[] big = new byte[5 * 1024 * 1024 + 1]; // > 5MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", big);

        mvc.perform(multipart("/api/profile/7/profile-image").file(file))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error", containsString("File too large")));
    }

    @Test
    void uploadProfileImage_invalidType_400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[] {1,2,3});

        mvc.perform(multipart("/api/profile/7/profile-image").file(file))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error", is("File must be an image")));
    }

    @Test
    void uploadProfileImage_userNotFound_404() throws Exception {
        when(userService.getUserById(7L)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile(
                "file", "pic.jpg", "image/jpeg", new byte[] {1,2});

        mvc.perform(multipart("/api/profile/7/profile-image").file(file))
           .andExpect(status().isNotFound());
    }

    @Test
    void uploadProfileImage_exception_400WithMessage() throws Exception {
        User u = user(7L, "eve@example.com", "Eve");
        when(userService.getUserById(7L)).thenReturn(Optional.of(u));
        when(userService.saveUser(any(User.class))).thenThrow(new RuntimeException("db down"));

        MockMultipartFile file = new MockMultipartFile(
                "file", "pic.jpg", "image/jpeg", new byte[] {1,2});

        mvc.perform(multipart("/api/profile/7/profile-image").file(file))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error", containsString("Failed to upload image: db down")));
    }

    // --------- GET /api/profile/{id}/profile-image ----------
    @Test
    void getProfileImage_ok() throws Exception {
        User u = user(12L, "kate@example.com", "Kate");
        u.setProfileImage(new byte[] {9,8,7});
        when(userService.getUserById(12L)).thenReturn(Optional.of(u));

        mvc.perform(get("/api/profile/12/profile-image"))
           .andExpect(status().isOk())
           .andExpect(header().string("Content-Type", "image/jpeg"))
           .andExpect(content().bytes(new byte[] {9,8,7}));
    }

    @Test
    void getProfileImage_noImage_404() throws Exception {
        User u = user(12L, "kate@example.com", "Kate");
        u.setProfileImage(null);
        when(userService.getUserById(12L)).thenReturn(Optional.of(u));

        mvc.perform(get("/api/profile/12/profile-image"))
           .andExpect(status().isNotFound());
    }

    // -------- helpers --------
    private static User user(Long id, String email, String name) {
        User u = new User();
        setField(u, "userId", id);
        u.setEmail(email);
        u.setUserName(name);
        return u;
    }

    private static void setField(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception ignored) {}
    }
}
