package com.example.closetconnect.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.ClothingReview;
import com.example.closetconnect.entities.User;

@Repository
public interface ClothingReviewRepository extends JpaRepository<ClothingReview, Long> {
    
    // Find all reviews for a specific clothing item
    List<ClothingReview> findByClothingCardOrderByCreatedAtDesc(ClothingCard clothingCard);
    
    // Find all reviews for a specific clothing item by clothing ID
    List<ClothingReview> findByClothingCardClothingIdOrderByCreatedAtDesc(Long clothingId);
    
    // Find all reviews by a specific user
    List<ClothingReview> findByReviewerOrderByCreatedAtDesc(User reviewer);
    
    // Find all reviews by a specific user ID
    List<ClothingReview> findByReviewerUserIdOrderByCreatedAtDesc(Long userId);
    
    // Find a specific review by clothing item and reviewer
    Optional<ClothingReview> findByClothingCardAndReviewer(ClothingCard clothingCard, User reviewer);
    
    // Find a specific review by clothing ID and reviewer ID
    Optional<ClothingReview> findByClothingCardClothingIdAndReviewerUserId(Long clothingId, Long reviewerId);
    
    // Check if a user has already reviewed a specific clothing item
    boolean existsByClothingCardClothingIdAndReviewerUserId(Long clothingId, Long reviewerId);
    
    // Count reviews for a specific clothing item
    long countByClothingCardClothingId(Long clothingId);
}

