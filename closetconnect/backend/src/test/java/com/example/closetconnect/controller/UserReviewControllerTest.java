package com.example.closetconnect.controller;

import com.example.closetconnect.entities.UserReview;
import com.example.closetconnect.services.UserReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserReviewController.class)
@AutoConfigureMockMvc(addFilters = false) // skip security filters for slice tests
class UserReviewControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserReviewService userReviewService;

    // ---- GETs ----

    @Test
    @DisplayName("GET /api/user-reviews/user/{userId} -> 200 with list")
    void getReviewsForUser_ok() throws Exception {
        Mockito.when(userReviewService.getReviewsForUser(1L))
               .thenReturn(List.of(new UserReview(), new UserReview()));

        mvc.perform(get("/api/user-reviews/user/{userId}", 1L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/user-reviews/by/{userId} -> 200 with list")
    void getReviewsByUser_ok() throws Exception {
        Mockito.when(userReviewService.getReviewsByUser(2L))
               .thenReturn(List.of(new UserReview()));

        mvc.perform(get("/api/user-reviews/by/{userId}", 2L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/user-reviews/{id} -> 200 when found")
    void getReviewById_found() throws Exception {
        Mockito.when(userReviewService.getReviewById(10L))
               .thenReturn(Optional.of(new UserReview()));

        mvc.perform(get("/api/user-reviews/{id}", 10L))
           .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/user-reviews/{id} -> 404 when not found")
    void getReviewById_notFound() throws Exception {
        Mockito.when(userReviewService.getReviewById(99L))
               .thenReturn(Optional.empty());

        mvc.perform(get("/api/user-reviews/{id}", 99L))
           .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/user-reviews/rental/{rentalId} -> 200 with list")
    void getReviewsByRental_ok() throws Exception {
        Mockito.when(userReviewService.getReviewsByRentalId(7L))
               .thenReturn(List.of(new UserReview(), new UserReview(), new UserReview()));

        mvc.perform(get("/api/user-reviews/rental/{rentalId}", 7L))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(3)));
    }

    // ---- POST create ----

    @Test
    @DisplayName("POST /api/user-reviews -> 201 when service succeeds")
    void createReview_created() throws Exception {
        var req = new UserReviewController.CreateUserReviewRequest();
        req.setRentalId(5L);
        req.setReviewedUserId(11L);
        req.setReviewerId(22L);
        req.setRating(5);
        req.setReviewText("Great!");

        Mockito.when(userReviewService.createReview(5L, 11L, 22L, 5, "Great!"))
               .thenReturn(new UserReview());

        mvc.perform(post("/api/user-reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
           .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/user-reviews -> 400 on validation (IllegalArgumentException)")
    void createReview_badRequest() throws Exception {
        var req = new UserReviewController.CreateUserReviewRequest();
        req.setRentalId(5L);
        req.setReviewedUserId(11L);
        req.setReviewerId(22L);
        req.setRating(6); // invalid rating, for example

        Mockito.when(userReviewService.createReview(anyLong(), anyLong(), anyLong(), anyInt(), any()))
               .thenThrow(new IllegalArgumentException("Rating must be between 1 and 5"));

        mvc.perform(post("/api/user-reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message").value("Rating must be between 1 and 5"));
    }

    @Test
    @DisplayName("POST /api/user-reviews -> 500 on unexpected exception")
    void createReview_serverError() throws Exception {
        var req = new UserReviewController.CreateUserReviewRequest();
        req.setRentalId(5L);
        req.setReviewedUserId(11L);
        req.setReviewerId(22L);
        req.setRating(4);

        Mockito.when(userReviewService.createReview(anyLong(), anyLong(), anyLong(), anyInt(), any()))
               .thenThrow(new RuntimeException("DB down"));

        mvc.perform(post("/api/user-reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
           .andExpect(status().isInternalServerError())
           .andExpect(jsonPath("$.message").value("Failed to create review: DB down"));
    }

    // ---- PUT update ----

    @Test
    @DisplayName("PUT /api/user-reviews/{id} -> 200 when service succeeds")
    void updateReview_ok() throws Exception {
        var req = new UserReviewController.UpdateUserReviewRequest();
        req.setReviewerId(22L);
        req.setRating(4);
        req.setReviewText("Updated");

        Mockito.when(userReviewService.updateReview(9L, 22L, 4, "Updated"))
               .thenReturn(new UserReview());

        mvc.perform(put("/api/user-reviews/{id}", 9L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
           .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/user-reviews/{id} -> 400 on validation error")
    void updateReview_badRequest() throws Exception {
        var req = new UserReviewController.UpdateUserReviewRequest();
        req.setReviewerId(22L);
        req.setRating(0);

        Mockito.when(userReviewService.updateReview(anyLong(), anyLong(), anyInt(), any()))
               .thenThrow(new IllegalArgumentException("Invalid rating"));

        mvc.perform(put("/api/user-reviews/{id}", 9L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message").value("Invalid rating"));
    }

    // ---- DELETE ----

    @Test
    @DisplayName("DELETE /api/user-reviews/{id}?reviewerId= -> 200 when service succeeds")
    void deleteReview_ok() throws Exception {
        Mockito.doNothing().when(userReviewService).deleteReview(3L, 22L);

        mvc.perform(delete("/api/user-reviews/{id}", 3L).param("reviewerId", "22"))
           .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/user-reviews/{id}?reviewerId= -> 400 on validation error")
    void deleteReview_badRequest() throws Exception {
        Mockito.doThrow(new IllegalArgumentException("Not your review"))
               .when(userReviewService).deleteReview(3L, 22L);

        mvc.perform(delete("/api/user-reviews/{id}", 3L).param("reviewerId", "22"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message").value("Not your review"));
    }

    // ---- can-review ----

    @Nested
    class CanReview {
        @Test
        @DisplayName("GET /api/user-reviews/can-review?rentalId=... -> 200 true")
        void withRentalId_true() throws Exception {
            Mockito.when(userReviewService.canUserReview(8L, 11L, 22L)).thenReturn(true);

            mvc.perform(get("/api/user-reviews/can-review")
                    .param("rentalId", "8")
                    .param("reviewedUserId", "11")
                    .param("reviewerId", "22"))
               .andExpect(status().isOk())
               .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("GET /api/user-reviews/can-review (legacy) -> 200 false")
        void legacy_false() throws Exception {
            Mockito.when(userReviewService.canUserReview(11L, 22L)).thenReturn(false);

            mvc.perform(get("/api/user-reviews/can-review")
                    .param("reviewedUserId", "11")
                    .param("reviewerId", "22"))
               .andExpect(status().isOk())
               .andExpect(content().string("false"));
        }
    }

    // ---- aggregates ----

    @Test
    @DisplayName("GET /api/user-reviews/user/{id}/average-rating -> 200 with double")
    void averageRating_ok() throws Exception {
        Mockito.when(userReviewService.getAverageRating(11L)).thenReturn(4.2);

        mvc.perform(get("/api/user-reviews/user/{userId}/average-rating", 11L))
           .andExpect(status().isOk())
           .andExpect(content().string("4.2"));
    }

    @Test
    @DisplayName("GET /api/user-reviews/user/{id}/count -> 200 with long")
    void reviewCount_ok() throws Exception {
        Mockito.when(userReviewService.getReviewCount(11L)).thenReturn(5L);

        mvc.perform(get("/api/user-reviews/user/{userId}/count", 11L))
           .andExpect(status().isOk())
           .andExpect(content().string("5"));
    }
}
