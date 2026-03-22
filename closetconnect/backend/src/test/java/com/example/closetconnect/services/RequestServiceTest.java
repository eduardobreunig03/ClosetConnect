package com.example.closetconnect.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.RentedClothingCard;
import com.example.closetconnect.entities.Request;
import com.example.closetconnect.entities.User;
import com.example.closetconnect.repositories.ClothingCardRepository;
import com.example.closetconnect.repositories.RentedClothingCardRepository;
import com.example.closetconnect.repositories.RequestRepository;
import com.example.closetconnect.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock private RequestRepository requestRepo;
    @Mock private RentedClothingCardRepository rentedRepo;
    @Mock private ClothingCardRepository clothingRepo;
    @Mock private UserRepository userRepo;

    @InjectMocks private RequestService service;

    // -----------------------
    // createRequest
    // -----------------------
    @Test
    @DisplayName("createRequest: throws if availabilityRangeRequest is null")
    void createRequest_nullAvailability_throws() {
        assertThatThrownBy(() ->
            service.createRequest(10L, 1L, 2L, null, "me@x.com", "pls"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("availabilityRangeRequest");
    }

    @Test
    @DisplayName("createRequest: sets approved=null and persists fields")
    void createRequest_setsApprovedNull_andPersists() {
        when(requestRepo.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime when = LocalDateTime.now().plusDays(3);
        var out = service.createRequest(10L, 1L, 2L, when, "me@x.com", "hi");

        ArgumentCaptor<Request> cap = ArgumentCaptor.forClass(Request.class);
        verify(requestRepo).save(cap.capture());
        Request saved = cap.getValue();

        assertThat(saved.getApproved()).isNull();
        assertThat(saved.getClothingId()).isEqualTo(10L);
        assertThat(saved.getFromUserId()).isEqualTo(1L);
        assertThat(saved.getToUserId()).isEqualTo(2L);
        assertThat(saved.getAvailabilityRangeRequest()).isEqualTo(when);

        // also returned object mirrors saved
        assertThat(out.getApproved()).isNull();
        assertThat(out.getAvailabilityRangeRequest()).isEqualTo(when);
    }

    // -----------------------
    // getters
    // -----------------------
    @Test
    @DisplayName("getRequestsByFromUserId: delegates to repo and returns")
    void getRequestsByFromUserId_ok() {
        var r1 = req(10L, 1L, 2L, LocalDateTime.now().plusDays(1));
        var r2 = req(11L, 1L, 3L, LocalDateTime.now().plusDays(2));
        when(requestRepo.findByFromUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(r1, r2));

        var out = service.getRequestsByFromUserId(1L);
        assertThat(out).containsExactly(r1, r2);
    }

    @Test
    @DisplayName("getRequestsByToUserId: filters out rejected (approved=false); keeps pending(null) and approved(true)")
    void getRequestsByToUserId_filtersRejected() {
        var rFalse = req(10L, 1L, 9L, LocalDateTime.now()); rFalse.setApproved(false);
        var rNull  = req(11L, 2L, 9L, LocalDateTime.now()); rNull.setApproved(null);
        var rTrue  = req(12L, 3L, 9L, LocalDateTime.now()); rTrue.setApproved(true);

        when(requestRepo.findByToUserIdOrderByCreatedAtDesc(9L)).thenReturn(List.of(rFalse, rNull, rTrue));

        var out = service.getRequestsByToUserId(9L);
        assertThat(out).containsExactly(rNull, rTrue);
    }

    // -----------------------
    // updateRequestApproval
    // -----------------------
    @Test
    @DisplayName("updateRequestApproval: reject(false) deletes request and returns deleted entity")
    void updateRequestApproval_reject_deletes() {
        var r = req(10L, 1L, 2L, LocalDateTime.now());
        when(requestRepo.findById(77L)).thenReturn(Optional.of(r));

        var out = service.updateRequestApproval(77L, false);

        verify(requestRepo).delete(r);
        verify(requestRepo, never()).save(any());
        assertThat(out).isSameAs(r);
    }

    @Test
    @DisplayName("updateRequestApproval: request not found -> throws")
    void updateRequestApproval_notFound_throws() {
        when(requestRepo.findById(123L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateRequestApproval(123L, true))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Request not found");
    }

    @Test
    @DisplayName("approve(true): creates rental, sets clothing unavailable, uses requested future return date")
    void approve_createsRental_futureReturn() {
        // request wanting future return date
        LocalDateTime requested = LocalDateTime.now().plusDays(5);
        var r = req(100L, 5L, 8L, requested);
        when(requestRepo.findById(77L)).thenReturn(Optional.of(r));
        when(requestRepo.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        // linked entities
        User renter = new User();
        ClothingCard card = new ClothingCard();
        card.setAvailability(true);

        when(userRepo.findById(5L)).thenReturn(Optional.of(renter));
        when(clothingRepo.findById(100L)).thenReturn(Optional.of(card));
        when(rentedRepo.findByClothingCardAndRenterAndRequestFalse(card, renter)).thenReturn(Optional.empty());
        when(rentedRepo.save(any(RentedClothingCard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clothingRepo.save(any(ClothingCard.class))).thenAnswer(inv -> inv.getArgument(0));

        var out = service.updateRequestApproval(77L, true);

        // request updated
        assertThat(out.getApproved()).isTrue();

        // rental created with expected fields
        ArgumentCaptor<RentedClothingCard> rentCap = ArgumentCaptor.forClass(RentedClothingCard.class);
        verify(rentedRepo).save(rentCap.capture());
        var savedRental = rentCap.getValue();
        assertThat(savedRental.getClothingCard()).isSameAs(card);
        assertThat(savedRental.getRenter()).isSameAs(renter);
        assertThat(savedRental.getReturned()).isFalse();
        assertThat(savedRental.getRequest()).isFalse();
        // return date should equal requested (future)
        assertThat(savedRental.getReturnDate()).isEqualTo(requested);
        // clothing made unavailable
        ArgumentCaptor<ClothingCard> cardCap = ArgumentCaptor.forClass(ClothingCard.class);
        verify(clothingRepo, atLeastOnce()).save(cardCap.capture());
        assertThat(cardCap.getValue().getAvailability()).isFalse();
    }

    @Test
    @DisplayName("approve(true): if rental already exists -> do NOT create duplicate, do NOT change availability")
    void approve_existingRental_noDuplicate() {
        LocalDateTime requested = LocalDateTime.now().plusDays(3);
        var r = req(100L, 5L, 8L, requested);
        when(requestRepo.findById(77L)).thenReturn(Optional.of(r));
        when(requestRepo.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        User renter = new User();
        ClothingCard card = new ClothingCard();
        card.setAvailability(true);

        when(userRepo.findById(5L)).thenReturn(Optional.of(renter));
        when(clothingRepo.findById(100L)).thenReturn(Optional.of(card));
        when(rentedRepo.findByClothingCardAndRenterAndRequestFalse(card, renter))
            .thenReturn(Optional.of(new RentedClothingCard()));

        var out = service.updateRequestApproval(77L, true);

        assertThat(out.getApproved()).isTrue();
        verify(rentedRepo, never()).save(any());
        verify(clothingRepo, never()).save(any(ClothingCard.class)); // availability unchanged
    }

    @Test
    @DisplayName("approve(true): requested return date in the past -> use rentalDate + 7 days")
    void approve_pastRequested_setsPlus7Days() {
        LocalDateTime requestedPast = LocalDateTime.now().minusDays(1);
        var r = req(100L, 5L, 8L, requestedPast);
        when(requestRepo.findById(77L)).thenReturn(Optional.of(r));
        when(requestRepo.save(any(Request.class))).thenAnswer(inv -> inv.getArgument(0));

        User renter = new User();
        ClothingCard card = new ClothingCard();
        card.setAvailability(true);

        when(userRepo.findById(5L)).thenReturn(Optional.of(renter));
        when(clothingRepo.findById(100L)).thenReturn(Optional.of(card));
        when(rentedRepo.findByClothingCardAndRenterAndRequestFalse(card, renter)).thenReturn(Optional.empty());
        when(rentedRepo.save(any(RentedClothingCard.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clothingRepo.save(any(ClothingCard.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateRequestApproval(77L, true);

        ArgumentCaptor<RentedClothingCard> rentCap = ArgumentCaptor.forClass(RentedClothingCard.class);
        verify(rentedRepo).save(rentCap.capture());
        var savedRental = rentCap.getValue();

        // Because both rentalDate and returnDate are generated inside the same method call,
        // they are consistent; assert exactly +7 days.
        assertThat(savedRental.getReturnDate()).isEqualTo(savedRental.getRentalDate().plusDays(7));
    }

    // -----------------------
    // hasUserRequestedClothing
    // -----------------------
    @Nested
    class HasUserRequestedClothing {
        @Test
        void present_true() {
            when(requestRepo.findByClothingIdAndFromUserId(10L, 1L))
                .thenReturn(Optional.of(req(10L, 1L, 2L, LocalDateTime.now())));
            assertThat(service.hasUserRequestedClothing(10L, 1L)).isTrue();
        }

        @Test
        void absent_false() {
            when(requestRepo.findByClothingIdAndFromUserId(10L, 1L))
                .thenReturn(Optional.empty());
            assertThat(service.hasUserRequestedClothing(10L, 1L)).isFalse();
        }
    }

    // -----------------------
    // helpers
    // -----------------------
    private static Request req(Long clothingId, Long fromUserId, Long toUserId, LocalDateTime when) {
        return new Request(clothingId, fromUserId, toUserId, when, "contact@x", "note");
    }
}
