package com.example.closetconnect.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.ClothingImage;
import com.example.closetconnect.services.ClothingCardService;

@WebMvcTest(controllers = ClothingCardController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClothingCardControllerTest {

  @Autowired private MockMvc mvc;

  @org.springframework.boot.test.mock.mockito.MockBean
  private ClothingCardService clothingCardService;
  
  @TestConfiguration
  static class Cfg {
    @Bean
    ClothingCardService clothingCardService() {
      return Mockito.mock(ClothingCardService.class);
    }
  }

  // ---------- GET /api/clothing-cards ----------

  @Test
  void getAllClothingCards_ok() throws Exception {
    when(clothingCardService.findAll()).thenReturn(List.of(new ClothingCard(), new ClothingCard()));

    mvc.perform(get("/api/clothing-cards"))
       .andExpect(status().isOk());

    verify(clothingCardService).findAll();
  }

  // ---------- GET /api/clothing-cards/{id} ----------

  @Test
  void getClothingCardById_found_ok() throws Exception {
    when(clothingCardService.findById(10L)).thenReturn(Optional.of(new ClothingCard()));

    mvc.perform(get("/api/clothing-cards/{id}", 10L))
       .andExpect(status().isOk());

    verify(clothingCardService).findById(10L);
  }

  @Test
  void getClothingCardById_notFound_404() throws Exception {
    when(clothingCardService.findById(99L)).thenReturn(Optional.empty());

    mvc.perform(get("/api/clothing-cards/{id}", 99L))
       .andExpect(status().isNotFound());
  }

  // ---------- GET /api/clothing-cards/available ----------

  @Test
  void getAvailable_ok() throws Exception {
    when(clothingCardService.findAvailableCards()).thenReturn(List.of());

    mvc.perform(get("/api/clothing-cards/available"))
       .andExpect(status().isOk());

    verify(clothingCardService).findAvailableCards();
  }

  // ---------- GET /api/clothing-cards/user/{userId} ----------

  @Test
  void getByUser_ok() throws Exception {
    when(clothingCardService.findByUserId(7L)).thenReturn(List.of());

    mvc.perform(get("/api/clothing-cards/user/{userId}", 7L))
       .andExpect(status().isOk());

    verify(clothingCardService).findByUserId(7L);
  }

  // ---------- POST /api/clothing-cards ----------

  @Test
  void create_ok_created() throws Exception {
    when(clothingCardService.save(any(ClothingCard.class))).thenReturn(new ClothingCard());

    mvc.perform(post("/api/clothing-cards")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
       .andExpect(status().isCreated());

    verify(clothingCardService).save(any(ClothingCard.class));
  }

  @Test
  void create_serviceThrows_badRequest() throws Exception {
    when(clothingCardService.save(any(ClothingCard.class)))
        .thenThrow(new IllegalArgumentException("bad"));

    mvc.perform(post("/api/clothing-cards")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
       .andExpect(status().isBadRequest());
  }

  // ---------- PUT /api/clothing-cards/{id} ----------

  @Test
  void update_found_ok() throws Exception {
    when(clothingCardService.findById(5L)).thenReturn(Optional.of(new ClothingCard()));
    when(clothingCardService.save(any(ClothingCard.class))).thenReturn(new ClothingCard());

    mvc.perform(put("/api/clothing-cards/{id}", 5L)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
       .andExpect(status().isOk());

    verify(clothingCardService).findById(5L);
    verify(clothingCardService).save(any(ClothingCard.class));
  }

  @Test
  void update_notFound_404() throws Exception {
    when(clothingCardService.findById(5L)).thenReturn(Optional.empty());

    mvc.perform(put("/api/clothing-cards/{id}", 5L)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
       .andExpect(status().isNotFound());
  }

  // ---------- DELETE /api/clothing-cards/{id} ----------

  @Test
  void delete_found_noContent() throws Exception {
    when(clothingCardService.findById(12L)).thenReturn(Optional.of(new ClothingCard()));
    doNothing().when(clothingCardService).deleteById(12L);

    mvc.perform(delete("/api/clothing-cards/{id}", 12L))
       .andExpect(status().isNoContent());

    verify(clothingCardService).deleteById(12L);
  }

  @Test
  void delete_notFound_404() throws Exception {
    when(clothingCardService.findById(12L)).thenReturn(Optional.empty());

    mvc.perform(delete("/api/clothing-cards/{id}", 12L))
       .andExpect(status().isNotFound());
  }

  // ---------- PATCH /api/clothing-cards/{id}/availability?isAvailable=... ----------

  @Test
  void updateAvailability_ok() throws Exception {
    when(clothingCardService.updateAvailability(3L, true))
        .thenReturn(Optional.of(new ClothingCard()));

    mvc.perform(patch("/api/clothing-cards/{id}/availability", 3L)
        .param("isAvailable", "true"))
       .andExpect(status().isOk());

    verify(clothingCardService).updateAvailability(3L, true);
  }

  @Test
  void updateAvailability_notFound_404() throws Exception {
    when(clothingCardService.updateAvailability(3L, false))
        .thenReturn(Optional.empty());

    mvc.perform(patch("/api/clothing-cards/{id}/availability", 3L)
        .param("isAvailable", "false"))
       .andExpect(status().isNotFound());
  }

  // ---------- GET /api/clothing-cards/search/size?size=M ----------

  @Test
  void searchBySize_ok() throws Exception {
    when(clothingCardService.findBySize("M")).thenReturn(List.of());

    mvc.perform(get("/api/clothing-cards/search/size").param("size", "M"))
       .andExpect(status().isOk());

    verify(clothingCardService).findBySize("M");
  }

  // ---------- Images ----------

  @Test
  void getImages_found_ok() throws Exception {
    ClothingCard card = new ClothingCard();
    when(clothingCardService.findById(8L)).thenReturn(Optional.of(card));
    when(clothingCardService.getImages(card)).thenReturn(List.of(new ClothingImage(), new ClothingImage()));

    mvc.perform(get("/api/clothing-cards/{id}/images", 8L))
       .andExpect(status().isOk());

    verify(clothingCardService).getImages(card);
  }

  @Test
  void getImages_notFound_404() throws Exception {
    when(clothingCardService.findById(8L)).thenReturn(Optional.empty());

    mvc.perform(get("/api/clothing-cards/{id}/images", 8L))
       .andExpect(status().isNotFound());
  }

  @Test
  void addImages_created_callsAddImageForEach() throws Exception {
    ClothingCard card = new ClothingCard();
    when(clothingCardService.findById(9L)).thenReturn(Optional.of(card));
    when(clothingCardService.getImages(card)).thenReturn(List.of()); // starting at 0
    // Return a fresh image for each add
    when(clothingCardService.addImage(eq(card), any(byte[].class), anyInt()))
        .thenAnswer(inv -> new ClothingImage());

    String body = """
      [
        "data:image/png;base64,SGVsbG8=",
        "U29tZUJhc2U2NA=="
      ]
      """;

    mvc.perform(post("/api/clothing-cards/{id}/images", 9L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
       .andExpect(status().isCreated());

    verify(clothingCardService, times(2)).addImage(eq(card), any(byte[].class), anyInt());
  }

  @Test
  void addImages_cardNotFound_404() throws Exception {
    when(clothingCardService.findById(9L)).thenReturn(Optional.empty());

    mvc.perform(post("/api/clothing-cards/{id}/images", 9L)
        .contentType(MediaType.APPLICATION_JSON)
        .content("[]"))
       .andExpect(status().isNotFound());
  }

  @Test
  void deleteImage_noContent() throws Exception {
    when(clothingCardService.findById(11L)).thenReturn(Optional.of(new ClothingCard()));
    doNothing().when(clothingCardService).deleteImage(55L);

    mvc.perform(delete("/api/clothing-cards/{cardId}/images/{imageId}", 11L, 55L))
       .andExpect(status().isNoContent());

    verify(clothingCardService).deleteImage(55L);
  }

  @Test
  void deleteImage_cardNotFound_404() throws Exception {
    when(clothingCardService.findById(11L)).thenReturn(Optional.empty());

    mvc.perform(delete("/api/clothing-cards/{cardId}/images/{imageId}", 11L, 55L))
       .andExpect(status().isNotFound());
  }

  @Test
  void setCover_ok() throws Exception {
    ClothingCard card = new ClothingCard();
    when(clothingCardService.findById(20L)).thenReturn(Optional.of(card));
    when(clothingCardService.save(any(ClothingCard.class))).thenReturn(card);

    mvc.perform(patch("/api/clothing-cards/{cardId}/cover", 20L)
        .param("imageId", "77"))
       .andExpect(status().isOk());

    verify(clothingCardService).save(any(ClothingCard.class));
  }

  @Test
  void setCover_notFound_404() throws Exception {
    when(clothingCardService.findById(20L)).thenReturn(Optional.empty());

    mvc.perform(patch("/api/clothing-cards/{cardId}/cover", 20L)
        .param("imageId", "77"))
       .andExpect(status().isNotFound());
  }
}
