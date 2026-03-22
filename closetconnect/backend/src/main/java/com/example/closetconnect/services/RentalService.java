package com.example.closetconnect.services;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.RentedClothingCard;
import com.example.closetconnect.entities.User;
import com.example.closetconnect.repositories.ClothingCardRepository;
import com.example.closetconnect.repositories.RentedClothingCardRepository;
import com.example.closetconnect.repositories.UserRepository;

@Service
public class RentalService {

    @Autowired
    private RentedClothingCardRepository rentedClothingCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClothingCardRepository clothingCardRepository;

    // Get all rental transactions (not requests) for a user as renter
    public List<RentedClothingCard> getRentalHistoryByRenter(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        // Get all rentals where request = false (actual rentals, not pending requests)
        return rentedClothingCardRepository.findByRenterAndRequestFalse(user.get());
    }

    // Get all rental transactions (not requests) for a user as owner
    public List<RentedClothingCard> getRentalHistoryByOwner(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        // Get all rentals for items owned by this user
        return rentedClothingCardRepository.findByClothingCardOwnerAndRequestFalse(user.get());
    }

    // Get active rentals (not returned) for items owned by a user
    public List<RentedClothingCard> getActiveRentalsByOwner(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        return rentedClothingCardRepository.findByClothingCardOwnerAndRequestFalseAndReturnedFalse(user.get());
    }

    // Get returned rentals (history) for items owned by a user
    public List<RentedClothingCard> getReturnedRentalsByOwner(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        return rentedClothingCardRepository.findByClothingCardOwnerAndRequestFalseAndReturnedTrue(user.get());
    }

    // Get all rental transactions for a user (both as renter and owner)
    public List<RentedClothingCard> getAllRentalHistory(Long userId) {
        List<RentedClothingCard> asRenter = getRentalHistoryByRenter(userId);
        List<RentedClothingCard> asOwner = getRentalHistoryByOwner(userId);
        
        // Combine and remove duplicates (in case of edge cases)
        return java.util.stream.Stream.concat(asRenter.stream(), asOwner.stream())
            .distinct()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // Most recent first
            .toList();
    }

    // Get a specific rental transaction by ID
    public Optional<RentedClothingCard> getRentalById(Long rentalId) {
        return rentedClothingCardRepository.findById(rentalId);
    }

    // Mark a rental as returned
    public RentedClothingCard markRentalAsReturned(Long rentalId, Long userId) {
        Optional<RentedClothingCard> rentalOpt = rentedClothingCardRepository.findById(rentalId);
        if (rentalOpt.isEmpty()) {
            throw new IllegalArgumentException("Rental not found");
        }

        RentedClothingCard rental = rentalOpt.get();
        
        // Verify that the user is the renter
        if (!rental.getRenter().getUserId().equals(userId)) {
            throw new IllegalArgumentException("You can only return items you rented");
        }

        // Check if already returned
        if (rental.getReturned()) {
            throw new IllegalArgumentException("This rental has already been returned");
        }

        // Mark as returned
        rental.setReturned(true);
        rental.setUpdatedAt(Instant.now());

        // Mark the clothing card as available again
        ClothingCard clothingCard = rental.getClothingCard();
        clothingCard.setAvailability(true);
        clothingCardRepository.save(clothingCard);

        return rentedClothingCardRepository.save(rental);
    }
}