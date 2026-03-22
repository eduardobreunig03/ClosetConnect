package com.example.closetconnect.controller;

import java.util.List;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.ClothingImage;
import com.example.closetconnect.services.ClothingCardService;

@RestController
@RequestMapping("/api/clothing-cards")
@CrossOrigin(origins = "*") // Configure as needed for your frontend
public class ClothingCardController {

    @Autowired
    private ClothingCardService clothingCardService;

    public ClothingCardController(ClothingCardService clothingCardService) {
        this.clothingCardService = clothingCardService;
    }

    // Inline sanitization method
    private void sanitizeCard(ClothingCard card) {
        if (card.getTitle() != null)
            card.setTitle(Jsoup.clean(card.getTitle(), Safelist.basic()));
        if (card.getDescription() != null)
            card.setDescription(Jsoup.clean(card.getDescription(), Safelist.basic()));
        if (card.getTag() != null)
            card.setTag(Jsoup.clean(card.getTag(), Safelist.basic()));
        if (card.getSize() != null)
            card.setSize(Jsoup.clean(card.getSize(), Safelist.basic()));
        if (card.getBrand() != null)
            card.setBrand(Jsoup.clean(card.getBrand(), Safelist.basic()));
    }

    @GetMapping
    public ResponseEntity<List<ClothingCard>> getAllClothingCards() {
        List<ClothingCard> cards = clothingCardService.findAll();
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClothingCard> getClothingCardById(@PathVariable Long id) {
        Optional<ClothingCard> card = clothingCardService.findById(id);
        return card.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    public ResponseEntity<List<ClothingCard>> getAvailableClothingCards() {
        List<ClothingCard> availableCards = clothingCardService.findAvailableCards();
        return ResponseEntity.ok(availableCards);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ClothingCard>> getClothingCardsByUser(@PathVariable Long userId) {
        List<ClothingCard> userCards = clothingCardService.findByUserId(userId);
        return ResponseEntity.ok(userCards);
    }

    @PostMapping
    public ResponseEntity<ClothingCard> createClothingCard(@RequestBody ClothingCard clothingCard) {
        try {
            sanitizeCard(clothingCard); // 👈 sanitize before saving
            ClothingCard savedCard = clothingCardService.save(clothingCard);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCard);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClothingCard> updateClothingCard(@PathVariable Long id,
            @RequestBody ClothingCard clothingCard) {
        Optional<ClothingCard> existingCard = clothingCardService.findById(id);
        if (existingCard.isPresent()) {
            clothingCard.setClothingId(id);
            sanitizeCard(clothingCard); // 👈 sanitize before saving
            ClothingCard updatedCard = clothingCardService.save(clothingCard);
            return ResponseEntity.ok(updatedCard);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClothingCard(@PathVariable Long id) {
        Optional<ClothingCard> existingCard = clothingCardService.findById(id);
        if (existingCard.isPresent()) {
            clothingCardService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<ClothingCard> updateAvailability(@PathVariable Long id,
            @RequestParam boolean isAvailable) {
        Optional<ClothingCard> card = clothingCardService.updateAvailability(id, isAvailable);
        return card.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search/size")
    public ResponseEntity<List<ClothingCard>> getClothingCardsBySize(@RequestParam String size) {
        List<ClothingCard> cards = clothingCardService.findBySize(size);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<List<ClothingImage>> getImages(@PathVariable Long id) {
        Optional<ClothingCard> card = clothingCardService.findById(id);
        if (card.isEmpty())
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(clothingCardService.getImages(card.get()));
    }

    @PostMapping("/{id}/images")
    public ResponseEntity<List<ClothingImage>> addImages(@PathVariable Long id,
            @RequestBody List<String> imagesBase64) {
        Optional<ClothingCard> cardOpt = clothingCardService.findById(id);
        if (cardOpt.isEmpty())
            return ResponseEntity.notFound().build();
        ClothingCard card = cardOpt.get();
        List<ClothingImage> saved = new java.util.ArrayList<>();
        int startPos = clothingCardService.getImages(card).size();
        for (int i = 0; i < imagesBase64.size(); i++) {
            String b64 = imagesBase64.get(i);
            if (b64 == null)
                continue;
            String clean = b64.contains(",") ? b64.substring(b64.indexOf(',') + 1) : b64;
            byte[] bytes = java.util.Base64.getDecoder().decode(clean);
            saved.add(clothingCardService.addImage(card, bytes, startPos + i));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{cardId}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long cardId, @PathVariable Long imageId) {
        Optional<ClothingCard> card = clothingCardService.findById(cardId);
        if (card.isEmpty())
            return ResponseEntity.notFound().build();
        clothingCardService.deleteImage(imageId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{cardId}/cover")
    public ResponseEntity<ClothingCard> setCover(@PathVariable Long cardId, @RequestParam Long imageId) {
        Optional<ClothingCard> cardOpt = clothingCardService.findById(cardId);
        if (cardOpt.isEmpty())
            return ResponseEntity.notFound().build();
        ClothingCard card = cardOpt.get();
        card.setCoverImageId(imageId);
        ClothingCard saved = clothingCardService.save(card);
        return ResponseEntity.ok(saved);
    }
}