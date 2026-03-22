package com.example.closetconnect.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.closetconnect.entities.RentedClothingCard;
import com.example.closetconnect.services.RentalService;

@RestController
@RequestMapping("/api/rentals")
@CrossOrigin(origins = "http://localhost:3000")
public class RentalController {

    @Autowired
    private RentalService rentalService;

    // Get all rental history for a user (as renter)
    @GetMapping("/renter/{userId}")
    public ResponseEntity<?> getRentalHistoryByRenter(@PathVariable Long userId) {
        try {
            List<RentedClothingCard> rentals = rentalService.getRentalHistoryByRenter(userId);
            return ResponseEntity.ok(rentals);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to fetch rental history"));
        }
    }

    // Get all rental history for a user (as owner)
    @GetMapping("/owner/{userId}")
    public ResponseEntity<?> getRentalHistoryByOwner(@PathVariable Long userId) {
        try {
            List<RentedClothingCard> rentals = rentalService.getRentalHistoryByOwner(userId);
            return ResponseEntity.ok(rentals);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to fetch rental history"));
        }
    }

    // Get active rentals for items owned by a user
    @GetMapping("/owner/{userId}/active")
    public ResponseEntity<?> getActiveRentalsByOwner(@PathVariable Long userId) {
        try {
            List<RentedClothingCard> rentals = rentalService.getActiveRentalsByOwner(userId);
            return ResponseEntity.ok(rentals);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to fetch active rentals"));
        }
    }

    // Get returned rentals (history) for items owned by a user
    @GetMapping("/owner/{userId}/returned")
    public ResponseEntity<?> getReturnedRentalsByOwner(@PathVariable Long userId) {
        try {
            List<RentedClothingCard> rentals = rentalService.getReturnedRentalsByOwner(userId);
            return ResponseEntity.ok(rentals);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to fetch returned rentals"));
        }
    }

    // Get all rental history for a user (both as renter and owner)
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getAllRentalHistory(@PathVariable Long userId) {
        try {
            List<RentedClothingCard> rentals = rentalService.getAllRentalHistory(userId);
            return ResponseEntity.ok(rentals);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to fetch rental history"));
        }
    }

    // Get a specific rental transaction by ID
    @GetMapping("/{rentalId}")
    public ResponseEntity<?> getRentalById(@PathVariable Long rentalId) {
        try {
            Optional<RentedClothingCard> rental = rentalService.getRentalById(rentalId);
            if (rental.isPresent()) {
                return ResponseEntity.ok(rental.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to fetch rental"));
        }
    }

    // Mark a rental as returned
    @PutMapping("/{rentalId}/return")
    public ResponseEntity<?> markRentalAsReturned(
            @PathVariable Long rentalId,
            @RequestBody ReturnRentalRequest request) {
        try {
            RentedClothingCard rental = rentalService.markRentalAsReturned(rentalId, request.userId());
            return ResponseEntity.ok(rental);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Failed to mark rental as returned"));
        }
    }

    public record ErrorResponse(String error) {}
    public record ReturnRentalRequest(Long userId) {}
}

