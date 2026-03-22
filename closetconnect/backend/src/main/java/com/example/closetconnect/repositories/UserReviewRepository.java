package com.example.closetconnect.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.closetconnect.entities.RentedClothingCard;
import com.example.closetconnect.entities.User;
import com.example.closetconnect.entities.UserReview;

@Repository
public interface UserReviewRepository extends JpaRepository<UserReview, Long> {
    
    // Find all reviews for a specific user (reviews about that user)
    List<UserReview> findByReviewedUserOrderByCreatedAtDesc(User reviewedUser);
    
    // Find all reviews for a specific user ID (reviews about that user)
    List<UserReview> findByReviewedUserUserIdOrderByCreatedAtDesc(Long reviewedUserId);
    
    // Find all reviews by a specific user (reviews written by that user)
    List<UserReview> findByReviewerOrderByCreatedAtDesc(User reviewer);
    
    // Find all reviews by a specific user ID (reviews written by that user)
    List<UserReview> findByReviewerUserIdOrderByCreatedAtDesc(Long reviewerId);
    
    // Find all reviews for a specific rental transaction
    List<UserReview> findByRentalOrderByCreatedAtDesc(RentedClothingCard rental);
    
    // Find all reviews for a specific rental ID
    List<UserReview> findByRentalRentalIdOrderByCreatedAtDesc(Long rentalId);
    
    // Find a specific review by rental and reviewer
    Optional<UserReview> findByRentalAndReviewer(RentedClothingCard rental, User reviewer);
    
    // Find a specific review by rental ID and reviewer ID
    Optional<UserReview> findByRentalRentalIdAndReviewerUserId(Long rentalId, Long reviewerId);
    
    // Check if a user has already reviewed for a specific rental
    boolean existsByRentalRentalIdAndReviewerUserId(Long rentalId, Long reviewerId);
    
    // Find a specific review by reviewed user and reviewer (for backwards compatibility)
    Optional<UserReview> findByReviewedUserAndReviewer(User reviewedUser, User reviewer);
    
    // Find a specific review by reviewed user ID and reviewer ID (for backwards compatibility)
    Optional<UserReview> findByReviewedUserUserIdAndReviewerUserId(Long reviewedUserId, Long reviewerId);
    
    // Check if a user has already reviewed another user (for backwards compatibility)
    boolean existsByReviewedUserUserIdAndReviewerUserId(Long reviewedUserId, Long reviewerId);
    
    // Count reviews for a specific user
    long countByReviewedUserUserId(Long reviewedUserId);
    
    // Get average rating for a specific user
    @Query("SELECT AVG(ur.rating) FROM UserReview ur WHERE ur.reviewedUser.userId = :userId")
    Double getAverageRatingByReviewedUserUserId(@Param("userId") Long userId);
}
