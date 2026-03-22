package com.example.closetconnect.entities;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.PersistenceException;

import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.flyway.enabled=false",
    // not strictly needed here, but safe if other entities use Postgres-specific types:
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserReviewEntityTest {

  @Autowired private TestEntityManager em;

  private static final AtomicInteger SEQ = new AtomicInteger();

  private User owner;
  private User renter;
  private User target; // reviewed user (could be owner or renter)
  private ClothingCard card;
  private RentedClothingCard rental;

  @BeforeEach
  void setup() {
    owner  = persistUser();
    renter = persistUser();
    target = owner; // review the owner in these tests

    card   = persistCard(owner);
    rental = persistRental(card, renter, LocalDateTime.now().plusDays(3));
  }

  private void flushAndClear() { em.flush(); em.clear(); }

  private String shortEmail() {
    return "u" + Long.toString(System.nanoTime(), 36) + SEQ.getAndIncrement() + "@t.io";
  }

  private User persistUser() {
    User u = new User("u" + SEQ.getAndIncrement(), shortEmail(), "pw");
    u.setCreatedAt(Instant.now());
    u.setUpdatedAt(Instant.now());
    em.persist(u); em.flush();
    return u;
  }

  private ClothingCard persistCard(User owner) {
    ClothingCard c = new ClothingCard(
        owner,
        "Sample Title",
        100,                 // cost
        20,                  // deposit
        "M",                 // size <= 5
        "Great condition",
        LocalDateTime.now().plusDays(2),
        "tag",
        "brand",
        "Sydney",
        0, false
    );
    em.persist(c); em.flush();
    return c;
  }

  private RentedClothingCard persistRental(ClothingCard card, User renter, LocalDateTime returnDate) {
    RentedClothingCard r = new RentedClothingCard(card, renter, returnDate);
    em.persist(r); em.flush();
    return r;
  }

  // ---------- tests ----------

  @Test
  void saveAndLoad_ok() {
    UserReview ur = new UserReview(rental, target, renter, 5, "excellent");
    em.persist(ur);
    flushAndClear();

    UserReview found = em.find(UserReview.class, ur.getReviewId());
    assertThat(found).isNotNull();
    assertThat(found.getRental().getRentalId()).isEqualTo(rental.getRentalId());
    assertThat(found.getReviewedUser().getUserId()).isEqualTo(target.getUserId());
    assertThat(found.getReviewer().getUserId()).isEqualTo(renter.getUserId());
    assertThat(found.getRating()).isEqualTo(5);
    assertThat(found.getReviewText()).isEqualTo("excellent");
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
  }

  @Test
void nullRental_violatesNotNull() {
  UserReview ur = new UserReview(null, target, renter, 4, "ok");
  assertThatThrownBy(() -> { em.persist(ur); em.flush(); })
      .isInstanceOfAny(
          DataIntegrityViolationException.class,
          PersistenceException.class,
          ConstraintViolationException.class
      )
      .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
}

@Test
void nullReviewedUser_violatesNotNull() {
  UserReview ur = new UserReview(rental, null, renter, 4, "ok");
  assertThatThrownBy(() -> { em.persist(ur); em.flush(); })
      .isInstanceOfAny(
          DataIntegrityViolationException.class,
          PersistenceException.class,
          ConstraintViolationException.class
      )
      .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
}

@Test
void nullReviewer_violatesNotNull() {
  UserReview ur = new UserReview(rental, target, null, 4, "ok");
  assertThatThrownBy(() -> { em.persist(ur); em.flush(); })
      .isInstanceOfAny(
          DataIntegrityViolationException.class,
          PersistenceException.class,
          ConstraintViolationException.class
      )
      .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
}

  @Test
  void reviewText_canBeNull() {
    UserReview ur = new UserReview(rental, target, renter, 3, null);
    em.persist(ur);
    flushAndClear();

    UserReview found = em.find(UserReview.class, ur.getReviewId());
    assertThat(found.getReviewText()).isNull();
  }

  @Test
  void updatedAt_canBeBumpedAndPersists() {
    UserReview ur = new UserReview(rental, target, renter, 4, "good");
    em.persist(ur);
    flushAndClear();

    UserReview f = em.find(UserReview.class, ur.getReviewId());
    Instant before = f.getUpdatedAt();
    f.setUpdatedAt(Instant.now().plusSeconds(5));
    em.merge(f);
    flushAndClear();

    UserReview g = em.find(UserReview.class, ur.getReviewId());
    assertThat(g.getUpdatedAt()).isAfter(before);
  }
}
