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
class RentedClothingCardEntityTest {

    @Autowired private TestEntityManager em;

    private static final AtomicInteger SEQ = new AtomicInteger();

    private User owner;
    private User renter;
    private ClothingCard card;

    @BeforeEach
    void setup() {
        owner = persistUser();
        renter = persistUser();
        card = persistClothingCard(owner);
    }

    // ----------------- helpers -----------------

    private User persistUser() {
        String suffix = Long.toString(System.nanoTime(), 36) + SEQ.getAndIncrement();
        User u = new User();
        u.setUserName("u" + suffix);
        u.setEmail("u" + suffix + "@t.io"); // keep < 50 chars
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
            "M",                 // size (<= 5)
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

    // ----------------- tests -----------------

    @Test
    void nullClothingCard_violatesNotNull() {
        RentedClothingCard r = new RentedClothingCard();
        r.setClothingCard(null);                // <- violate NOT NULL
        r.setRenter(renter);
        r.setRentalDate(LocalDateTime.now().plusDays(1));
        r.setReturnDate(LocalDateTime.now().plusDays(7));
        r.setReturned(false);

        assertThatThrownBy(() -> {
            em.persist(r);
            em.flush(); // force INSERT
        })
        .isInstanceOf(ConstraintViolationException.class)
        .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
    }

    @Test
    void nullRenter_violatesNotNull() {
        RentedClothingCard r = new RentedClothingCard();
        r.setClothingCard(card);
        r.setRenter(null);                      // <- violate NOT NULL
        r.setRentalDate(LocalDateTime.now().plusDays(1));
        r.setReturnDate(LocalDateTime.now().plusDays(7));
        r.setReturned(false);

        assertThatThrownBy(() -> {
            em.persist(r);
            em.flush();
        })
        .isInstanceOf(ConstraintViolationException.class)
        .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
    }

    @Test
    void nullReturnDate_violatesNotNull() {
        RentedClothingCard r = new RentedClothingCard();
        r.setClothingCard(card);
        r.setRenter(renter);
        r.setRentalDate(LocalDateTime.now().plusDays(1));
        r.setReturnDate(null);                  // <- violate NOT NULL
        r.setReturned(false);

        assertThatThrownBy(() -> {
            em.persist(r);
            em.flush();
        })
        .isInstanceOf(ConstraintViolationException.class)
        .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
    }

    @Test
    void updatedAt_bumpsOnMerge() {
        RentedClothingCard r = new RentedClothingCard();
        r.setClothingCard(card);
        r.setRenter(renter);
        r.setRentalDate(LocalDateTime.now().plusDays(1));
        r.setReturnDate(LocalDateTime.now().plusDays(7));
        r.setReturned(false);

        em.persist(r);
        flushAndClear();

        RentedClothingCard found = em.find(RentedClothingCard.class, r.getRentalId());
        Instant before = found.getUpdatedAt();

        // bump updatedAt manually (or trigger @PreUpdate by changing a field)
        found.setUpdatedAt(Instant.now().plusSeconds(5));
        em.merge(found);
        flushAndClear();

        RentedClothingCard after = em.find(RentedClothingCard.class, r.getRentalId());
        assertThat(after.getUpdatedAt()).isAfter(before);
    }
}
