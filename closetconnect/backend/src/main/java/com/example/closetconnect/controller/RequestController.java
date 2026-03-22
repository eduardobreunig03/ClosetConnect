package com.example.closetconnect.controller;

import com.example.closetconnect.entities.Request;
import com.example.closetconnect.services.RequestService;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "*")
public class RequestController {

    @Autowired
    private RequestService requestService;

    private String sanitize(String input) {
        return input == null ? null : Jsoup.clean(input, Safelist.basic());
    }

    // Create a new request
    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody CreateRequestDTO requestDTO) {
        System.out.println("Received DTO: " + requestDTO);
        System.out.println("StartDate (String): " + requestDTO.startDate());
        System.out.println("EndDate (String): " + requestDTO.endDate());
        System.out.println("AvailabilityRangeRequest (String): " + requestDTO.availabilityRangeRequest());

        try {
            // Parse the start date string
            LocalDateTime requestDate = null;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            // Try to parse startDate first
            if (requestDTO.startDate() != null && !requestDTO.startDate().isEmpty()) {
                try {
                    requestDate = LocalDateTime.parse(requestDTO.startDate(), formatter);
                    System.out.println("Parsed startDate: " + requestDate);
                } catch (DateTimeParseException e) {
                    System.out.println("Failed to parse startDate: " + e.getMessage());
                    // Try ISO format as fallback
                    try {
                        requestDate = LocalDateTime.parse(requestDTO.startDate());
                        System.out.println("Parsed startDate (ISO): " + requestDate);
                    } catch (DateTimeParseException e2) {
                        return ResponseEntity.badRequest()
                                .body(new ErrorResponse("Invalid start date format. Expected: yyyy-MM-ddTHH:mm:ss"));
                    }
                }
            }

            // Fallback to availabilityRangeRequest if startDate is not provided
            if (requestDate == null && requestDTO.availabilityRangeRequest() != null
                    && !requestDTO.availabilityRangeRequest().isEmpty()) {
                try {
                    requestDate = LocalDateTime.parse(requestDTO.availabilityRangeRequest(), formatter);
                    System.out.println("Parsed availabilityRangeRequest: " + requestDate);
                } catch (DateTimeParseException e) {
                    try {
                        requestDate = LocalDateTime.parse(requestDTO.availabilityRangeRequest());
                        System.out.println("Parsed availabilityRangeRequest (ISO): " + requestDate);
                    } catch (DateTimeParseException e2) {
                        return ResponseEntity.badRequest()
                                .body(new ErrorResponse("Invalid date format in availabilityRangeRequest"));
                    }
                }
            }

            // Validate that we have a start date
            if (requestDate == null) {
                System.out.println("ERROR: Request date is null after parsing!");
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Start date is required and cannot be null"));
            }

            // Parse and validate end date if provided
            LocalDateTime endDate = null;
            if (requestDTO.endDate() != null && !requestDTO.endDate().isEmpty()) {
                try {
                    endDate = LocalDateTime.parse(requestDTO.endDate(), formatter);
                } catch (DateTimeParseException e) {
                    try {
                        endDate = LocalDateTime.parse(requestDTO.endDate());
                    } catch (DateTimeParseException e2) {
                        return ResponseEntity.badRequest()
                                .body(new ErrorResponse("Invalid end date format. Expected: yyyy-MM-ddTHH:mm:ss"));
                    }
                }

                // Validate date range
                if (endDate.isBefore(requestDate)) {
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponse("End date must be after or equal to start date"));
                }
            }

            System.out.println("Using requestDate: " + requestDate);
            Request request = requestService.createRequest(
                    requestDTO.clothingId(),
                    requestDTO.fromUserId(),
                    requestDTO.toUserId(),
                    requestDate,
                    sanitize(requestDTO.requesterContactInfo()),
                    sanitize(requestDTO.commentsToOwner()));
            return ResponseEntity.status(HttpStatus.CREATED).body(request);
        } catch (Exception e) {
            System.out.println("Exception in createRequest: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Get all requests by a specific user (as requester)
    @GetMapping("/requester/{fromUserId}")
    public ResponseEntity<?> getRequestsByRequester(@PathVariable Long fromUserId) {
        try {
            List<Request> requests = requestService.getRequestsByFromUserId(fromUserId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Get all requests received by a clothing card owner
    @GetMapping("/owner/{toUserId}")
    public ResponseEntity<?> getRequestsByOwner(@PathVariable Long toUserId) {
        try {
            List<Request> requests = requestService.getRequestsByToUserId(toUserId);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Approve or reject a request
    @PutMapping("/{requestId}/approve")
    public ResponseEntity<?> updateRequestApproval(@PathVariable Long requestId, @RequestBody ApprovalDTO approvalDTO) {
        try {
            Request request = requestService.updateRequestApproval(requestId, approvalDTO.approved());
            // If rejected (false), request is deleted - return 204 No Content
            if (approvalDTO.approved() != null && !approvalDTO.approved()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(request);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Check if a user has already requested a specific clothing card
    @GetMapping("/check/{clothingId}/{fromUserId}")
    public ResponseEntity<?> checkExistingRequest(@PathVariable Long clothingId, @PathVariable Long fromUserId) {
        try {
            boolean hasRequested = requestService.hasUserRequestedClothing(clothingId, fromUserId);
            return ResponseEntity.ok(new CheckResponse(hasRequested));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // DTOs
    public record CreateRequestDTO(
            Long clothingId,
            Long fromUserId,
            Long toUserId,
            String availabilityRangeRequest, // Deprecated: use startDate instead - accept as String for parsing
            String startDate, // Accept as String and parse manually
            String endDate, // Accept as String and parse manually
            String requesterContactInfo,
            String commentsToOwner) {
    }

    public record ApprovalDTO(Boolean approved) {
    }

    public record ErrorResponse(String error) {
    }

    public record CheckResponse(Boolean hasRequested) {
    }
}