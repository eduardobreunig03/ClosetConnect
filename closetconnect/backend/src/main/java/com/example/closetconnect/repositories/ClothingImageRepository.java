package com.example.closetconnect.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.ClothingImage;

public interface ClothingImageRepository extends JpaRepository<ClothingImage, Long> {
    List<ClothingImage> findByClothingCardOrderByPositionAsc(ClothingCard card);
}