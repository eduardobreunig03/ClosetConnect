package com.example.closetconnect.entities;

import java.time.Instant;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rented_clothing_cards")
public class RentedClothingCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rental_id")
    private Long rentalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothing_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "images"})
    private ClothingCard clothingCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "renter_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User renter;

    @Column(name = "rental_date", nullable = false)
    private LocalDateTime rentalDate = LocalDateTime.now();

    @Column(name = "return_date", nullable = false)
    private LocalDateTime returnDate;

    @Column(nullable = false)
    private Boolean returned = false;

    @Column(nullable = false)
    private Boolean request = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // Constructors
    public RentedClothingCard() {}

    public RentedClothingCard(ClothingCard clothingCard, User renter, LocalDateTime returnDate) {
        this.clothingCard = clothingCard;
        this.renter = renter;
        this.returnDate = returnDate;
    }

    // Getters and setters
    public Long getRentalId() { return rentalId; }
    public void setRentalId(Long rentalId) { this.rentalId = rentalId; }

    public ClothingCard getClothingCard() { return clothingCard; }
    public void setClothingCard(ClothingCard clothingCard) { this.clothingCard = clothingCard; }

    public User getRenter() { return renter; }
    public void setRenter(User renter) { this.renter = renter; }

    public LocalDateTime getRentalDate() { return rentalDate; }
    public void setRentalDate(LocalDateTime rentalDate) { this.rentalDate = rentalDate; }

    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }

    public Boolean getReturned() { return returned; }
    public void setReturned(Boolean returned) { this.returned = returned; }

    public Boolean getRequest() { return request; }
    public void setRequest(Boolean request) { this.request = request; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
