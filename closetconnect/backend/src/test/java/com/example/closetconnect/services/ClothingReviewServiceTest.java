package com.example.closetconnect.services;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.ClothingReview;
import com.example.closetconnect.entities.User;
import com.example.closetconnect.repositories.ClothingCardRepository;
import com.example.closetconnect.repositories.ClothingReviewRepository;
import com.example.closetconnect.repositories.RentedClothingCardRepositoryTest;
import com.example.closetconnect.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClothingReviewServiceTest {

    @InjectMocks
    private ClothingReviewService service;

    @Mock private ClothingReviewRepository clothingReviewRepository;
    @Mock private ClothingCardRepository clothingCardRepository;
    @Mock private UserRepository userRepository;
    @Mock private RentedClothingCardRepositoryTest rentedClothingCardRepository;

    private User owner;
    private User reviewer;
    private ClothingCard card;

    @BeforeEach
    void setUp() {
        owner = new User();
        reviewer = new User();
        card = new ClothingCard();

        // set required relationships/ids via reflection (no code changes needed)
        setUserId(owner, 10L);
        setUserId(reviewer, 20L);
        setClothingId(card, 100L);
        setOwner(card, owner);
    }

    // ------------------ get lists ------------------

    @Test
    void getReviewsByClothingId_delegates() {
        when(clothingReviewRepository.findByClothingCardClothingIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(new ClothingReview(), new ClothingReview()));
        assertThat(service.getReviewsByClothingId(100L)).hasSize(2);
        verify(clothingReviewRepository).findByClothingCardClothingIdOrderByCreatedAtDesc(100L);
    }

    @Test
    void getReviewsByUserId_delegates() {
        when(clothingReviewRepository.findByReviewerUserIdOrderByCreatedAtDesc(20L))
                .thenReturn(List.of(new ClothingReview()));
        assertThat(service.getReviewsByUserId(20L)).hasSize(1);
        verify(clothingReviewRepository).findByReviewerUserIdOrderByCreatedAtDesc(20L);
    }

    // ------------------ createReview ------------------

    @Test
    void createReview_throwsWhenRatingOutOfBounds() {
        assertThatThrownBy(() -> service.createReview(100L, 20L, 0, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be between 1 and 5");
        assertThatThrownBy(() -> service.createReview(100L, 20L, 6, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be between 1 and 5");
        verifyNoInteractions(clothingCardRepository, userRepository);
    }

    @Test
    void createReview_throwsWhenClothingNotFound() {
        when(clothingCardRepository.findById(100L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createReview(100L, 20L, 5, "ok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Clothing item not found");
    }

    @Test
    void createReview_throwsWhenReviewerNotFound() {
        when(clothingCardRepository.findById(100L)).thenReturn(Optional.of(card));
        when(userRepository.findById(20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createReview(100L, 20L, 5, "ok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reviewer not found");
    }

    @Test
    void createReview_throwsWhenReviewOwnItem() {
        when(clothingCardRepository.findById(100L)).thenReturn(Optional.of(card));
        when(userRepository.findById(10L)).thenReturn(Optional.of(owner)); // reviewer==owner
        assertThatThrownBy(() -> service.createReview(100L, 10L, 5, "self"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot review your own");
    }

    @Test
    void createReview_throwsWhenAlreadyReviewed() {
        when(clothingCardRepository.findById(100L)).thenReturn(Optional.of(card));
        when(userRepository.findById(20L)).thenReturn(Optional.of(reviewer));
        when(clothingReviewRepository.existsByClothingCardClothingIdAndReviewerUserId(100L, 20L))
                .thenReturn(true);
        assertThatThrownBy(() -> service.createReview(100L, 20L, 5, "dup"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already reviewed");
    }

    // ------------------ updateReview ------------------

    @Test
    void updateReview_throwsWhenRatingInvalid() {
        assertThatThrownBy(() -> service.updateReview(55L, 20L, 0, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be between 1 and 5");
    }

    @Test
    void updateReview_throwsWhenReviewNotFound() {
        when(clothingReviewRepository.findById(55L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateReview(55L, 20L, 5, "ok"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review not found");
    }

    @Test
    void updateReview_throwsWhenDifferentUser() {
        ClothingReview r = new ClothingReview();
        setReviewer(r, owner); // owner id = 10
        when(clothingReviewRepository.findById(55L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.updateReview(55L, 20L, 5, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only update your own");
    }

    @Test
    void updateReview_happyPath_updatesAndSaves() {
        ClothingReview r = new ClothingReview();
        setReviewer(r, reviewer);
        r.setReview(2);
        r.setReviewText("old");
        r.setUpdatedAt(Instant.EPOCH);

        when(clothingReviewRepository.findById(55L)).thenReturn(Optional.of(r));
        when(clothingReviewRepository.save(any(ClothingReview.class))).thenAnswer(inv -> inv.getArgument(0));

        ClothingReview out = service.updateReview(55L, 20L, 4, "new");
        assertThat(out.getReview()).isEqualTo(4);
        assertThat(out.getReviewText()).isEqualTo("new");
        assertThat(out.getUpdatedAt()).isNotNull();
        assertThat(out.getUpdatedAt()).isAfter(Instant.EPOCH);
    }

    // ------------------ deleteReview ------------------

    @Test
    void deleteReview_throwsWhenNotFound() {
        when(clothingReviewRepository.findById(77L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteReview(77L, 20L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Review not found");
    }

    @Test
    void deleteReview_throwsWhenDifferentUser() {
        ClothingReview r = new ClothingReview();
        setReviewer(r, owner);
        when(clothingReviewRepository.findById(77L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.deleteReview(77L, 20L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("only delete your own");
    }

    @Test
    void deleteReview_success_callsDelete() {
        ClothingReview r = new ClothingReview();
        setReviewer(r, reviewer);
        when(clothingReviewRepository.findById(77L)).thenReturn(Optional.of(r));
        service.deleteReview(77L, 20L);
        verify(clothingReviewRepository).deleteById(77L);
    }

    // ------------------ getReviewById / canUserReview ------------------

    @Test
    void getReviewById_delegates() {
        when(clothingReviewRepository.findById(88L)).thenReturn(Optional.of(new ClothingReview()));
        assertThat(service.getReviewById(88L)).isPresent();
        verify(clothingReviewRepository).findById(88L);
    }

    @Test
    void canUserReview_falseWhenClothingMissing() {
        when(clothingCardRepository.findById(100L)).thenReturn(Optional.empty());
        assertThat(service.canUserReview(100L, 20L)).isFalse();
    }

    @Test
    void canUserReview_falseWhenOwnItem() {
        when(clothingCardRepository.findById(100L)).thenReturn(Optional.of(card));
        assertThat(service.canUserReview(100L, 10L)).isFalse(); // owner id
    }

    @Test
    void canUserReview_falseWhenAlreadyReviewed() {
        when(clothingCardRepository.findById(100L)).thenReturn(Optional.of(card));
        when(clothingReviewRepository.existsByClothingCardClothingIdAndReviewerUserId(100L, 20L))
                .thenReturn(true);
        assertThat(service.canUserReview(100L, 20L)).isFalse();
    }

    // ------------------ average & count ------------------

    @Test
    void getAverageRating_zeroWhenNoReviews() {
        when(clothingReviewRepository.findByClothingCardClothingIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of());
        assertThat(service.getAverageRating(100L)).isZero();
    }

    @Test
    void getAverageRating_computesAverage() {
        ClothingReview r1 = new ClothingReview(); r1.setReview(4);
        ClothingReview r2 = new ClothingReview(); r2.setReview(2);
        ClothingReview r3 = new ClothingReview(); r3.setReview(5);

        when(clothingReviewRepository.findByClothingCardClothingIdOrderByCreatedAtDesc(100L))
                .thenReturn(List.of(r1, r2, r3));

        assertThat(service.getAverageRating(100L)).isEqualTo((4 + 2 + 5) / 3.0);
    }

    @Test
    void getReviewCount_delegates() {
        when(clothingReviewRepository.countByClothingCardClothingId(100L)).thenReturn(7L);
        assertThat(service.getReviewCount(100L)).isEqualTo(7L);
        verify(clothingReviewRepository).countByClothingCardClothingId(100L);
    }

    // ------------------ reflection helpers (no entity code changes needed) ------------------

    private void setUserId(User u, Long id) {
        try {
            var f = User.class.getDeclaredField("userId");
            f.setAccessible(true);
            f.set(u, id);
        } catch (Exception ignore) {}
    }

    private void setClothingId(ClothingCard c, Long id) {
        try {
            var f = ClothingCard.class.getDeclaredField("clothingId");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception ignore) {}
    }

    private void setOwner(ClothingCard c, User owner) {
        try {
            // prefer real setter if it exists
            try {
                ClothingCard.class.getMethod("setOwner", User.class).invoke(c, owner);
                return;
            } catch (Exception ignored) { }
            var f = ClothingCard.class.getDeclaredField("owner");
            f.setAccessible(true);
            f.set(c, owner);
        } catch (Exception ignore) {}
    }

    private void setReviewer(ClothingReview r, User u) {
        r.setReviewer(u);
    }
}
