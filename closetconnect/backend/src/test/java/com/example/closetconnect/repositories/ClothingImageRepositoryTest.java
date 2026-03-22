package com.example.closetconnect.repositories;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.ClothingImage;
import com.example.closetconnect.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ClothingImageRepositoryTest {

    @Autowired TestEntityManager tem;
    @Autowired ClothingImageRepository repo;

    private User owner;
    private ClothingCard cardA;
    private ClothingCard cardB;

    @BeforeEach
    void setup() {
        owner = user("owner", "owner@example.com", "pw");
        tem.persist(owner);

        cardA = card(owner, "Card A", "Desc A", true, 60, "M",
                "BrandA", "tagA", "Sydney",
                LocalDateTime.of(2025, 10, 20, 0, 0),
                LocalDateTime.of(2025, 10, 10, 10, 0), 4);

        cardB = card(owner, "Card B", "Desc B", true, 40, "S",
                "BrandB", "tagB", "Newtown",
                LocalDateTime.of(2025, 10, 21, 0, 0),
                LocalDateTime.of(2025, 10, 11, 10, 0), 5);

        tem.persist(cardA);
        tem.persist(cardB);

        // Images for A in unsorted positions: 2, 0, 1
        tem.persist(image(cardA, new byte[]{1}, 2L));
        tem.persist(image(cardA, new byte[]{2}, 0L));
        tem.persist(image(cardA, new byte[]{3}, 1L));

        // Images for B to prove isolation
        tem.persist(image(cardB, new byte[]{9}, 5L));

        tem.flush();
        tem.clear();
    }
    // ---------- Helpers ----------

    private User user(String userName, String email, String password) {
        User u = new User();
        u.setUserName(userName);   // NOT NULL
        u.setEmail(email);         // NOT NULL, UNIQUE
        u.setPassword(password);   // NOT NULL
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

    private ClothingImage image(ClothingCard card, byte[] bytes, Long position) {
        ClothingImage img = new ClothingImage();
        img.setClothingCard(card);                 // NOT NULL
        img.setImage(bytes);                       // assume NOT NULL
        img.setPosition(position.intValue());      // NOT NULL
        return img;
    }

    /**
     * Reattach a reference without loading full entity to simulate typical repo usage after clear().
     */
    private ClothingCard ref(ClothingCard detached) {
        return tem.getEntityManager().getReference(ClothingCard.class, detached.getClothingId());
    }
}
