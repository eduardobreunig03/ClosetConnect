package com.example.closetconnect.controller;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.services.ClothingCardQueryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

// ✅ Only the Hamcrest bits you assert with
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;

@WebMvcTest(ClothingCardFilterController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClothingCardFilterControllerTest {

    @Autowired private MockMvc mvc;

    @MockBean private ClothingCardQueryService queryService;

    // ---------- full set of params, valid date, custom pagination ----------
    @Test
    void filter_allParams_validDate_passedThrough_andJsonOk() throws Exception {
        LocalDateTime from = LocalDateTime.parse("2025-11-02T12:00:00");

        Page<ClothingCard> page = new PageImpl<>(List.of(card(1L), card(2L)));
        when(queryService.filter(
                eq("dress"), eq(true), eq("female"), eq("Sydney"),
                eq("M"), eq("Zara"), eq(10), eq(100),
                eq(from), eq("price_desc"), any(Pageable.class)))
            .thenReturn(page);

        mvc.perform(get("/api/clothing-cards/filter")
                .param("q", "dress")
                .param("availability", "true")
                .param("gender", "female")
                .param("location", "Sydney")
                .param("size", "M")
                .param("brand", "Zara")
                .param("minCost", "10")
                .param("maxCost", "100")
                .param("availableFrom", "2025-11-02T12:00:00")
                .param("sort", "price_desc")
                .param("page", "2")
                .param("sizePage", "5"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content", hasSize(2)))
           .andExpect(jsonPath("$.content[0].clothingId", is(1)))
           .andExpect(jsonPath("$.content[1].clothingId", is(2)));

        // capture pageable to assert page & size
        ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
        verify(queryService).filter(
                eq("dress"), eq(true), eq("female"), eq("Sydney"),
                eq("M"), eq("Zara"), eq(10), eq(100),
                eq(from), eq("price_desc"), pageableCap.capture());
        Pageable p = pageableCap.getValue();
        // controller uses PageRequest.of(page, sizePage)
        // page=2, sizePage=5 -> page index 2, size 5
        org.assertj.core.api.Assertions.assertThat(p.getPageNumber()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(p.getPageSize()).isEqualTo(5);
    }

    // ---------- invalid date should be ignored (null passed to service) ----------
    @Test
    void filter_invalidDate_ignored_nullFromPassed() throws Exception {
        when(queryService.filter(
                eq("coat"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(),
                isNull(), eq("popularity"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(card(10L))));

        mvc.perform(get("/api/clothing-cards/filter")
                .param("q", "coat")
                .param("availableFrom", "not-a-date")) // will be caught & ignored
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content[0].clothingId", is(10)));

        // also verify we didn't accidentally parse any date
        verify(queryService).filter(
                eq("coat"), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(),
                isNull(), eq("popularity"), any(Pageable.class));
    }

    // ---------- defaults when no params provided ----------
    @Test
    void filter_defaults_used_whenNoParams() throws Exception {
        when(queryService.filter(
                isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(),
                isNull(), eq("popularity"), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        mvc.perform(get("/api/clothing-cards/filter"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content", hasSize(0)));

        ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
        verify(queryService).filter(
                isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), isNull(), isNull(),
                isNull(), eq("popularity"), pageableCap.capture());
        Pageable p = pageableCap.getValue();
        // defaults: page=0, sizePage=12
        org.assertj.core.api.Assertions.assertThat(p.getPageNumber()).isEqualTo(0);
        org.assertj.core.api.Assertions.assertThat(p.getPageSize()).isEqualTo(12);
    }

    // ----- small helpers (no production changes needed) -----
    private static ClothingCard card(Long id) {
        ClothingCard c = new ClothingCard();
        try {
            var f = ClothingCard.class.getDeclaredField("clothingId");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception ignored) {}
        return c;
    }
}
