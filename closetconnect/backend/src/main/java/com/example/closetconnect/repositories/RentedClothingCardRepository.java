package com.example.closetconnect.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.RentedClothingCard;
import com.example.closetconnect.entities.User;

@Repository
public interface RentedClothingCardRepository extends JpaRepository<RentedClothingCard, Long> {
    
    // Find all rental requests for a specific clothing card
    List<RentedClothingCard> findByClothingCardAndRequestTrue(ClothingCard clothingCard);
    
    // Find all rental requests by a specific user
    List<RentedClothingCard> findByRenterAndRequestTrue(User renter);
    
    // Find all rental requests received by a clothing card owner
    List<RentedClothingCard> findByClothingCardOwnerAndRequestTrue(User owner);
    
    // Check if a user has already requested a specific clothing card
    Optional<RentedClothingCard> findByClothingCardAndRenterAndRequestTrue(ClothingCard clothingCard, User renter);
    
    // Check if a user already has an active rental for a specific clothing card
    Optional<RentedClothingCard> findByClothingCardAndRenterAndRequestFalse(ClothingCard clothingCard, User renter);
    
    // Find all active rentals (not requests)
    List<RentedClothingCard> findByRequestFalse();
    
    // Find active rentals for a specific user
    List<RentedClothingCard> findByRenterAndRequestFalse(User renter);
    
    // Find active rentals for items owned by a specific user
    List<RentedClothingCard> findByClothingCardOwnerAndRequestFalse(User owner);
    
    // Find returned rentals for items owned by a specific user
    List<RentedClothingCard> findByClothingCardOwnerAndRequestFalseAndReturnedTrue(User owner);
    
    // Find active (not returned) rentals for items owned by a specific user
    List<RentedClothingCard> findByClothingCardOwnerAndRequestFalseAndReturnedFalse(User owner);
    
    // Check if a user has returned a rental for a specific clothing item (for review validation)
    boolean existsByClothingCardClothingIdAndRenterUserIdAndReturnedTrue(Long clothingId, Long renterId);
}
