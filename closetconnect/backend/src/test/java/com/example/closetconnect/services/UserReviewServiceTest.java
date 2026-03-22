package com.example.closetconnect.services;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.RentedClothingCard;
import com.example.closetconnect.entities.User;
import com.example.closetconnect.entities.UserReview;
import com.example.closetconnect.repositories.UserReviewRepository;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    // Keep H2 in Postgres compatibility mode for dialect-specific columns if any:
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(UserReviewService.class)
class UserReviewServiceTest {

  @Autowired private TestEntityManager em;
  @Autowired private UserReviewService service;
  @Autowired private UserReviewRepository reviewRepo;

  private static final AtomicInteger SEQ = new AtomicInteger();

  private User owner;
  private User renter;
  private User outsider;
  private ClothingCard card;
  private RentedClothingCard rental;

  private void flushClear() { em.flush(); em.clear(); }

  private static String uniqEmail() {
    return "u" + Long.toString(System.nanoTime(), 36) + SEQ.getAndIncrement() + "@t.io";
  }

  private User persistUser(String name) {
    User u = new User(name, uniqEmail(), "pw");
    em.persist(u);
    return u;
  }

  private ClothingCard persistCard(User owner) {
    ClothingCard c = new ClothingCard(
        owner,
        "Sample Title",
        100,                 // cost
        20,                  // deposit
        "M",                 // size
        "Great condition",
        LocalDateTime.now().plusDays(2),
        "tag",
        "brand",
        "Sydney",
        0, false
    );
    em.persist(c);
    return c;
  }

  private RentedClothingCard persistRental(ClothingCard card, User renter, LocalDateTime returnDate) {
    RentedClothingCard r = new RentedClothingCard(card, renter, returnDate);
    em.persist(r);
    return r;
  }

  @BeforeEach
  void setup() {
    owner    = persistUser("owner");
    renter   = persistUser("renter");
    outsider = persistUser("outsider");
    card     = persistCard(owner);
    rental   = persistRental(card, renter, LocalDateTime.now().plusDays(3));
    flushClear();
  }

  // ---------- createReview: happy path ----------

  @Test
  void createReview_ownerReviewsRenter_ok() {
    UserReview saved = service.createReview(
        rental.getRentalId(),
        renter.getUserId(),   // reviewed
        owner.getUserId(),    // reviewer
        5,
        "excellent"
    );

    assertThat(saved.getReviewId()).isNotNull();
    assertThat(saved.getRental().getRentalId()).isEqualTo(rental.getRentalId());
    assertThat(saved.getReviewedUser().getUserId()).isEqualTo(renter.getUserId());
    assertThat(saved.getReviewer().getUserId()).isEqualTo(owner.getUserId());
    assertThat(saved.getRating()).isEqualTo(5);
    assertThat(saved.getReviewText()).isEqualTo("excellent");
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
  }

  // ---------- createReview: validation & errors ----------

  @Test
  void createReview_ratingTooLow_throws() {
    assertThatThrownBy(() -> service.createReview(
        rental.getRentalId(), renter.getUserId(), owner.getUserId(), 0, "bad"
    ))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Rating must be between 1 and 5");
  }

  @Test
  void createReview_ratingTooHigh_throws() {
    assertThatThrownBy(() -> service.createReview(
        rental.getRentalId(), renter.getUserId(), owner.getUserId(), 6, "bad"
    ))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Rating must be between 1 and 5");
  }

  @Test
  void createReview_rentalNotFound_throws() {
    Long missingRentalId = -123L;
    assertThatThrownBy(() -> service.createReview(
        missingRentalId, renter.getUserId(), owner.getUserId(), 4, "ok"
    ))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Rental transaction not found");
  }

  @Test
  void createReview_reviewerNotInRental_throws() {
    assertThatThrownBy(() -> service.createReview(
        rental.getRentalId(),
        renter.getUserId(),
        outsider.getUserId(),  // not owner/renter
        4,
        "ok"
    ))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("participated");
  }

