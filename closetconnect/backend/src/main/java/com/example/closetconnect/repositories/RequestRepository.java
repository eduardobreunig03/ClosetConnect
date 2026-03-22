package com.example.closetconnect.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.closetconnect.entities.Request;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    
    // Find all requests by a specific user (as requester)
    List<Request> findByFromUserId(Long fromUserId);
    
    // Find all requests received by a clothing card owner
    List<Request> findByToUserId(Long toUserId);
    
    // Find requests by requester and approval status
    List<Request> findByFromUserIdAndApproved(Long fromUserId, Boolean approved);
    
    // Find requests by owner and approval status
    List<Request> findByToUserIdAndApproved(Long toUserId, Boolean approved);
    
    // Check if a user has already requested a specific clothing card
    Optional<Request> findByClothingIdAndFromUserId(Long clothingId, Long fromUserId);
    
    // Find requests ordered by creation date (descending) for a specific owner
    List<Request> findByToUserIdOrderByCreatedAtDesc(Long toUserId);
    
    // Find requests ordered by creation date (descending) for a specific requester
    List<Request> findByFromUserIdOrderByCreatedAtDesc(Long fromUserId);
}
