package com.example.closetconnect.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.ClothingReview;
import com.example.closetconnect.entities.User;
import com.example.closetconnect.repositories.ClothingCardRepository;
import com.example.closetconnect.repositories.ClothingReviewRepository;
import com.example.closetconnect.repositories.RentedClothingCardRepository;
import com.example.closetconnect.repositories.UserRepository;

@Service
public class ClothingReviewService {

    @Autowired
    private ClothingReviewRepository clothingReviewRepository;

    @Autowired
    private ClothingCardRepository clothingCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RentedClothingCardRepository rentedClothingCardRepository;

    // Get all reviews for a specific clothing item
    public List<ClothingReview> getReviewsByClothingId(Long clothingId) {
        return clothingReviewRepository.findByClothingCardClothingIdOrderByCreatedAtDesc(clothingId);
    }

    // Get all reviews by a specific user
    public List<ClothingReview> getReviewsByUserId(Long userId) {
        return clothingReviewRepository.findByReviewerUserIdOrderByCreatedAtDesc(userId);
    }

    // Create a new review
    public ClothingReview createReview(Long clothingId, Long reviewerId, Integer rating, String reviewText) {
        // Validate rating (1-5)
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Get clothing card and reviewer
        Optional<ClothingCard> clothingCardOpt = clothingCardRepository.findById(clothingId);
        Optional<User> reviewerOpt = userRepository.findById(reviewerId);

        if (clothingCardOpt.isEmpty()) {
            throw new IllegalArgumentException("Clothing item not found");
        }
        if (reviewerOpt.isEmpty()) {
            throw new IllegalArgumentException("Reviewer not found");
        }

        ClothingCard clothingCard = clothingCardOpt.get();
        User reviewer = reviewerOpt.get();

        // Check if user is trying to review their own item
        if (clothingCard.getOwner().getUserId().equals(reviewerId)) {
            throw new IllegalArgumentException("Cannot review your own clothing item");
        }

        // Check if user has already reviewed this item
        if (clothingReviewRepository.existsByClothingCardClothingIdAndReviewerUserId(clothingId, reviewerId)) {
            throw new IllegalArgumentException("You have already reviewed this clothing item");
        }

        // Check if user has rented and returned this item (required for reviews)
        if (!rentedClothingCardRepository.existsByClothingCardClothingIdAndRenterUserIdAndReturnedTrue(clothingId, reviewerId)) {
            throw new IllegalArgumentException("You can only review items you have rented and returned. Please review from your rental history.");
        }

        // Create new review
        ClothingReview review = new ClothingReview();
        review.setClothingCard(clothingCard);
        review.setReviewer(reviewer);
        review.setReview(rating);
        review.setReviewText(reviewText);
        review.setCreatedAt(Instant.now());
        review.setUpdatedAt(Instant.now());

        return clothingReviewRepository.save(review);
    }

    // Update an existing review
    public ClothingReview updateReview(Long reviewId, Long reviewerId, Integer rating, String reviewText) {
        // Validate rating (1-5)
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Optional<ClothingReview> reviewOpt = clothingReviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            throw new IllegalArgumentException("Review not found");
        }

        ClothingReview review = reviewOpt.get();

        // Check if the reviewer is the owner of the review
        if (!review.getReviewer().getUserId().equals(reviewerId)) {
            throw new IllegalArgumentException("You can only update your own reviews");
        }

        // Update review
        review.setReview(rating);
        review.setReviewText(reviewText);
        review.setUpdatedAt(Instant.now());

        return clothingReviewRepository.save(review);
    }

    // Delete a review
    public void deleteReview(Long reviewId, Long reviewerId) {
        Optional<ClothingReview> reviewOpt = clothingReviewRepository.findById(reviewId);
        if (reviewOpt.isEmpty()) {
            throw new IllegalArgumentException("Review not found");
        }

        ClothingReview review = reviewOpt.get();

        // Check if the reviewer is the owner of the review
        if (!review.getReviewer().getUserId().equals(reviewerId)) {
            throw new IllegalArgumentException("You can only delete your own reviews");
        }

        clothingReviewRepository.deleteById(reviewId);
    }

    // Get a specific review
    public Optional<ClothingReview> getReviewById(Long reviewId) {
        return clothingReviewRepository.findById(reviewId);
    }

    // Check if a user can review a specific clothing item
    public boolean canUserReview(Long clothingId, Long userId) {
        // Check if clothing item exists
        Optional<ClothingCard> clothingCardOpt = clothingCardRepository.findById(clothingId);
        if (clothingCardOpt.isEmpty()) {
            return false;
        }

        ClothingCard clothingCard = clothingCardOpt.get();

        // Check if user is trying to review their own item
        if (clothingCard.getOwner().getUserId().equals(userId)) {
            return false;
        }

        // Check if user has already reviewed this item
        if (clothingReviewRepository.existsByClothingCardClothingIdAndReviewerUserId(clothingId, userId)) {
            return false;
        }

        // Check if user has rented and returned this item (required for reviews)
        return rentedClothingCardRepository.existsByClothingCardClothingIdAndRenterUserIdAndReturnedTrue(clothingId, userId);
    }

    // Get average rating for a clothing item
    public Double getAverageRating(Long clothingId) {
        List<ClothingReview> reviews = clothingReviewRepository.findByClothingCardClothingIdOrderByCreatedAtDesc(clothingId);
        if (reviews.isEmpty()) {
            return 0.0;
        }

        double sum = reviews.stream().mapToInt(ClothingReview::getReview).sum();
        return sum / reviews.size();
    }

    // Get review count for a clothing item
    public Long getReviewCount(Long clothingId) {
        return clothingReviewRepository.countByClothingCardClothingId(clothingId);
    }
}