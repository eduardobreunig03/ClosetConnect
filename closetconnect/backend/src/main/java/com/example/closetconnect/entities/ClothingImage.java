package com.example.closetconnect.entities;

import java.time.Instant;
import java.util.Base64;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
@Table(name = "clothing_images")
public class ClothingImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothing_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    @JsonBackReference
    private ClothingCard clothingCard;

    @Column(name = "image", nullable = false, columnDefinition = "bytea")
    private byte[] image;

    @Column(name = "position", nullable = false)
    private Integer position = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getImageId() { return imageId; }
    public void setImageId(Long imageId) { this.imageId = imageId; }

    public ClothingCard getClothingCard() { return clothingCard; }
    public void setClothingCard(ClothingCard clothingCard) { this.clothingCard = clothingCard; }

    public byte[] getImage() { return image; }
    public void setImage(byte[] image) { this.image = image; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // Method to get image as base64 string for JSON serialization
    public String getImageData() {
        if (image == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(image);
    }
}


