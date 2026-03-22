package com.example.closetconnect.services;

import com.example.closetconnect.repositories.ClothingCardRepository;
import com.example.closetconnect.repositories.ClothingImageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import com.example.closetconnect.entities.ClothingImage;
import com.example.closetconnect.entities.ClothingCard;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClothingCardServiceTest {

  @Mock private ClothingCardRepository clothingCardRepository;
  @Mock private ClothingImageRepository clothingImageRepository;

  @InjectMocks private ClothingCardService service;

  @Test
  void findAll_delegatesToRepo() {
    List<ClothingCard> stub = List.of(new ClothingCard(), new ClothingCard());
    when(clothingCardRepository.findAll()).thenReturn(stub);

    List<ClothingCard> out = service.findAll();

    assertEquals(stub, out);
    verify(clothingCardRepository).findAll();
    verifyNoMoreInteractions(clothingCardRepository, clothingImageRepository);
  }

  @Test
  void findById_present() {
    ClothingCard card = new ClothingCard();
    when(clothingCardRepository.findById(1L)).thenReturn(Optional.of(card));

    Optional<ClothingCard> out = service.findById(1L);

    assertTrue(out.isPresent());
    assertSame(card, out.get());
    verify(clothingCardRepository).findById(1L);
    verifyNoMoreInteractions(clothingCardRepository, clothingImageRepository);
  }

  @Test
  void findById_absent() {
    when(clothingCardRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<ClothingCard> out = service.findById(99L);

    assertTrue(out.isEmpty());
    verify(clothingCardRepository).findById(99L);
    verifyNoMoreInteractions(clothingCardRepository, clothingImageRepository);
  }

  @Test
  void save_delegates() {
    ClothingCard in = new ClothingCard();
    ClothingCard saved = new ClothingCard();
    when(clothingCardRepository.save(in)).thenReturn(saved);

    ClothingCard out = service.save(in);

    assertSame(saved, out);
    verify(clothingCardRepository).save(in);
    verifyNoMoreInteractions(clothingCardRepository, clothingImageRepository);
  }

  @Test
  void deleteById_delegates() {
    service.deleteById(5L);
    verify(clothingCardRepository).deleteById(5L);
    verifyNoMoreInteractions(clothingCardRepository, clothingImageRepository);
  }

  @Test
  void findAvailableCards_delegates() {
    List<ClothingCard> stub = List.of(new ClothingCard());
    when(clothingCardRepository.findByAvailabilityTrue()).thenReturn(stub);

    List<ClothingCard> out = service.findAvailableCards();

    assertEquals(stub, out);
    verify(clothingCardRepository).findByAvailabilityTrue();
    verifyNoMoreInteractions(clothingCardRepository, clothingImageRepository);
  }

  @Test
  void findBySize_delegates() {
    List<ClothingCard> stub = List.of(new ClothingCard(), new ClothingCard());
    when(clothingCardRepository.findBySize("M")).thenReturn(stub);

    List<ClothingCard> out = service.findBySize("M");

    assertEquals(stub, out);
    verify(clothingCardRepository).findBySize("M");
    verifyNoMoreInteractions(clothingCardRepository, clothingImageRepository);
  }

  @Test
  void findByUserId_delegates() {
    List<ClothingCard> stub = List.of(new ClothingCard());
    when(clothingCardRepository.findByOwnerUserId(42L)).thenReturn(stub);

    List<ClothingCard> out = service.findByUserId(42L);

    assertEquals(stub, out);
    verify(clothingCardRepository).findByOwnerUserId(42L);
    verifyNoMoreInteractions(clothingCardRepository, clothingImageRepository);
  }

  @Test
  void updateAvailability_present_updatesAndSaves() {
    ClothingCard existing = new ClothingCard();
    existing.setAvailability(false); // initial state

    when(clothingCardRepository.findById(1L)).thenReturn(Optional.of(existing));
    when(clothingCardRepository.save(any(ClothingCard.class))).thenAnswer(inv -> inv.getArgument(0));

    Optional<ClothingCard> out = service.updateAvailability(1L, true);

    assertTrue(out.isPresent());
    assertEquals(Boolean.TRUE, out.get().getAvailability()); // entity uses getAvailability()
    verify(clothingCardRepository).findById(1L);
    verify(clothingCardRepository).save(existing);
    verifyNoMoreInteractions(clothingCardRepository, clothingImageRepository);
  }

  @Test
  void updateAvailability_absent_returnsEmptyAndDoesNotSave() {
    when(clothingCardRepository.findById(9L)).thenReturn(Optional.empty());

    Optional<ClothingCard> out = service.updateAvailability(9L, false);

    assertTrue(out.isEmpty());
    verify(clothingCardRepository).findById(9L);
    verify(clothingCardRepository, never()).save(any());
    verifyNoMoreInteractions(clothingCardRepository, clothingImageRepository);
  }
  
}

