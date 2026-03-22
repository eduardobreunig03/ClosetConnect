package com.example.closetconnect.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.ClothingImage;
import com.example.closetconnect.repositories.ClothingCardRepository;
import com.example.closetconnect.repositories.ClothingImageRepository;

@Service
public class ClothingCardService {

    @Autowired
    private ClothingCardRepository clothingCardRepository;

    @Autowired
    private ClothingImageRepository clothingImageRepository;

    public List<ClothingCard> findAll() {
        return clothingCardRepository.findAll();
    }

    public Optional<ClothingCard> findById(Long id) {
        return clothingCardRepository.findById(id);
    }

    public ClothingCard save(ClothingCard clothingCard) {
        return clothingCardRepository.save(clothingCard);
    }

    public void deleteById(Long id) {
        clothingCardRepository.deleteById(id);
    }

    public List<ClothingCard> findAvailableCards() {
        return clothingCardRepository.findByAvailabilityTrue();
    }

    public List<ClothingCard> findBySize(String size) {
        return clothingCardRepository.findBySize(size);
    }

    public List<ClothingCard> findByUserId(Long userId) {
        return clothingCardRepository.findByOwnerUserId(userId);
    }

    public Optional<ClothingCard> updateAvailability(Long id, boolean isAvailable) {
        Optional<ClothingCard> cardOptional = clothingCardRepository.findById(id);
        if (cardOptional.isPresent()) {
            ClothingCard card = cardOptional.get();
            card.setAvailability(isAvailable);
            return Optional.of(clothingCardRepository.save(card));
        }
        return Optional.empty();
    }

    public List<ClothingImage> getImages(ClothingCard card) {
        return clothingImageRepository.findByClothingCardOrderByPositionAsc(card);
    }

    public ClothingImage addImage(ClothingCard card, byte[] image, int position) {
        ClothingImage ci = new ClothingImage();
        ci.setClothingCard(card);
        ci.setImage(image);
        ci.setPosition(position);
        return clothingImageRepository.save(ci);
    }

    public void deleteImage(Long imageId) {
        clothingImageRepository.deleteById(imageId);
    }
}