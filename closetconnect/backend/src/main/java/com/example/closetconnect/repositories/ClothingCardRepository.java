package com.example.closetconnect.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.closetconnect.entities.ClothingCard;

@Repository
public interface ClothingCardRepository extends JpaRepository<ClothingCard, Long> {

    List<ClothingCard> findByAvailabilityTrue();

    List<ClothingCard> findBySize(String size);

    List<ClothingCard> findByOwnerUserId(Long userId);

}