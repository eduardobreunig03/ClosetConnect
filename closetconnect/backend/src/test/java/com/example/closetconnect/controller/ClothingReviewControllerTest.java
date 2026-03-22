package com.example.closetconnect.controller;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.ClothingReview;
import com.example.closetconnect.entities.User;
import com.example.closetconnect.services.ClothingReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClothingReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClothingReviewControllerTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;

    @MockBean private ClothingReviewService clothingReviewService;

    // ---------- GET /clothing/{clothingId} ----------
    @Test
    void getReviewsByClothingId_ok() throws Exception {
        when(clothingReviewService.getReviewsByClothingId(7L))
                .thenReturn(List.of(review(1L, 5, "Great"), review(2L, 4, "Nice")));

        mvc.perform(get("/api/clothing-reviews/clothing/7"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)))
           .andExpect(jsonPath("$[0].reviewId", is(1)))
           .andExpect(jsonPath("$[0].review", is(5)))
           .andExpect(jsonPath("$[0].reviewText", is("Great")));
    }

    @Test
    void getReviewsByClothingId_500_onException() throws Exception {
        when(clothingReviewService.getReviewsByClothingId(7L))
                .thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/clothing-reviews/clothing/7"))
           .andExpect(status().isInternalServerError());
    }

    // ---------- GET /user/{userId} ----------
    @Test
    void getReviewsByUserId_ok() throws Exception {
        when(clothingReviewService.getReviewsByUserId(3L))
                .thenReturn(List.of(review(9L, 3, "ok")));

        mvc.perform(get("/api/clothing-reviews/user/3"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].reviewId", is(9)))
           .andExpect(jsonPath("$[0].review", is(3)));
    }

    @Test
    void getReviewsByUserId_500_onException() throws Exception {
        when(clothingReviewService.getReviewsByUserId(3L))
                .thenThrow(new RuntimeException("x"));

        mvc.perform(get("/api/clothing-reviews/user/3"))
           .andExpect(status().isInternalServerError());
    }

    // ---------- GET /{reviewId} ----------
    @Test
    void getReviewById_found() throws Exception {
        when(clothingReviewService.getReviewById(11L))
                .thenReturn(Optional.of(review(11L, 4, "fine")));

        mvc.perform(get("/api/clothing-reviews/11"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.reviewId", is(11)))
           .andExpect(jsonPath("$.review", is(4)))
           .andExpect(jsonPath("$.reviewText", is("fine")));
    }

    @Test
    void getReviewById_notFound() throws Exception {
        when(clothingReviewService.getReviewById(11L))
                .thenReturn(Optional.empty());

        mvc.perform(get("/api/clothing-reviews/11"))
           .andExpect(status().isNotFound());
    }

    @Test
    void getReviewById_500_onException() throws Exception {
        when(clothingReviewService.getReviewById(11L))
                .thenThrow(new RuntimeException("x"));

        mvc.perform(get("/api/clothing-reviews/11"))
           .andExpect(status().isInternalServerError());
    }

    // ---------- POST (create) ----------
    @Test
    void createReview_created() throws Exception {
        var req = new ClothingReviewController.CreateReviewRequest();
        req.setClothingId(5L);
        req.setReviewerId(2L);
        req.setRating(5);
        req.setReviewText("A+");

        when(clothingReviewService.createReview(5L, 2L, 5, "A+"))
                .thenReturn(review(100L, 5, "A+"));

        mvc.perform(post("/api/clothing-reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.reviewId", is(100)))
           .andExpect(jsonPath("$.review", is(5)))
           .andExpect(jsonPath("$.reviewText", is("A+")));
    }

    @Test
    void createReview_badRequest_onIllegalArgument() throws Exception {
        var req = new ClothingReviewController.CreateReviewRequest();
        req.setClothingId(5L); req.setReviewerId(2L); req.setRating(6); req.setReviewText("too high");

        when(clothingReviewService.createReview(eq(5L), eq(2L), eq(6), eq("too high")))
                .thenThrow(new IllegalArgumentException("Rating must be between 1 and 5"));

        mvc.perform(post("/api/clothing-reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message", is("Rating must be between 1 and 5")));
    }

    @Test
    void createReview_500_onOtherException() throws Exception {
        var req = new ClothingReviewController.CreateReviewRequest();
        req.setClothingId(5L); req.setReviewerId(2L); req.setRating(4); req.setReviewText("ok");

        when(clothingReviewService.createReview(5L, 2L, 4, "ok"))
                .thenThrow(new RuntimeException("db down"));

        mvc.perform(post("/api/clothing-reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.message", is("Failed to create review")));
    }

    // ---------- PUT (update) ----------
    @Test
    void updateReview_ok() throws Exception {
        var req = new ClothingReviewController.UpdateReviewRequest();
        req.setReviewerId(2L);
        req.setRating(4);
        req.setReviewText("updated");

        when(clothingReviewService.updateReview(9L, 2L, 4, "updated"))
                .thenReturn(review(9L, 4, "updated"));

        mvc.perform(put("/api/clothing-reviews/9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.reviewId", is(9)))
           .andExpect(jsonPath("$.review", is(4)))
           .andExpect(jsonPath("$.reviewText", is("updated")));
    }

    @Test
    void updateReview_badRequest_onIllegalArgument() throws Exception {
        var req = new ClothingReviewController.UpdateReviewRequest();
        req.setReviewerId(2L);
        req.setRating(7); // invalid
        req.setReviewText("bad");

        when(clothingReviewService.updateReview(9L, 2L, 7, "bad"))
                .thenThrow(new IllegalArgumentException("Rating must be between 1 and 5"));

        mvc.perform(put("/api/clothing-reviews/9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message", is("Rating must be between 1 and 5")));
    }

    @Test
    void updateReview_500_onException() throws Exception {
        var req = new ClothingReviewController.UpdateReviewRequest();
        req.setReviewerId(2L);
        req.setRating(3);
        req.setReviewText("ok");

        when(clothingReviewService.updateReview(9L, 2L, 3, "ok"))
                .thenThrow(new RuntimeException("x"));

        mvc.perform(put("/api/clothing-reviews/9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.message", is("Failed to update review")));
    }

    // ---------- DELETE /{reviewId}?reviewerId=... ----------
    @Test
    void deleteReview_ok() throws Exception {
        mvc.perform(delete("/api/clothing-reviews/44").param("reviewerId", "2"))
           .andExpect(status().isOk());
        verify(clothingReviewService).deleteReview(44L, 2L);
    }

    @Test
    void deleteReview_badRequest_onIllegalArgument() throws Exception {
        doThrow(new IllegalArgumentException("You can only delete your own reviews"))
                .when(clothingReviewService).deleteReview(44L, 2L);

        mvc.perform(delete("/api/clothing-reviews/44").param("reviewerId", "2"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message", is("You can only delete your own reviews")));
    }

    @Test
    void deleteReview_500_onException() throws Exception {
        doThrow(new RuntimeException("db err"))
                .when(clothingReviewService).deleteReview(44L, 2L);

        mvc.perform(delete("/api/clothing-reviews/44").param("reviewerId", "2"))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.message", is("Failed to delete review")));
    }

    // ---------- GET /can-review ----------
    @Test
    void canUserReview_ok() throws Exception {
        when(clothingReviewService.canUserReview(5L, 2L)).thenReturn(true);

        mvc.perform(get("/api/clothing-reviews/can-review")
                .param("clothingId", "5")
                .param("userId", "2"))
           .andExpect(status().isOk())
           .andExpect(content().string("true"));
    }

    @Test
    void canUserReview_500_onException() throws Exception {
        when(clothingReviewService.canUserReview(anyLong(), anyLong()))
                .thenThrow(new RuntimeException("x"));

        mvc.perform(get("/api/clothing-reviews/can-review")
                .param("clothingId", "5")
                .param("userId", "2"))
           .andExpect(status().isInternalServerError());
    }

    // ---------- GET /clothing/{clothingId}/average-rating ----------
    @Test
    void getAverageRating_ok() throws Exception {
        when(clothingReviewService.getAverageRating(7L)).thenReturn(4.5);

        mvc.perform(get("/api/clothing-reviews/clothing/7/average-rating"))
           .andExpect(status().isOk())
           .andExpect(content().string("4.5"));
    }

    @Test
    void getAverageRating_500_onException() throws Exception {
        when(clothingReviewService.getAverageRating(7L)).thenThrow(new RuntimeException("x"));

        mvc.perform(get("/api/clothing-reviews/clothing/7/average-rating"))
           .andExpect(status().isInternalServerError());
    }

    // ---------- GET /clothing/{clothingId}/count ----------
    @Test
    void getReviewCount_ok() throws Exception {
        when(clothingReviewService.getReviewCount(7L)).thenReturn(3L);

        mvc.perform(get("/api/clothing-reviews/clothing/7/count"))
           .andExpect(status().isOk())
           .andExpect(content().string("3"));
    }

    @Test
    void getReviewCount_500_onException() throws Exception {
        when(clothingReviewService.getReviewCount(7L)).thenThrow(new RuntimeException("x"));

        mvc.perform(get("/api/clothing-reviews/clothing/7/count"))
           .andExpect(status().isInternalServerError());
    }

    // ------- helpers -------
    private static ClothingReview review(Long id, int rating, String text) {
        ClothingReview r = new ClothingReview();
        setField(r, "reviewId", id);
        r.setReview(rating);
        r.setReviewText(text);
        r.setCreatedAt(Instant.now());
        r.setUpdatedAt(Instant.now());
        // include minimal relations to satisfy JSON (backref on clothingCard avoids recursion)
        ClothingCard card = new ClothingCard();
        setField(card, "clothingId", 123L);
        r.setClothingCard(card);

        User u = new User();
        setField(u, "userId", 55L);
        u.setEmail("user@example.com");
        u.setUserName("User");
        r.setReviewer(u);
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
