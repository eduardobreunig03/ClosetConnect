package com.example.closetconnect.repositories;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ClothingCardRepositoryTest {

    @Autowired TestEntityManager tem;
    @Autowired ClothingCardRepository repo;

    private User ownerA;
    private User ownerB;

    private ClothingCard a1_available_S;
    private ClothingCard a2_unavailable_M;
    private ClothingCard a3_available_M;
    private ClothingCard b1_available_L;

    @BeforeEach
    void seed() {
        // ---- owners (User has NOT NULL userName/email/password in your entity) ----
        ownerA = user("alice", "alice@example.com", "pw");
        ownerB = user("bob", "bob@example.com", "pw");
        tem.persist(ownerA);
        tem.persist(ownerB);

        // ---- cards (ensure all NOT NULL columns are set, notably owner, size, cost, deposit, dates, title, description) ----
        a1_available_S = card(ownerA, "Red Dress", "Formal red dress", true,  60, "S",
                "Zara", "female", "Sydney", LocalDateTime.of(2025,10,10,12,0), LocalDateTime.of(2025,10,1,  9,0), 5);

        a2_unavailable_M = card(ownerA, "Denim Jacket", "Classic jacket",  false, 80, "M",
                "Levis", "male", "Newtown", LocalDateTime.of(2025,10,20, 0,0), LocalDateTime.of(2025,10,2,10,0), 4);

        a3_available_M = card(ownerA, "Skirt", "Floral skirt",             true,  45, "M",
                "Zara", "female", "Bondi",   LocalDateTime.of(2025,10,15, 0,0), LocalDateTime.of(2025,10,3,10,0), 4);

        b1_available_L = card(ownerB, "Plain Tee", "Basic tee",            true,  25, "L",
                "Uniqlo","unisex","Sydney",  LocalDateTime.of(2025,10,12, 0,0), LocalDateTime.of(2025,10,4,10,0), 3);

        for (var c : List.of(a1_available_S, a2_unavailable_M, a3_available_M, b1_available_L)) {
            tem.persist(c);
        }
        tem.flush();
        tem.clear();
    }

    // ---------- Tests ----------

    @Test
    @DisplayName("findByAvailabilityTrue() returns only available items")
    void findByAvailabilityTrue_returnsOnlyAvailable() {
        List<ClothingCard> out = repo.findByAvailabilityTrue();
        assertFalse(out.isEmpty());

        Set<String> titles = out.stream().map(ClothingCard::getTitle).collect(Collectors.toSet());
        assertTrue(titles.contains("Red Dress"));
        assertTrue(titles.contains("Skirt"));
        assertTrue(titles.contains("Plain Tee"));
        assertFalse(titles.contains("Denim Jacket")); // unavailable
    }

    @Test
    @DisplayName("findBySize('M') returns only M sized items")
    void findBySize_returnsExactMatches() {
        List<ClothingCard> out = repo.findBySize("M");
        Set<String> titles = out.stream().map(ClothingCard::getTitle).collect(Collectors.toSet());

        assertEquals(2, out.size());
        assertTrue(titles.contains("Denim Jacket"));
        assertTrue(titles.contains("Skirt"));
    }

    @Test
    @DisplayName("findByOwnerUserId(ownerA) returns only ownerA's cards")
    void findByOwnerUserId_filtersByOwner() {
        List<ClothingCard> outA = repo.findByOwnerUserId(ownerA.getUserId());
        List<ClothingCard> outB = repo.findByOwnerUserId(ownerB.getUserId());

        Set<String> titlesA = outA.stream().map(ClothingCard::getTitle).collect(Collectors.toSet());
        Set<String> titlesB = outB.stream().map(ClothingCard::getTitle).collect(Collectors.toSet());

        assertEquals(3, outA.size());
        assertTrue(titlesA.containsAll(Set.of("Red Dress", "Denim Jacket", "Skirt")));
        assertFalse(titlesA.contains("Plain Tee"));

        assertEquals(1, outB.size());
        assertTrue(titlesB.contains("Plain Tee"));
    }

    // ---------- Helpers ----------

    private User user(String userName, String email, String password) {
        User u = new User();
        u.setUserName(userName);
        u.setEmail(email);
        u.setPassword(password);
        // other fields are optional (have defaults) per your entity
        return u;
    }

    private ClothingCard card(
            User owner, String title, String desc, boolean availability, int cost, String size,
            String brand, String tag, String location,
            LocalDateTime availabilityDay, LocalDateTime datePosted, Integer rating
    ) {
        ClothingCard c = new ClothingCard();
        c.setOwner(owner);                         // NOT NULL
        c.setTitle(title);                         // NOT NULL
        c.setDescription(desc);                    // NOT NULL
        c.setAvailability(availability);
        c.setCost(cost);                           // NOT NULL
        c.setDeposit(Math.max(1, cost / 2));       // NOT NULL in schema
        c.setSize(size);                           // NOT NULL
        c.setBrand(brand);
        c.setTag(tag);
        c.setLocationOfClothing(location);
        c.setAvailabilityDay(availabilityDay);     // NOT NULL
        c.setDatePosted(datePosted);               // NOT NULL
        c.setRating(rating != null ? rating : 0);
        return c;
    }
}
