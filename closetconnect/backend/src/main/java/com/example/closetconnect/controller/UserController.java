package com.example.closetconnect.controller;

import java.util.Map;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.closetconnect.entities.User;
import com.example.closetconnect.services.UserService;

@RestController
@RequestMapping("/api/profile")
public class UserController {

    private final UserService userService;

    private String sanitize(String input) {
        return input == null ? null : Jsoup.clean(input, Safelist.basic());
    }

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/test")
    public ResponseEntity<?> testEndpoint() {
        return ResponseEntity
                .ok(Map.of("message", "UserController is working", "timestamp", System.currentTimeMillis()));
    }

    @GetMapping("/{id}")
    public User getProfile(@PathVariable Long id) {
        Optional<User> user = userService.getUserById(id);
        return user.orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PutMapping("/{id}")
    public User updateProfile(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        Optional<User> optionalUser = userService.getUserById(id);
        User user = optionalUser.orElseThrow(() -> new RuntimeException("User not found"));

        if (updates.containsKey("bio"))
            user.setBio(sanitize(updates.get("bio")));
        if (updates.containsKey("address"))
            user.setAddress(sanitize(updates.get("address")));

        return userService.saveUser(user);
    }

    @PostMapping("/{id}/profile-image")
    public ResponseEntity<?> uploadProfileImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            System.out.println("Profile image upload request received for user ID: " + id);
            System.out.println("File name: " + file.getOriginalFilename());
            System.out.println("File size: " + file.getSize());

            // Validate file
            if (file.isEmpty()) {
                System.out.println("File is empty");
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Check file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                System.out.println("File too large: " + file.getSize());
                return ResponseEntity.badRequest().body(Map.of("error", "File too large. Maximum size is 5MB"));
            }

            // Check file type
            String contentType = file.getContentType();
            System.out.println("Content type: " + contentType);
            if (contentType == null || !contentType.startsWith("image/")) {
                System.out.println("Invalid file type: " + contentType);
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image"));
            }

            // Get user
            Optional<User> optionalUser = userService.getUserById(id);
            if (!optionalUser.isPresent()) {
                System.out.println("User not found with ID: " + id);
                return ResponseEntity.notFound().build();
            }
            User user = optionalUser.get();
            System.out.println("Found user: " + user.getEmail());

            // Convert image to byte array
            byte[] imageBytes = file.getBytes();
            user.setProfileImage(imageBytes);

            User savedUser = userService.saveUser(user);
            System.out.println("Profile image saved successfully for user: " + savedUser.getEmail());

            return ResponseEntity.ok(Map.of(
                    "message", "Profile image uploaded successfully",
                    "userId", savedUser.getUserId()));

        } catch (Exception e) {
            System.out.println("Error uploading profile image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to upload image: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/profile-image")
    public ResponseEntity<byte[]> getProfileImage(@PathVariable Long id) {
        Optional<User> optionalUser = userService.getUserById(id);
        User user = optionalUser.orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getProfileImage() == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .body(user.getProfileImage());
    }
}