  @Test
  void createReview_reviewedUserNotOtherParty_throws() {
    // outsider is not owner/renter of this rental
    assertThatThrownBy(() -> service.createReview(
        rental.getRentalId(),
        outsider.getUserId(),   // reviewed user not in the rental
        owner.getUserId(),
        4,
        "ok"
    ))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("other party");
  }

  @Test
  void createReview_duplicateForSameRentalAndReviewer_throws() {
    service.createReview(rental.getRentalId(), renter.getUserId(), owner.getUserId(), 5, "great");

    assertThatThrownBy(() -> service.createReview(
        rental.getRentalId(), renter.getUserId(), owner.getUserId(), 3, "again"
    ))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("already reviewed");
  }

  // ---------- updateReview ----------

  @Test
  void updateReview_ok() {
    UserReview saved = service.createReview(rental.getRentalId(), renter.getUserId(), owner.getUserId(), 3, "ok");
    Long id = saved.getReviewId();

    UserReview updated = service.updateReview(id, owner.getUserId(), 5, "much better");
    assertThat(updated.getRating()).isEqualTo(5);
    assertThat(updated.getReviewText()).isEqualTo("much better");
    assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updated.getCreatedAt());
  }

  @Test
  void updateReview_wrongUser_throws() {
    UserReview saved = service.createReview(rental.getRentalId(), renter.getUserId(), owner.getUserId(), 3, "ok");
    Long id = saved.getReviewId();

    assertThatThrownBy(() -> service.updateReview(id, renter.getUserId(), 4, "hijack"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("only update your own");
  }

  @Test
  void updateReview_invalidRating_throws() {
    UserReview saved = service.createReview(rental.getRentalId(), renter.getUserId(), owner.getUserId(), 3, "ok");
    Long id = saved.getReviewId();

    assertThatThrownBy(() -> service.updateReview(id, owner.getUserId(), 0, "nope"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("between 1 and 5");
  }

  @Test
  void updateReview_notFound_throws() {
    assertThatThrownBy(() -> service.updateReview(9999L, owner.getUserId(), 4, "x"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Review not found");
  }

  // ---------- deleteReview ----------

  @Test
  void deleteReview_ok() {
    UserReview saved = service.createReview(rental.getRentalId(), renter.getUserId(), owner.getUserId(), 4, "nice");
    Long id = saved.getReviewId();

    service.deleteReview(id, owner.getUserId());
    assertThat(reviewRepo.findById(id)).isEmpty();
  }

  @Test
  void deleteReview_wrongUser_throws() {
    UserReview saved = service.createReview(rental.getRentalId(), renter.getUserId(), owner.getUserId(), 4, "nice");
    Long id = saved.getReviewId();

    assertThatThrownBy(() -> service.deleteReview(id, renter.getUserId()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("only delete your own");
  }

  @Test
  void deleteReview_notFound_throws() {
    assertThatThrownBy(() -> service.deleteReview(424242L, owner.getUserId()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Review not found");
  }

  // ---------- canUserReview (rental-aware) ----------

  @Test
  void canUserReview_true_whenOtherPartyAndNotReviewedYet() {
    boolean allowed = service.canUserReview(rental.getRentalId(), renter.getUserId(), owner.getUserId());
    assertThat(allowed).isTrue();
  }

  @Test
  void canUserReview_false_whenReviewerIsOutsider() {
    boolean allowed = service.canUserReview(rental.getRentalId(), renter.getUserId(), outsider.getUserId());
    assertThat(allowed).isFalse();
  }

  @Test
  void canUserReview_false_whenSelf() {
    boolean allowed = service.canUserReview(rental.getRentalId(), owner.getUserId(), owner.getUserId());
    assertThat(allowed).isFalse();
  }

  @Test
  void canUserReview_false_whenAlreadyReviewed() {
    service.createReview(rental.getRentalId(), renter.getUserId(), owner.getUserId(), 5, "great");
    boolean allowed = service.canUserReview(rental.getRentalId(), renter.getUserId(), owner.getUserId());
    assertThat(allowed).isFalse();
  }

  // ---------- legacy canUserReview(reviewedUserId, reviewerId) ----------

  @Test
  void legacy_canUserReview_true_whenNoPriorDirectReviewAndNotSelf() {
    // No review yet between owner -> renter in legacy mode
    assertThat(service.canUserReview(renter.getUserId(), owner.getUserId())).isTrue();
  }

  @Test
  void legacy_canUserReview_false_whenSelf() {
    assertThat(service.canUserReview(owner.getUserId(), owner.getUserId())).isFalse();
  }

  @Test
  void legacy_canUserReview_false_whenDirectReviewAlreadyExists() {
    // Seed a direct review via rental-aware path (repo method checks pair)
    service.createReview(rental.getRentalId(), renter.getUserId(), owner.getUserId(), 5, "once");
    assertThat(service.canUserReview(renter.getUserId(), owner.getUserId())).isFalse();
  }

  // ---------- queries, ordering & aggregates ----------

  @Test
  void getReviewsForUser_and_ByUser_and_ByRentalId_work() {
    // owner reviews renter, renter reviews owner
    UserReview r1 = service.createReview(rental.getRentalId(), renter.getUserId(), owner.getUserId(), 5, "A");
    // tweak createdAt to ensure ordering
    r1.setCreatedAt(Instant.now().minusSeconds(5));
    reviewRepo.save(r1);

    UserReview r2 = service.createReview(rental.getRentalId(), owner.getUserId(), renter.getUserId(), 4, "B");

    List<UserReview> aboutOwner = service.getReviewsForUser(owner.getUserId());
    List<UserReview> aboutRenter = service.getReviewsForUser(renter.getUserId());
    List<UserReview> byOwner    = service.getReviewsByUser(owner.getUserId());
    List<UserReview> byRenter   = service.getReviewsByUser(renter.getUserId());
    List<UserReview> byRental   = service.getReviewsByRentalId(rental.getRentalId());

    assertThat(aboutOwner).extracting(UserReview::getReviewText).containsExactly("B"); // only r2
    assertThat(aboutRenter).extracting(UserReview::getReviewText).containsExactly("A"); // only r1

    assertThat(byOwner).extracting(UserReview::getReviewText).containsExactly("A");
    assertThat(byRenter).extracting(UserReview::getReviewText).containsExactly("B");

    assertThat(byRental).hasSize(2);
    assertThat(byRental.get(0).getReviewText()).isEqualTo("B"); // desc order
    assertThat(byRental.get(1).getReviewText()).isEqualTo("A");
  }

  @Test
  void getAverageRating_and_Count_work() {
    service.createReview(rental.getRentalId(), renter.getUserId(), owner.getUserId(), 5, "great");
    service.createReview(rental.getRentalId(), owner.getUserId(), renter.getUserId(), 3, "ok");

    Double avgOwner  = service.getAverageRating(owner.getUserId());   // reviews about owner -> 3
    Double avgRenter = service.getAverageRating(renter.getUserId());  // reviews about renter -> 5
    long countOwner  = service.getReviewCount(owner.getUserId());
    long countRenter = service.getReviewCount(renter.getUserId());

    assertThat(avgOwner).isEqualTo(3.0);
    assertThat(avgRenter).isEqualTo(5.0);
    assertThat(countOwner).isEqualTo(1);
    assertThat(countRenter).isEqualTo(1);
  }

  @Test
  void getReviewById_returnsSavedReview() {
    UserReview saved = service.createReview(rental.getRentalId(), renter.getUserId(), owner.getUserId(), 4, "ok");
    assertThat(service.getReviewById(saved.getReviewId()))
      .isPresent()
      .get()
      .extracting(UserReview::getReviewText)
      .isEqualTo("ok");
  }
}
