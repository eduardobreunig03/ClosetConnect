package com.example.closetconnect.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import com.example.closetconnect.repositories.ClothingCardRepository;
import com.example.closetconnect.repositories.UserRepository;

import java.util.concurrent.atomic.AtomicInteger;

@DataJpaTest(properties = {
    // keep tests fast and isolated
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ClothingCardEntityTest {

    @Autowired private TestEntityManager em;
    @Autowired private ClothingCardRepository clothingRepo;
    @Autowired private UserRepository userRepo;

    private static final AtomicInteger SEQ = new AtomicInteger();

    private User owner;

    @BeforeEach
    void setup() {
        owner = persistUser(); // TODO: implement to satisfy your User constraints
    }

    // --- helpers -------------------------------------------------------------

    private User persistUser() {
        // short + unique, always < 50 chars
        String suffix = Long.toString(System.nanoTime(), 36) + SEQ.getAndIncrement();
        String email  = "u" + suffix + "@t.io";  // e.g., uky3d9m7p1@t.io
    
        User u = new User();
        u.setUserName("alice" + suffix);  // keep short
        u.setEmail(email);                // NOT NULL, fits VARCHAR(50)
        u.setPassword("secret");          // satisfy NOT NULL
        // set other NOT NULLs your User may have:
        // u.setVerificationStatus(false);
        // u.setRating(0);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
    
        em.persist(u);
        em.flush();
        return u;
    }
    

    private ClothingCard newCard(User owner) {
        return new ClothingCard(
            owner,
            "Red Cocktail Dress",
            120,                  // cost
            50,                   // deposit
            "M",                  // size (<= 5 chars)
            "Elegant dress in great condition.",
            LocalDateTime.now().plusDays(7), // availabilityDay
            "dress",              // tag
            "Zara",               // brand
            "Sydney",             // location
            null,                 // rating -> should default to 0
            null                  // gender -> should default to false
        );
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    // --- tests ---------------------------------------------------------------

    @Test
    void saveAndLoad_ok_defaultsApplied() {
        ClothingCard card = newCard(owner);

        ClothingCard saved = clothingRepo.save(card);
        flushAndClear();

        ClothingCard found = clothingRepo.findById(saved.getClothingId()).orElseThrow();

        // required/simple fields
        assertThat(found.getOwner().getClass().getName()).contains("User");
        assertThat(found.getTitle()).isEqualTo("Red Cocktail Dress");
        assertThat(found.getCost()).isEqualTo(120);
        assertThat(found.getDeposit()).isEqualTo(50);
        assertThat(found.getSize()).isEqualTo("M");
        assertThat(found.getDescription()).contains("Elegant dress");

        // defaults on entity
        assertThat(found.getAvailability()).isTrue();
        assertThat(found.getRating()).isZero();
        assertThat(found.getGender()).isFalse();

        // timestamps present
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();

        // datePosted is set (constructor sets now)
        assertThat(found.getDatePosted()).isNotNull();
        // Provided availabilityDay from constructor should be persisted
        assertThat(found.getAvailabilityDay()).isNotNull();

        // optional fields
        assertThat(found.getCoverImageId()).isNull();
        assertThat(found.getImages()).isEmpty();
    }

    @Test
    void nullOwner_violatesNotNull() {
        ClothingCard card = newCard(owner);
        card.setOwner(null);

        assertThatThrownBy(() -> {
            clothingRepo.saveAndFlush(card);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nullTitle_violatesNotNull() {
        ClothingCard card = newCard(owner);
        card.setTitle(null);

        assertThatThrownBy(() -> {
            clothingRepo.saveAndFlush(card);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nullDescription_violatesNotNull() {
        ClothingCard card = newCard(owner);
        card.setDescription(null);

        assertThatThrownBy(() -> {
            clothingRepo.saveAndFlush(card);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void sizeLongerThanFive_violatesLengthConstraint() {
        ClothingCard card = newCard(owner);
        card.setSize("XLONG6"); // > 5 chars

        assertThatThrownBy(() -> {
            clothingRepo.saveAndFlush(card);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ratingNull_violatesNotNull() {
        ClothingCard card = newCard(owner);
        card.setRating(null); // @Column(nullable=false)

        assertThatThrownBy(() -> {
            clothingRepo.saveAndFlush(card);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void updatedAt_canBeManuallyBumpedAndPersists() {
        ClothingCard card = clothingRepo.save(newCard(owner));
        flushAndClear();

        ClothingCard found = clothingRepo.findById(card.getClothingId()).orElseThrow();
        Instant oldUpdatedAt = found.getUpdatedAt();

        // There is no @PreUpdate hook in the entity, so bump manually:
        found.setUpdatedAt(Instant.now().plusSeconds(5));
        clothingRepo.saveAndFlush(found);
        flushAndClear();

        ClothingCard after = clothingRepo.findById(card.getClothingId()).orElseThrow();
        assertThat(after.getUpdatedAt()).isAfter(oldUpdatedAt);
    }
}
