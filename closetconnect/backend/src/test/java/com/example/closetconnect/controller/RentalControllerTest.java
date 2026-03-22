package com.example.closetconnect.controller;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.RentedClothingCard;
import com.example.closetconnect.entities.User;
import com.example.closetconnect.services.RentalService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RentalController.class)
@AutoConfigureMockMvc(addFilters = false)
class RentalControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;

    @MockBean private RentalService rentalService;

    // ---------- GET /api/rentals/renter/{userId} ----------
    @Test
    void getRentalHistoryByRenter_ok() throws Exception {
        when(rentalService.getRentalHistoryByRenter(2L))
                .thenReturn(List.of(rental(1L, false)));

        mvc.perform(get("/api/rentals/renter/2"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(1)))
           .andExpect(jsonPath("$[0].rentalId", is(1)));
    }

    @Test
    void getRentalHistoryByRenter_badRequest_onIllegalArg() throws Exception {
        when(rentalService.getRentalHistoryByRenter(2L))
                .thenThrow(new IllegalArgumentException("User not found"));

        mvc.perform(get("/api/rentals/renter/2"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error", is("User not found")));
    }

    @Test
    void getRentalHistoryByRenter_500_onException() throws Exception {
        when(rentalService.getRentalHistoryByRenter(2L))
                .thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/rentals/renter/2"))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.error", is("Failed to fetch rental history")));
    }

    // ---------- GET /api/rentals/owner/{userId} ----------
    @Test
    void getRentalHistoryByOwner_ok() throws Exception {
        when(rentalService.getRentalHistoryByOwner(9L))
                .thenReturn(List.of(rental(5L, true)));

        mvc.perform(get("/api/rentals/owner/9"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].rentalId", is(5)));
    }

    @Test
    void getRentalHistoryByOwner_badRequest() throws Exception {
        when(rentalService.getRentalHistoryByOwner(9L))
                .thenThrow(new IllegalArgumentException("User not found"));

        mvc.perform(get("/api/rentals/owner/9"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error", is("User not found")));
    }

    @Test
    void getRentalHistoryByOwner_500() throws Exception {
        when(rentalService.getRentalHistoryByOwner(9L))
                .thenThrow(new RuntimeException("x"));

        mvc.perform(get("/api/rentals/owner/9"))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.error", is("Failed to fetch rental history")));
    }

    // ---------- GET /api/rentals/owner/{userId}/active ----------
    @Test
    void getActiveRentalsByOwner_ok() throws Exception {
        when(rentalService.getActiveRentalsByOwner(7L))
                .thenReturn(List.of(rental(22L, false)));

        mvc.perform(get("/api/rentals/owner/7/active"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].rentalId", is(22)));
    }

    @Test
    void getActiveRentalsByOwner_badRequest() throws Exception {
        when(rentalService.getActiveRentalsByOwner(7L))
                .thenThrow(new IllegalArgumentException("User not found"));

        mvc.perform(get("/api/rentals/owner/7/active"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error", is("User not found")));
    }

    @Test
    void getActiveRentalsByOwner_500() throws Exception {
        when(rentalService.getActiveRentalsByOwner(7L))
                .thenThrow(new RuntimeException("err"));

        mvc.perform(get("/api/rentals/owner/7/active"))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.error", is("Failed to fetch active rentals")));
    }

    // ---------- GET /api/rentals/owner/{userId}/returned ----------
    @Test
    void getReturnedRentalsByOwner_ok() throws Exception {
        when(rentalService.getReturnedRentalsByOwner(7L))
                .thenReturn(List.of(rental(30L, true)));

        mvc.perform(get("/api/rentals/owner/7/returned"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].rentalId", is(30)));
    }

    @Test
    void getReturnedRentalsByOwner_badRequest() throws Exception {
        when(rentalService.getReturnedRentalsByOwner(7L))
                .thenThrow(new IllegalArgumentException("User not found"));

        mvc.perform(get("/api/rentals/owner/7/returned"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error", is("User not found")));
    }

    @Test
    void getReturnedRentalsByOwner_500() throws Exception {
        when(rentalService.getReturnedRentalsByOwner(7L))
                .thenThrow(new RuntimeException("err"));

        mvc.perform(get("/api/rentals/owner/7/returned"))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.error", is("Failed to fetch returned rentals")));
    }

    // ---------- GET /api/rentals/user/{userId} ----------
    @Test
    void getAllRentalHistory_ok() throws Exception {
        when(rentalService.getAllRentalHistory(4L))
                .thenReturn(List.of(rental(40L, true), rental(41L, false)));

        mvc.perform(get("/api/rentals/user/4"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)))
           .andExpect(jsonPath("$[0].rentalId", is(40)));
    }

    @Test
    void getAllRentalHistory_badRequest() throws Exception {
        when(rentalService.getAllRentalHistory(4L))
                .thenThrow(new IllegalArgumentException("User not found"));

        mvc.perform(get("/api/rentals/user/4"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error", is("User not found")));
    }

    @Test
    void getAllRentalHistory_500() throws Exception {
        when(rentalService.getAllRentalHistory(4L))
                .thenThrow(new RuntimeException("x"));

        mvc.perform(get("/api/rentals/user/4"))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.error", is("Failed to fetch rental history")));
    }

    // ---------- GET /api/rentals/{rentalId} ----------
    @Test
    void getRentalById_found() throws Exception {
        when(rentalService.getRentalById(100L))
                .thenReturn(Optional.of(rental(100L, false)));

        mvc.perform(get("/api/rentals/100"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.rentalId", is(100)))
           .andExpect(jsonPath("$.returned", is(false)));
    }

    @Test
    void getRentalById_notFound() throws Exception {
        when(rentalService.getRentalById(100L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/rentals/100"))
           .andExpect(status().isNotFound());
    }

    @Test
    void getRentalById_500() throws Exception {
        when(rentalService.getRentalById(100L)).thenThrow(new RuntimeException("x"));

        mvc.perform(get("/api/rentals/100"))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.error", is("Failed to fetch rental")));
    }

    // ---------- PUT /api/rentals/{rentalId}/return ----------
    @Test
    void markRentalAsReturned_ok() throws Exception {
        var req = new RentalController.ReturnRentalRequest(2L);
        when(rentalService.markRentalAsReturned(77L, 2L))
                .thenReturn(rental(77L, true));

        mvc.perform(put("/api/rentals/77/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.rentalId", is(77)))
           .andExpect(jsonPath("$.returned", is(true)));
    }

    @Test
    void markRentalAsReturned_badRequest_onIllegalArg() throws Exception {
        var req = new RentalController.ReturnRentalRequest(2L);
        when(rentalService.markRentalAsReturned(77L, 2L))
                .thenThrow(new IllegalArgumentException("This rental has already been returned"));

        mvc.perform(put("/api/rentals/77/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.error", is("This rental has already been returned")));
    }

    @Test
    void markRentalAsReturned_500_onException() throws Exception {
        var req = new RentalController.ReturnRentalRequest(2L);
        when(rentalService.markRentalAsReturned(77L, 2L))
                .thenThrow(new RuntimeException("db err"));

        mvc.perform(put("/api/rentals/77/return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.error", is("Failed to mark rental as returned")));
    }

    // -------- helpers --------
    private static RentedClothingCard rental(Long id, boolean returned) {
        RentedClothingCard r = new RentedClothingCard();
        r.setRentalId(id);
        r.setRentalDate(LocalDateTime.now().minusDays(2));
        r.setReturnDate(LocalDateTime.now().plusDays(5));
        r.setReturned(returned);
        r.setRequest(false);
        r.setCreatedAt(Instant.now());
        r.setUpdatedAt(Instant.now());

        ClothingCard card = new ClothingCard();
        setField(card, "clothingId", 10L);
        r.setClothingCard(card);

        User u = new User();
        setField(u, "userId", 2L);
        u.setEmail("user@example.com");
        u.setUserName("User");
        r.setRenter(u);
        return r;
    }

    private static void setField(Object target, String name, Object value) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception ignored) {}
    }
}
