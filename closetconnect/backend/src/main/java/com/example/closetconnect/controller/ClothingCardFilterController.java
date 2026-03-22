package com.example.closetconnect.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.services.ClothingCardQueryService;

@RestController
@RequestMapping("/api/clothing-cards/filter")
@CrossOrigin(origins = "*")
public class ClothingCardFilterController {
    private final ClothingCardQueryService queryService;
    public ClothingCardFilterController(ClothingCardQueryService queryService) { this.queryService = queryService; }

    @GetMapping
    public ResponseEntity<Page<ClothingCard>> filter(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean availability,
            @RequestParam(required = false, name="gender") String genderOrTag,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String size,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer minCost,
            @RequestParam(required = false) Integer maxCost,
            @RequestParam(required = false) String availableFrom,
            @RequestParam(defaultValue = "popularity") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int sizePage
    ) {
        LocalDateTime from = null;
        if (availableFrom != null && !availableFrom.isBlank()) {
            try { from = LocalDateTime.parse(availableFrom); } catch (Exception ignored) {}
        }
        Pageable pageable = PageRequest.of(page, sizePage);
        return ResponseEntity.ok(
                queryService.filter(q, availability, genderOrTag, location, size, brand,
                        minCost, maxCost, from, sort, pageable)
        );
    }
}
