package com.example.closetconnect.repositories;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.ClothingReview;
import com.example.closetconnect.entities.User;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ClothingReviewRepositoryTest {

  @Autowired ClothingReviewRepository reviewRepo;
  @Autowired ClothingCardRepository cardRepo;
  @Autowired UserRepository userRepo;

  private User alice, bob;
  private ClothingCard card1, card2;

  @BeforeEach
  void setup() {
    alice = userRepo.save(user("alice@example.com", "Alice"));
    bob   = userRepo.save(user("bob@example.com",   "Bob"));

    card1 = cardRepo.save(cardOwnedBy(alice));
    card2 = cardRepo.save(cardOwnedBy(alice));

    var t1 = Instant.parse("2025-01-01T00:00:00Z");
    var t2 = Instant.parse("2025-01-02T00:00:00Z");
    var t3 = Instant.parse("2025-01-03T00:00:00Z");
    var t4 = Instant.parse("2025-01-04T00:00:00Z");

    reviewRepo.save(review(card1, bob,   4, "good",  t2));
    reviewRepo.save(review(card1, bob,   5, "great", t4));
    reviewRepo.save(review(card1, alice, 3, "ok",    t1));
    reviewRepo.save(review(card2, bob,   2, "meh",   t3));
  }


  // -------- helpers (put these in THIS class) --------

  private static User user(String email, String name) {
    var u = new User();
    u.setEmail(email);
    u.setUserName(name);
    u.setPassword("x");       // if non-null in your entity
    u.setRating(0);           // if non-null
    u.setCreatedAt(Instant.now());
    u.setUpdatedAt(Instant.now());
    return u;
  }

  private static ClothingCard cardOwnedBy(User owner) {
    var c = new ClothingCard();
    // Required fields (present in your services)
    setIfPresent(c, "setOwner", User.class, owner);
    setIfPresent(c, "setAvailability", Boolean.class, true);
    setIfPresent(c, "setCreatedAt", Instant.class, Instant.now());
    setIfPresent(c, "setUpdatedAt", Instant.class, Instant.now());

    // Optional fields (only set if the setter exists)
    setIfPresent(c, "setBrand", String.class, "Brand");
    setIfPresent(c, "setSize", String.class, "M");
    setIfPresent(c, "setName", String.class, "Test");
    setIfPresent(c, "setTitle", String.class, "Test");

    // price/cost variations
    setIfPresent(c, "setCost", Integer.class, 10);
    setIfPresent(c, "setCost", Long.class, 10L);
    setIfPresent(c, "setCostPerDay", Integer.class, 10);
    setIfPresent(c, "setPricePerDay", Integer.class, 10);
    setIfPresent(c, "setDailyPrice", Integer.class, 10);
    return c;
  }

  private static ClothingReview review(ClothingCard card, User reviewer, int rating, String text, Instant at) {
    var r = new ClothingReview();
    r.setClothingCard(card);
    r.setReviewer(reviewer);
    r.setReview(rating);
    r.setReviewText(text);
    r.setCreatedAt(at);
    r.setUpdatedAt(at);
    return r;
  }

  private static <T> void setIfPresent(Object target, String method, Class<T> type, T value) {
    try {
      var m = target.getClass().getMethod(method, type);
      m.setAccessible(true);
      m.invoke(target, value);
    } catch (NoSuchMethodException ignored) {
      // ok—field doesn't exist on your entity, skip
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
