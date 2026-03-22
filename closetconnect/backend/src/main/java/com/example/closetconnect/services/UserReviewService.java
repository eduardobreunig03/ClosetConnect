package com.example.closetconnect.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.closetconnect.entities.RentedClothingCard;
import com.example.closetconnect.entities.User;
import com.example.closetconnect.entities.UserReview;
import com.example.closetconnect.repositories.RentedClothingCardRepository;
import com.example.closetconnect.repositories.UserRepository;
import com.example.closetconnect.repositories.UserReviewRepository;

@Service
public class UserReviewService {

    @Autowired
    private UserReviewRepository userReviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RentedClothingCardRepository rentedClothingCardRepository;

    // Get all reviews for a specific user (reviews about that user)
    public List<UserReview> getReviewsForUser(Long userId) {
        return userReviewRepository.findByReviewedUserUserIdOrderByCreatedAtDesc(userId);
    }

    // Get all reviews by a specific user (reviews written by that user)
    public List<UserReview> getReviewsByUser(Long userId) {
        return userReviewRepository.findByReviewerUserIdOrderByCreatedAtDesc(userId);
    }

    // Get all reviews for a specific rental transaction
    public List<UserReview> getReviewsByRentalId(Long rentalId) {
        return userReviewRepository.findByRentalRentalIdOrderByCreatedAtDesc(rentalId);
    }

    // Get a specific review by ID
    public Optional<UserReview> getReviewById(Long reviewId) {
        return userReviewRepository.findById(reviewId);
    }

    // Create a new user review for a rental transaction
    public UserReview createReview(Long rentalId, Long reviewedUserId, Long reviewerId, Integer rating, String reviewText) {
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Cannot review yourself
        if (reviewedUserId.equals(reviewerId)) {
            throw new IllegalArgumentException("Users cannot review themselves");
        }

        // Get rental transaction
        Optional<RentedClothingCard> rentalOpt = rentedClothingCardRepository.findById(rentalId);
        if (rentalOpt.isEmpty()) {
            throw new IllegalArgumentException("Rental transaction not found");
        }
        RentedClothingCard rental = rentalOpt.get();

        // Validate that the reviewer is part of the rental (either owner or renter)
        Long ownerId = rental.getClothingCard().getOwner().getUserId();
        Long renterId = rental.getRenter().getUserId();

        if (!reviewerId.equals(ownerId) && !reviewerId.equals(renterId)) {
            throw new IllegalArgumentException("You can only review users for rentals you participated in");
        }

        // Validate that the reviewed user is the other party in the rental
        if (!reviewedUserId.equals(ownerId) && !reviewedUserId.equals(renterId)) {
            throw new IllegalArgumentException("You can only review the other party in the rental transaction");
        }

        // Ensure reviewer is reviewing the OTHER party (not themselves)
        if (reviewedUserId.equals(reviewerId)) {
            throw new IllegalArgumentException("You cannot review yourself");
        }

        // Check if reviewer has already reviewed for this rental
        if (userReviewRepository.existsByRentalRentalIdAndReviewerUserId(rentalId, reviewerId)) {
            throw new IllegalArgumentException("You have already reviewed for this rental transaction");
        }

        // Get user entities
        User reviewedUser = userRepository.findById(reviewedUserId)
            .orElseThrow(() -> new IllegalArgumentException("Reviewed user not found"));
        
        User reviewer = userRepository.findById(reviewerId)
            .orElseThrow(() -> new IllegalArgumentException("Reviewer not found"));

        // Create and save review
        UserReview review = new UserReview(rental, reviewedUser, reviewer, rating, reviewText);
        review.setCreatedAt(Instant.now());
        review.setUpdatedAt(Instant.now());
        return userReviewRepository.save(review);
    }

    // Update an existing review
    public UserReview updateReview(Long reviewId, Long reviewerId, Integer rating, String reviewText) {
        UserReview review = userReviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getReviewer().getUserId().equals(reviewerId)) {
            throw new IllegalArgumentException("You can only update your own reviews");
        }

        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        review.setRating(rating);
        review.setReviewText(reviewText);
        review.setUpdatedAt(Instant.now());

        return userReviewRepository.save(review);
    }

    // Delete a review
    public void deleteReview(Long reviewId, Long reviewerId) {
        UserReview review = userReviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        if (!review.getReviewer().getUserId().equals(reviewerId)) {
            throw new IllegalArgumentException("You can only delete your own reviews");
        }

        userReviewRepository.delete(review);
    }

    // Check if a user can review another user for a specific rental
    public boolean canUserReview(Long rentalId, Long reviewedUserId, Long reviewerId) {
        // Cannot review yourself
        if (reviewedUserId.equals(reviewerId)) {
            return false;
        }

        // Check if already reviewed for this rental
        if (userReviewRepository.existsByRentalRentalIdAndReviewerUserId(rentalId, reviewerId)) {
            return false;
        }

        // Validate rental exists and user is part of it
        Optional<RentedClothingCard> rentalOpt = rentedClothingCardRepository.findById(rentalId);
        if (rentalOpt.isEmpty()) {
            return false;
        }

        RentedClothingCard rental = rentalOpt.get();
        Long ownerId = rental.getClothingCard().getOwner().getUserId();
        Long renterId = rental.getRenter().getUserId();

        // Reviewer must be part of the rental
        if (!reviewerId.equals(ownerId) && !reviewerId.equals(renterId)) {
            return false;
        }

        // Reviewed user must be the other party
        return reviewedUserId.equals(ownerId) || reviewedUserId.equals(renterId);
    }

    // Legacy method for backwards compatibility (check if user can review another user without rental context)
    public boolean canUserReview(Long reviewedUserId, Long reviewerId) {
        if (reviewedUserId.equals(reviewerId)) {
            return false;
        }
        return !userReviewRepository.existsByReviewedUserUserIdAndReviewerUserId(reviewedUserId, reviewerId);
    }

    // Get average rating for a user
    public Double getAverageRating(Long userId) {
        return userReviewRepository.getAverageRatingByReviewedUserUserId(userId);
    }

    // Get review count for a user
    public long getReviewCount(Long userId) {
        return userReviewRepository.countByReviewedUserUserId(userId);
    }
}