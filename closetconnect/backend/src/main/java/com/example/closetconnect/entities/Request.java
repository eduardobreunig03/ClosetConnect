package com.example.closetconnect.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "clothing_id", nullable = false)
    private Long clothingId;

    @Column(name = "from_user_id", nullable = false)
    private Long fromUserId;

    @Column(name = "to_user_id", nullable = false)
    private Long toUserId;

    @Column(name = "availability_range_request", nullable = false)
    private LocalDateTime availabilityRangeRequest;

    @Column(name = "requester_contact_info", nullable = false, length = 255)
    private String requesterContactInfo;

    @Column(name = "comments_to_owner", columnDefinition = "TEXT")
    private String commentsToOwner;

    @Column(name = "approved", nullable = true, columnDefinition = "BOOLEAN")
    private Boolean approved = null;  // null = pending, true = approved, false = rejected

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public Request() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.approved = null;  // Explicitly set to null for pending status
    }

    public Request(Long clothingId, Long fromUserId, Long toUserId, 
                   LocalDateTime availabilityRangeRequest, String requesterContactInfo, 
                   String commentsToOwner) {
        this();
        this.clothingId = clothingId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.availabilityRangeRequest = availabilityRangeRequest;
        this.requesterContactInfo = requesterContactInfo;
        this.commentsToOwner = commentsToOwner;
        this.approved = null;  // Explicitly set to null for pending status
    }

    // Getters and Setters
    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getClothingId() {
        return clothingId;
    }

    public void setClothingId(Long clothingId) {
        this.clothingId = clothingId;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public LocalDateTime getAvailabilityRangeRequest() {
        return availabilityRangeRequest;
    }

    public void setAvailabilityRangeRequest(LocalDateTime availabilityRangeRequest) {
        this.availabilityRangeRequest = availabilityRangeRequest;
    }

    public String getRequesterContactInfo() {
        return requesterContactInfo;
    }

    public void setRequesterContactInfo(String requesterContactInfo) {
        this.requesterContactInfo = requesterContactInfo;
    }

    public String getCommentsToOwner() {
        return commentsToOwner;
    }

    public void setCommentsToOwner(String commentsToOwner) {
        this.commentsToOwner = commentsToOwner;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
        // Ensure approved is null for new requests (pending status)
        this.approved = null;  // Always set to null for new requests
        
        // Validate that availabilityRangeRequest is not null before persisting
        if (this.availabilityRangeRequest == null) {
            throw new IllegalStateException("Cannot persist Request: availabilityRangeRequest is null. This is required.");
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}