package com.example.closetconnect.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.flyway.enabled=false"
    // If you ever see dialect issues with TEXT/bytea, uncomment:
    // "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ClothingReviewEntityTest {

    @Autowired private TestEntityManager em;

    private static final AtomicInteger SEQ = new AtomicInteger();

    private User owner;
    private User reviewer;
    private ClothingCard card;

    @BeforeEach
    void setup() {
        owner = persistUser();
        reviewer = persistUser();
        card = persistClothingCard(owner);
    }

    // ------------ helpers ------------

    private User persistUser() {
        String suffix = Long.toString(System.nanoTime(), 36) + SEQ.getAndIncrement();
        User u = new User();
        u.setUserName("u" + suffix);
        u.setEmail("u" + suffix + "@t.io"); // keep under 50 chars
        u.setPassword("pw");
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        em.persist(u);
        em.flush();
        return u;
    }

    private ClothingCard persistClothingCard(User owner) {
        ClothingCard c = new ClothingCard(
            owner,
            "Sample Title",
            100,                 // cost
            20,                  // deposit
            "M",                 // size <= 5
            "Nice item desc",
            LocalDateTime.now().plusDays(3),
            "tag",
            "brand",
            "Sydney",
            0,                   // rating
            false
        );
        em.persist(c);
        em.flush();
        return c;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    // ------------ tests ------------

    @Test
    void saveAndLoad_ok() {
        ClothingReview r = new ClothingReview(card, reviewer, 5, "Loved it");
        // createdAt is insertable (updatable=false just blocks later UPDATEs)
        r.setCreatedAt(Instant.now());

        em.persist(r);
        flushAndClear();

        ClothingReview found = em.find(ClothingReview.class, r.getReviewId());
        assertThat(found).isNotNull();
        assertThat(found.getClothingCard().getClothingId()).isEqualTo(card.getClothingId());
        assertThat(found.getReviewer().getUserId()).isEqualTo(reviewer.getUserId());
        assertThat(found.getReview()).isEqualTo(5);
        assertThat(found.getReviewText()).isEqualTo("Loved it");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void nullClothingCard_violatesNotNull() {
        ClothingReview r = new ClothingReview(null, reviewer, 3, "ok");
        assertThatThrownBy(() -> {
            em.persist(r);
            em.flush(); // force INSERT
        })
        // Hibernate wrapper (top-level)
        .isInstanceOf(ConstraintViolationException.class)
        // DB-specific root cause (H2 in tests)
        .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
    }

    @Test
    void nullReviewer_violatesNotNull() {
        ClothingReview r = new ClothingReview(card, null, 3, "ok");
        assertThatThrownBy(() -> {
            em.persist(r);
            em.flush();
        })
        .isInstanceOf(ConstraintViolationException.class)
        .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
    }

    @Test
    void nullReview_violatesNotNull() {
        ClothingReview r = new ClothingReview(card, reviewer, null, "ok");
        assertThatThrownBy(() -> {
            em.persist(r);
            em.flush();
        })
        .isInstanceOf(ConstraintViolationException.class)
        .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
    }

    @Test
    void reviewText_canBeNull() {
        ClothingReview r = new ClothingReview(card, reviewer, 4, null);
        em.persist(r);
        flushAndClear();

        ClothingReview found = em.find(ClothingReview.class, r.getReviewId());
        assertThat(found.getReviewText()).isNull();
    }

    @Test
    void updatedAt_canBeManuallyBumpedAndPersists() {
        ClothingReview r = new ClothingReview(card, reviewer, 4, "text");
        em.persist(r);
        flushAndClear();

        ClothingReview found = em.find(ClothingReview.class, r.getReviewId());
        Instant before = found.getUpdatedAt();

        found.setUpdatedAt(Instant.now().plusSeconds(5));
        em.merge(found);
        flushAndClear();

        ClothingReview after = em.find(ClothingReview.class, r.getReviewId());
        assertThat(after.getUpdatedAt()).isAfter(before);
    }
}
