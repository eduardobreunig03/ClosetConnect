package com.example.closetconnect.services;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.RentedClothingCard;
import com.example.closetconnect.entities.User;
import com.example.closetconnect.repositories.ClothingCardRepository;
import com.example.closetconnect.repositories.RentedClothingCardRepository;
import com.example.closetconnect.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock RentedClothingCardRepository rentedRepo;
    @Mock UserRepository userRepo;
    @Mock ClothingCardRepository clothingRepo;

    @InjectMocks RentalService service;

    private User user(long id) {
        User u = new User();
        u.setUserId(id);
        return u;
    }

    private ClothingCard card(long id, User owner, boolean available) {
        ClothingCard c = new ClothingCard();
        c.setClothingId(id);
        c.setOwner(owner);
        c.setAvailability(available);
        return c;
    }

    private RentedClothingCard rental(User renter, ClothingCard card, boolean returned, Instant createdAt) {
        RentedClothingCard r = new RentedClothingCard();
        r.setRenter(renter);
        r.setClothingCard(card);
        r.setReturned(returned);
        r.setCreatedAt(createdAt);
        r.setRequest(false);
        return r;
    }

    // ---------- getRentalHistoryByRenter ----------

    @Test
    @DisplayName("getRentalHistoryByRenter: throws when user not found")
    void getRentalHistoryByRenter_userNotFound() {
        when(userRepo.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getRentalHistoryByRenter(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
        verifyNoInteractions(rentedRepo);
    }

    @Test
    @DisplayName("getRentalHistoryByRenter: returns repository result")
    void getRentalHistoryByRenter_ok() {
        User u = user(1L);
        when(userRepo.findById(1L)).thenReturn(Optional.of(u));
        var r1 = new RentedClothingCard();
        when(rentedRepo.findByRenterAndRequestFalse(u)).thenReturn(List.of(r1));

        var out = service.getRentalHistoryByRenter(1L);
        assertThat(out).containsExactly(r1);
    }

    // ---------- getRentalHistoryByOwner / active / returned ----------

    @Test
    @DisplayName("getRentalHistoryByOwner: throws when user not found")
    void getRentalHistoryByOwner_userNotFound() {
        when(userRepo.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getRentalHistoryByOwner(7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    @DisplayName("getRentalHistoryByOwner: returns repository result")
    void getRentalHistoryByOwner_ok() {
        User owner = user(2L);
        when(userRepo.findById(2L)).thenReturn(Optional.of(owner));
        var r = new RentedClothingCard();
        when(rentedRepo.findByClothingCardOwnerAndRequestFalse(owner)).thenReturn(List.of(r));

        var out = service.getRentalHistoryByOwner(2L);
        assertThat(out).containsExactly(r);
    }

    @Test
    @DisplayName("getActiveRentalsByOwner: returns repository result")
    void getActiveRentalsByOwner_ok() {
        User owner = user(5L);
        when(userRepo.findById(5L)).thenReturn(Optional.of(owner));
        var r = new RentedClothingCard();
        when(rentedRepo.findByClothingCardOwnerAndRequestFalseAndReturnedFalse(owner)).thenReturn(List.of(r));

        var out = service.getActiveRentalsByOwner(5L);
        assertThat(out).containsExactly(r);
    }

    @Test
    @DisplayName("getReturnedRentalsByOwner: returns repository result")
    void getReturnedRentalsByOwner_ok() {
        User owner = user(6L);
        when(userRepo.findById(6L)).thenReturn(Optional.of(owner));
        var r = new RentedClothingCard();
        when(rentedRepo.findByClothingCardOwnerAndRequestFalseAndReturnedTrue(owner)).thenReturn(List.of(r));

        var out = service.getReturnedRentalsByOwner(6L);
        assertThat(out).containsExactly(r);
    }

    // ---------- getAllRentalHistory (combine + distinct + sort desc by createdAt) ----------

 @Test
    @DisplayName("getAllRentalHistory: merges renter/owner lists, de-duplicates same instance, sorts by createdAt desc")
    void getAllRentalHistory_mergesDistinctSorts() {
        // user returned by UserRepository (id value not needed here)
        User u = user();
        when(userRepo.findById(9L)).thenReturn(Optional.of(u)); // called twice inside service

        Instant t1 = Instant.parse("2025-05-10T10:00:00Z");
        Instant t2 = Instant.parse("2025-09-01T12:00:00Z"); // latest
        Instant t3 = Instant.parse("2024-12-31T23:59:59Z"); // oldest

        var owner  = u;
        var renter = u;

        var cardA = card(owner, false);
        var cardB = card(owner, false); // <-- fixed (was 'car' and had a space before '(')

        var r1 = rental(renter, cardA, false, t1);
        var r2 = rental(renter, cardB, false, t2);
        var r3 = rental(renter, cardA, true,  t3);

        when(rentedRepo.findByRenterAndRequestFalse(u)).thenReturn(List.of(r1, r2));
        // include the SAME instance r2 in the owner list to exercise distinct()
        when(rentedRepo.findByClothingCardOwnerAndRequestFalse(u)).thenReturn(List.of(r2, r3));

        var out = service.getAllRentalHistory(9L);

        // must be sorted by createdAt desc and de-duplicated
        assertThat(out).containsExactly(r2, r1, r3);
    }

    // -----------------
    // helpers (no ids needed)
    // -----------------
    private static User user() {
        return new User();
    }

    private static ClothingCard card(User owner, boolean available) {
        ClothingCard c = new ClothingCard();
        c.setOwner(owner);
        c.setAvailability(available);
        return c;
    }

    @Test
    @DisplayName("getRentalById: returns Optional from repository")
    void getRentalById_ok() {
        var r = new RentedClothingCard();
        when(rentedRepo.findById(77L)).thenReturn(Optional.of(r));

        var out = service.getRentalById(77L);
        assertThat(out).containsSame(r);
    }

    // ---------- markRentalAsReturned ----------

    @Nested
    class MarkReturned {

        User renter, owner;
        ClothingCard c;
        RentedClothingCard r;

        @BeforeEach
        void setup() {
            renter = user(22L);
            owner  = user(33L);
            c = card(500L, owner, false);
            r = rental(renter, c, false, Instant.parse("2025-08-01T00:00:00Z"));
            r.setUpdatedAt(null);
        }

        @Test
        @DisplayName("throws when rental not found")
        void notFound() {
            when(rentedRepo.findById(900L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.markRentalAsReturned(900L, 22L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Rental not found");
        }

        @Test
        @DisplayName("throws when caller is not the renter")
        void wrongUser() {
            when(rentedRepo.findById(900L)).thenReturn(Optional.of(r));
            assertThatThrownBy(() -> service.markRentalAsReturned(900L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("You can only return items you rented");
        }

        @Test
        @DisplayName("throws when already returned")
        void alreadyReturned() {
            r.setReturned(true);
            when(rentedRepo.findById(900L)).thenReturn(Optional.of(r));
            assertThatThrownBy(() -> service.markRentalAsReturned(900L, 22L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("This rental has already been returned");
        }

        @Test
        @DisplayName("success: sets returned=true, flips card availability, updates timestamps, saves both")
        void success() {
            when(rentedRepo.findById(900L)).thenReturn(Optional.of(r));

            // Return the same objects on save
            when(clothingRepo.save(any(ClothingCard.class))).thenAnswer(inv -> inv.getArgument(0));
            when(rentedRepo.save(any(RentedClothingCard.class))).thenAnswer(inv -> inv.getArgument(0));

            Instant before = Instant.now();

            var out = service.markRentalAsReturned(900L, 22L);

            // assertions on returned entity
            assertThat(out.getReturned()).isTrue();
            assertThat(out.getUpdatedAt()).isNotNull();
            assertThat(out.getUpdatedAt()).isAfterOrEqualTo(before);

            // card availability flipped and saved
            assertThat(c.getAvailability()).isTrue();
            verify(clothingRepo).save(c);

            // rental saved
            ArgumentCaptor<RentedClothingCard> cap = ArgumentCaptor.forClass(RentedClothingCard.class);
            verify(rentedRepo).save(cap.capture());
            assertThat(cap.getValue().getReturned()).isTrue();
        }
    }
}
