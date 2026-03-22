package com.example.closetconnect.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.closetconnect.entities.Request;
import com.example.closetconnect.services.RequestService;

@WebMvcTest(controllers = RequestController.class)
@AutoConfigureMockMvc(addFilters = false) // avoid security filters if any
class RequestControllerTest {

  @Autowired private MockMvc mvc;
  @MockBean RequestService requestService;

  @TestConfiguration
  static class Cfg {
    @Bean
    RequestService requestService() {
      return Mockito.mock(RequestService.class);
    }
  }

  // --- POST /api/requests ---

  @Test
  void createRequest_withStartDate_returns201_andParsesDate() throws Exception {
    // given
    when(requestService.createRequest(anyLong(), anyLong(), anyLong(), any(LocalDateTime.class), any(), any()))
        .thenReturn(new Request()); // body content is not asserted

    String json = """
      {
        "clothingId": 10,
        "fromUserId": 1,
        "toUserId": 2,
        "startDate": "2025-11-05T12:34:56",
        "endDate": "2025-11-05T12:34:56",
        "requesterContactInfo": "me@example.com",
        "commentsToOwner": "please"
      }
      """;

    mvc.perform(post("/api/requests")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
       .andExpect(status().isCreated());

    // verify date parsed correctly
    ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(requestService).createRequest(eq(10L), eq(1L), eq(2L), captor.capture(), any(), any());
    LocalDateTime parsed = captor.getValue();
    // basic sanity check
    assert parsed.equals(LocalDateTime.of(2025, 11, 5, 12, 34, 56));
  }

  @Test
  void createRequest_fallback_availabilityRangeRequest_returns201() throws Exception {
    when(requestService.createRequest(anyLong(), anyLong(), anyLong(), any(LocalDateTime.class), any(), any()))
        .thenReturn(new Request());

    String json = """
      {
        "clothingId": 10,
        "fromUserId": 1,
        "toUserId": 2,
        "availabilityRangeRequest": "2025-12-01T00:00:00",
        "requesterContactInfo": "me@example.com",
        "commentsToOwner": "please"
      }
      """;

    mvc.perform(post("/api/requests")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
       .andExpect(status().isCreated());

    verify(requestService).createRequest(eq(10L), eq(1L), eq(2L), any(LocalDateTime.class), any(), any());
  }

  @Test
  void createRequest_invalidStartDate_returns400() throws Exception {
    String json = """
      {
        "clothingId": 10,
        "fromUserId": 1,
        "toUserId": 2,
        "startDate": "05/11/2025 12:34:56",
        "requesterContactInfo": "me@example.com",
        "commentsToOwner": "please"
      }
      """;

    mvc.perform(post("/api/requests")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
       .andExpect(status().isBadRequest())
       .andExpect(content().string(containsString("Invalid start date format")));
    verifyNoInteractions(requestService);
  }

  @Test
  void createRequest_missingDates_returns400() throws Exception {
    String json = """
      {
        "clothingId": 10,
        "fromUserId": 1,
        "toUserId": 2,
        "requesterContactInfo": "me@example.com",
        "commentsToOwner": "please"
      }
      """;

    mvc.perform(post("/api/requests")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
       .andExpect(status().isBadRequest())
       .andExpect(content().string(containsString("Start date is required")));
    verifyNoInteractions(requestService);
  }

  @Test
  void createRequest_endBeforeStart_returns400() throws Exception {
    String json = """
      {
        "clothingId": 10,
        "fromUserId": 1,
        "toUserId": 2,
        "startDate": "2025-11-05T12:34:56",
        "endDate":   "2025-11-05T12:34:55",
        "requesterContactInfo": "me@example.com",
        "commentsToOwner": "please"
      }
      """;

    mvc.perform(post("/api/requests")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
       .andExpect(status().isBadRequest())
       .andExpect(content().string(containsString("End date must be after or equal to start date")));
    verifyNoInteractions(requestService);
  }

  @Test
  void createRequest_serviceThrows_returns400() throws Exception {
    when(requestService.createRequest(anyLong(), anyLong(), anyLong(), any(LocalDateTime.class), any(), any()))
        .thenThrow(new IllegalArgumentException("boom"));

    String json = """
      {
        "clothingId": 10,
        "fromUserId": 1,
        "toUserId": 2,
        "startDate": "2025-11-05T12:34:56",
        "requesterContactInfo": "me@example.com",
        "commentsToOwner": "please"
      }
      """;

    mvc.perform(post("/api/requests")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
       .andExpect(status().isBadRequest())
       .andExpect(content().string(containsString("boom")));
  }

  // --- GET /api/requests/requester/{fromUserId} ---

  @Test
  void getRequestsByRequester_ok() throws Exception {
    when(requestService.getRequestsByFromUserId(1L)).thenReturn(java.util.List.of());

    mvc.perform(get("/api/requests/requester/{fromUserId}", 1L))
       .andExpect(status().isOk())
       .andExpect(content().json("[]"));
  }

  // --- GET /api/requests/owner/{toUserId} ---

  @Test
  void getRequestsByOwner_ok() throws Exception {
    when(requestService.getRequestsByToUserId(2L)).thenReturn(java.util.List.of());

    mvc.perform(get("/api/requests/owner/{toUserId}", 2L))
       .andExpect(status().isOk())
       .andExpect(content().json("[]"));
  }

  // --- PUT /api/requests/{id}/approve ---

  @Test
  void approve_true_returns200() throws Exception {
    when(requestService.updateRequestApproval(eq(5L), eq(true))).thenReturn(new Request());

    String json = """
      { "approved": true }
      """;

    mvc.perform(put("/api/requests/{id}/approve", 5L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
       .andExpect(status().isOk());

    verify(requestService).updateRequestApproval(5L, true);
  }

  @Test
  void approve_false_returns204() throws Exception {
    when(requestService.updateRequestApproval(eq(6L), eq(false))).thenReturn(new Request());

    String json = """
      { "approved": false }
      """;

    mvc.perform(put("/api/requests/{id}/approve", 6L)
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
       .andExpect(status().isNoContent());

    verify(requestService).updateRequestApproval(6L, false);
  }

  // --- GET /api/requests/check/{clothingId}/{fromUserId} ---

  @Test
  void checkExistingRequest_true_returns200() throws Exception {
    when(requestService.hasUserRequestedClothing(10L, 1L)).thenReturn(true);

    mvc.perform(get("/api/requests/check/{clothingId}/{fromUserId}", 10L, 1L))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.hasRequested").value(true));
  }
}
