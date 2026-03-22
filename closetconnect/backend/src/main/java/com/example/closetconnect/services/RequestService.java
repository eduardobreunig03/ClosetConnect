package com.example.closetconnect.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.RentedClothingCard;
import com.example.closetconnect.entities.Request;
import com.example.closetconnect.entities.User;
import com.example.closetconnect.repositories.ClothingCardRepository;
import com.example.closetconnect.repositories.RentedClothingCardRepository;
import com.example.closetconnect.repositories.RequestRepository;
import com.example.closetconnect.repositories.UserRepository;

@Service
public class RequestService {

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private RentedClothingCardRepository rentedClothingCardRepository;

    @Autowired
    private ClothingCardRepository clothingCardRepository;

    @Autowired
    private UserRepository userRepository;

    // Create a new request
    public Request createRequest(Long clothingId, Long fromUserId, Long toUserId, 
                                LocalDateTime availabilityRangeRequest, String requesterContactInfo, 
                                String commentsToOwner) {
        // Validate that availabilityRangeRequest is not null
        if (availabilityRangeRequest == null) {
            throw new IllegalArgumentException("availabilityRangeRequest cannot be null");
        }
        
        Request request = new Request(clothingId, fromUserId, toUserId, 
                                    availabilityRangeRequest, requesterContactInfo, commentsToOwner);
        // Explicitly ensure approved is null (pending) for new requests
        request.setApproved(null);
        
        // Double-check that availabilityRangeRequest is set before saving
        if (request.getAvailabilityRangeRequest() == null) {
            throw new IllegalStateException("availabilityRangeRequest is null after creating Request entity");
        }
        
        return requestRepository.save(request);
    }


    // Get all requests by a specific user (as requester)
    public List<Request> getRequestsByFromUserId(Long fromUserId) {
        return requestRepository.findByFromUserIdOrderByCreatedAtDesc(fromUserId);
    }

    // Get all requests received by a clothing card owner
    // Show pending (null) and approved (true) requests, but filter out rejected (false) requests
    // Note: In normal operation, approved/rejected requests are deleted, but this method handles edge cases
    public List<Request> getRequestsByToUserId(Long toUserId) {
        List<Request> allRequests = requestRepository.findByToUserIdOrderByCreatedAtDesc(toUserId);
        // Filter out rejected requests (approved=false), keep pending (null) and approved (true)
        return allRequests.stream()
            .filter(request -> request.getApproved() == null || Boolean.TRUE.equals(request.getApproved()))
            .toList();
    }


    // Approve or reject a request
    @Transactional
    public Request updateRequestApproval(Long requestId, Boolean approved) {
        Optional<Request> requestOpt = requestRepository.findById(requestId);
        if (requestOpt.isPresent()) {
            Request request = requestOpt.get();
            
            // If rejecting, delete the request from database (removed from owner's view)
            // Note: This means requesters won't see rejected requests either
            if (approved != null && !approved) {
                requestRepository.delete(request);
                // Return the deleted request for confirmation (though it's deleted from DB)
                return request;
            }
            
            // If approving, create rental and then delete the request
            // Approved requests are deleted because they become rented cards which provide sufficient history
            if (approved != null && approved) {
                request.setApproved(approved);
                Request savedRequest = requestRepository.save(request);
                
                // Create a RentedClothingCard from the approved request
                createRentedClothingCardFromRequest(savedRequest);
                
                // Delete the request after successfully creating the rental
                // The rented card provides all necessary history
                requestRepository.delete(savedRequest);
                
                // Return the deleted request for confirmation (though it's deleted from DB)
                return savedRequest;
            }
            
            return request;
        }
        throw new RuntimeException("Request not found with id: " + requestId);
    }

    // Create a RentedClothingCard from an approved request
    private void createRentedClothingCardFromRequest(Request request) {
        // Check if rental already exists for this request
        Optional<ClothingCard> clothingCardOpt = clothingCardRepository.findById(request.getClothingId());
        Optional<User> renterOpt = userRepository.findById(request.getFromUserId());
        
        if (clothingCardOpt.isEmpty()) {
            throw new RuntimeException("Clothing card not found with id: " + request.getClothingId());
        }
        if (renterOpt.isEmpty()) {
            throw new RuntimeException("User (renter) not found with id: " + request.getFromUserId());
        }
        
        ClothingCard clothingCard = clothingCardOpt.get();
        User renter = renterOpt.get();
        
        // Check if rental already exists for this clothing card and renter
        Optional<RentedClothingCard> existingRental = rentedClothingCardRepository
            .findByClothingCardAndRenterAndRequestFalse(clothingCard, renter);
        
        if (existingRental.isPresent()) {
            // Rental already exists, don't create duplicate
            return;
        }
        
        // Use availabilityRangeRequest as the return date (the date requested in the rental request)
        LocalDateTime rentalDate = LocalDateTime.now();
        LocalDateTime requestedReturnDate = request.getAvailabilityRangeRequest();
        
        // Ensure return_date is after rental_date (database constraint requirement)
        LocalDateTime returnDate;
        if (requestedReturnDate.isBefore(rentalDate) || requestedReturnDate.isEqual(rentalDate)) {
            // If requested return date is in the past or today, set it to 7 days from now
            returnDate = rentalDate.plusDays(7);
        } else {
            // Use the requested return date if it's in the future
            returnDate = requestedReturnDate;
        }
        
        // Create the rental record
        RentedClothingCard rentedClothingCard = new RentedClothingCard();
        rentedClothingCard.setClothingCard(clothingCard);
        rentedClothingCard.setRenter(renter);
        rentedClothingCard.setRentalDate(rentalDate);
        rentedClothingCard.setReturnDate(returnDate);
        rentedClothingCard.setReturned(false);
        rentedClothingCard.setRequest(false); // This is an actual rental, not a request
        
        rentedClothingCardRepository.save(rentedClothingCard);
        
        // Mark the clothing card as unavailable
        clothingCard.setAvailability(false);
        clothingCardRepository.save(clothingCard);
    }

    // Check if a user has already requested a specific clothing card
    public boolean hasUserRequestedClothing(Long clothingId, Long fromUserId) {
        return requestRepository.findByClothingIdAndFromUserId(clothingId, fromUserId).isPresent();
    }

}