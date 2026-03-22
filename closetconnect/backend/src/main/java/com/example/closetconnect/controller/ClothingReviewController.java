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

import com.example.closetconnect.entities.ClothingReview;
import com.example.closetconnect.services.ClothingReviewService;

@RestController
@RequestMapping("/api/clothing-reviews")
@CrossOrigin(origins = "http://localhost:3000")
public class ClothingReviewController {

    @Autowired
    private ClothingReviewService clothingReviewService;

    // Get all reviews for a specific clothing item
    @GetMapping("/clothing/{clothingId}")
    public ResponseEntity<List<ClothingReview>> getReviewsByClothingId(@PathVariable Long clothingId) {
        try {
            List<ClothingReview> reviews = clothingReviewService.getReviewsByClothingId(clothingId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get all reviews by a specific user
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClothingReview>> getReviewsByUserId(@PathVariable Long userId) {
        try {
            List<ClothingReview> reviews = clothingReviewService.getReviewsByUserId(userId);
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get a specific review by ID
    @GetMapping("/{reviewId}")
    public ResponseEntity<ClothingReview> getReviewById(@PathVariable Long reviewId) {
        try {
            Optional<ClothingReview> review = clothingReviewService.getReviewById(reviewId);
            if (review.isPresent()) {
                return ResponseEntity.ok(review.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Create a new review
    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody CreateReviewRequest request) {
        try {
            ClothingReview review = clothingReviewService.createReview(
                request.getClothingId(),
                request.getReviewerId(),
                request.getRating(),
                request.getReviewText()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(review);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to create review"));
        }
    }

    // Update an existing review
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long reviewId,
            @RequestBody UpdateReviewRequest request) {
        try {
            ClothingReview review = clothingReviewService.updateReview(
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
            clothingReviewService.deleteReview(reviewId, reviewerId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to delete review"));
        }
    }

    // Check if a user can review a specific clothing item
    @GetMapping("/can-review")
    public ResponseEntity<Boolean> canUserReview(
            @RequestParam Long clothingId,
            @RequestParam Long userId) {
        try {
            boolean canReview = clothingReviewService.canUserReview(clothingId, userId);
            return ResponseEntity.ok(canReview);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get average rating for a clothing item
    @GetMapping("/clothing/{clothingId}/average-rating")
    public ResponseEntity<Double> getAverageRating(@PathVariable Long clothingId) {
        try {
            Double averageRating = clothingReviewService.getAverageRating(clothingId);
            return ResponseEntity.ok(averageRating);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get review count for a clothing item
    @GetMapping("/clothing/{clothingId}/count")
    public ResponseEntity<Long> getReviewCount(@PathVariable Long clothingId) {
        try {
            Long count = clothingReviewService.getReviewCount(clothingId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Request DTOs
    public static class CreateReviewRequest {
        private Long clothingId;
        private Long reviewerId;
        private Integer rating;
        private String reviewText;

        // Getters and setters
        public Long getClothingId() { return clothingId; }
        public void setClothingId(Long clothingId) { this.clothingId = clothingId; }

        public Long getReviewerId() { return reviewerId; }
        public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }

        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }

        public String getReviewText() { return reviewText; }
        public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    }

    public static class UpdateReviewRequest {
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
