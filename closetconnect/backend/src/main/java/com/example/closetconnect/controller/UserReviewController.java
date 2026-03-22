package com.example.closetconnect.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.closetconnect.entities.UserReview;
import com.example.closetconnect.services.UserReviewService;

@RestController
@RequestMapping("/api/user-reviews")
@CrossOrigin(origins = "http://localhost:3000")
public class UserReviewController {

    @Autowired
    private UserReviewService userReviewService;

    // Get all reviews for a specific user (reviews about that user)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserReview>> getReviewsForUser(@PathVariable Long userId) {
        try {
            List<UserReview> reviews = userReviewService.getReviewsForUser(userId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get all reviews by a specific user (reviews written by that user)
    @GetMapping("/by/{userId}")
    public ResponseEntity<List<UserReview>> getReviewsByUser(@PathVariable Long userId) {
        try {
            List<UserReview> reviews = userReviewService.getReviewsByUser(userId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get a specific review by ID
    @GetMapping("/{reviewId}")
    public ResponseEntity<UserReview> getReviewById(@PathVariable Long reviewId) {
        try {
            Optional<UserReview> review = userReviewService.getReviewById(reviewId);
            if (review.isPresent()) {
                return ResponseEntity.ok(review.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get all reviews for a specific rental transaction
    @GetMapping("/rental/{rentalId}")
    public ResponseEntity<List<UserReview>> getReviewsByRentalId(@PathVariable Long rentalId) {
        try {
            List<UserReview> reviews = userReviewService.getReviewsByRentalId(rentalId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Create a new user review for a rental transaction
    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody CreateUserReviewRequest request) {
        try {
            System.out.println("Creating user review with: rentalId=" + request.getRentalId() + 
                ", reviewedUserId=" + request.getReviewedUserId() + 
                ", reviewerId=" + request.getReviewerId() + 
                ", rating=" + request.getRating());
            
            UserReview review = userReviewService.createReview(
                request.getRentalId(),
                request.getReviewedUserId(),
                request.getReviewerId(),
                request.getRating(),
                request.getReviewText()
            );
            System.out.println("User review created successfully: " + review.getReviewId());
            return ResponseEntity.status(HttpStatus.CREATED).body(review);
        } catch (IllegalArgumentException e) {
            System.err.println("User review validation error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("User review creation failed: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to create review: " + e.getMessage()));
        }
    }

    // Update an existing review
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long reviewId,
            @RequestBody UpdateUserReviewRequest request) {
        try {
            UserReview review = userReviewService.updateReview(
                reviewId,
                request.getReviewerId(),
                request.getRating(),
                request.getReviewText()
            );
            return ResponseEntity.ok(review);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to update review"));
        }
    }

    // Delete a review
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long reviewId,
            @RequestParam Long reviewerId) {
        try {
            userReviewService.deleteReview(reviewId, reviewerId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to delete review"));
        }
    }

    // Check if a user can review another user for a specific rental
    @GetMapping("/can-review")
    public ResponseEntity<Boolean> canUserReview(
            @RequestParam(required = false) Long rentalId,
            @RequestParam Long reviewedUserId,
            @RequestParam Long reviewerId) {
        try {
            boolean canReview;
            if (rentalId != null) {
                // New method: check with rental context
                canReview = userReviewService.canUserReview(rentalId, reviewedUserId, reviewerId);
            } else {
                // Legacy method: check without rental context
                canReview = userReviewService.canUserReview(reviewedUserId, reviewerId);
            }
            return ResponseEntity.ok(canReview);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get average rating for a user
    @GetMapping("/user/{userId}/average-rating")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long userId) {
        try {
            Double averageRating = userReviewService.getAverageRating(userId);
            return ResponseEntity.ok(averageRating);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get review count for a user
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> getReviewCount(@PathVariable Long userId) {
        try {
            Long count = userReviewService.getReviewCount(userId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Request DTOs
    public static class CreateUserReviewRequest {
        private Long rentalId;        // The rental transaction ID
        private Long reviewedUserId;   // The user being reviewed (renter or owner)
        private Long reviewerId;      // The user writing the review
        private Integer rating;        // Rating 1-5
        private String reviewText;    // Optional review text

        // Getters and setters
        public Long getRentalId() { return rentalId; }
        public void setRentalId(Long rentalId) { this.rentalId = rentalId; }

        public Long getReviewedUserId() { return reviewedUserId; }
        public void setReviewedUserId(Long reviewedUserId) { this.reviewedUserId = reviewedUserId; }

        public Long getReviewerId() { return reviewerId; }
        public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }

        public String getReviewText() { return reviewText; }
        public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    }

    public static class UpdateUserReviewRequest {
        private Long reviewerId;
        private Integer rating;
        private String reviewText;

        // Getters and setters
        public Long getReviewerId() { return reviewerId; }
        public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }

        public String getReviewText() { return reviewText; }
        public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}