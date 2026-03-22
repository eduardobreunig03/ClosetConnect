package com.example.closetconnect.entities;

import java.time.Instant;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "clothing_cards")
public class ClothingCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "clothing_id")
    private Long clothingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
    private User owner;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(nullable = false)
    private Integer cost;

    @Column(nullable = false)
    private Integer deposit;

    @Column(nullable = false, length = 5)
    private String size;

    @Column(nullable = false)
    private Boolean availability = true;

    @Column(name = "availability_day", nullable = false)
    private LocalDateTime availabilityDay = LocalDateTime.now();

    @Column(name = "date_posted", nullable = false)
    private LocalDateTime datePosted = LocalDateTime.now();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer rating = 0;

    @Column
    private String tag;

    @Column
    private String brand;

    @Column(name = "location_of_clothing")
    private String locationOfClothing;

    @Column(name = "gender")
    private Boolean gender = false;

    @Column(name = "cover_image_id")
    private Long coverImageId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "clothingCard")
    @JsonManagedReference
    private java.util.List<ClothingImage> images = new java.util.ArrayList<>();

    public ClothingCard() {
    }

    public ClothingCard(User owner, String title, Integer cost, Integer deposit, String size, String description,
            LocalDateTime availabilityDay, String tag, String brand, String locationOfClothing,
            Integer rating, Boolean gender) {
        this.owner = owner;
        this.title = title;
        this.cost = cost;
        this.deposit = deposit;
        this.size = size;
        this.description = description;
        this.datePosted = LocalDateTime.now();
        this.availabilityDay = availabilityDay;
        this.tag = tag;
        this.brand = brand;
        this.locationOfClothing = locationOfClothing;
        this.rating = rating != null ? rating : 0;
        this.gender = gender != null ? gender : false;
    }

    // Getters and Setters

    public Long getClothingId() {
        return clothingId;
    }

    public void setClothingId(Long clothingId) {
        this.clothingId = clothingId;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    public Integer getDeposit() {
        return deposit;
    }

    public void setDeposit(Integer deposit) {
        this.deposit = deposit;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Boolean getAvailability() {
        return availability;
    }

    public void setAvailability(Boolean availability) {
        this.availability = availability;
    }

    public LocalDateTime getAvailabilityDay() {
        return availabilityDay;
    }

    public void setAvailabilityDay(LocalDateTime availabilityDay) {
        this.availabilityDay = availabilityDay;
    }

    public LocalDateTime getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(LocalDateTime datePosted) {
        this.datePosted = datePosted;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // no image getter/setter; handled by ClothingImage

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getLocationOfClothing() {
        return locationOfClothing;
    }

    public void setLocationOfClothing(String locationOfClothing) {
        this.locationOfClothing = locationOfClothing;
    }

    public Boolean getGender() {
        return gender;
    }

    public void setGender(Boolean gender) {
        this.gender = gender;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public java.util.List<ClothingImage> getImages() {
        return images;
    }

    public void setImages(java.util.List<ClothingImage> images) {
        this.images = images;
    }

    public Long getCoverImageId() {
        return coverImageId;
    }

    public void setCoverImageId(Long coverImageId) {
        this.coverImageId = coverImageId;
    }
}