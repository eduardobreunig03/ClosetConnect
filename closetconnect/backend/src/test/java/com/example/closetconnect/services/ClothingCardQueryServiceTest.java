package com.example.closetconnect.services;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(ClothingCardQueryService.class)
class ClothingCardQueryServiceTest {

    @Autowired TestEntityManager tem;
    @Autowired ClothingCardQueryService service;

    private User owner; // FK is NOT NULL
    private ClothingCard silkDress, denimJacket, eveningGown, tshirt, floralSkirt;

    @BeforeEach
    void seed() {
        owner = makeUser("owner@example.com");
        tem.persist(owner);

        silkDress = card(
                "Silk Dress", "Elegant silk dress", "Zara", "female",
                "Sydney CBD", true, "S", 50,
                LocalDateTime.of(2025, 11, 5, 0, 0),
                LocalDateTime.of(2025, 10, 10, 10, 0),
                5
        );
        denimJacket = card(
                "Denim Jacket", "Classic blue denim", "Levis", "male",
                "Newtown", true, "M", 80,
                LocalDateTime.of(2025, 11, 2, 0, 0),
                LocalDateTime.of(2025, 10, 12, 10, 0),
                4
        );
        eveningGown = card(
                "Evening Gown", "Floor-length gown", "Gucci", "female",
                "Parramatta", false, "M", 300,
                LocalDateTime.of(2025, 11, 20, 0, 0),
                LocalDateTime.of(2025, 10, 15, 10, 0),
                4
        );
        tshirt = card(
                "T-Shirt", "Plain tee", "Uniqlo", "unisex",
                "Sydney CBD", true, "L", 25,
                LocalDateTime.of(2025, 10, 30, 0, 0),
                LocalDateTime.of(2025, 10, 18, 10, 0),
                3
        );
        floralSkirt = card(
                "Floral Skirt", "Flowery pattern", "Zara", "female",
                "Bondi", true, "S", 45,
                LocalDateTime.of(2025, 10, 29, 0, 0),
                LocalDateTime.of(2025, 10, 19, 10, 0),
                4
        );

        for (ClothingCard c : List.of(silkDress, denimJacket, eveningGown, tshirt, floralSkirt)) {
            tem.persist(c);
        }
        tem.flush();
        tem.clear();
    }

    private User makeUser(String email) {
        User u = new User();
        u.setUserName("Owner");     // @Column(nullable = false)
        u.setEmail(email);          // @Column(nullable = false, unique = true)
        u.setPassword("pw");        // @Column(nullable = false)
        return u;
    }

    private ClothingCard card(
            String title, String desc, String brand, String tag,
            String location, boolean availability, String size, int cost,
            LocalDateTime availabilityDay, LocalDateTime datePosted, Integer rating
    ) {
        ClothingCard c = new ClothingCard();
        c.setOwner(owner);                     // FK not null
        c.setTitle(title);                     // not null
        c.setDescription(desc);                // not null
        c.setBrand(brand);
        c.setTag(tag);
        c.setLocationOfClothing(location);
        c.setAvailability(availability);
        c.setSize(size);                       // not null
        c.setCost(cost);                       // not null
        c.setDeposit(Math.max(1, cost / 2));   // not null in schema → prevent H2 violation
        c.setAvailabilityDay(availabilityDay); // not null
        c.setDatePosted(datePosted);           // not null
        c.setRating(rating != null ? rating : 0);
        return c;
    }

    @Test
    @DisplayName("Default popularity sort (null/unknown key) → rating desc, datePosted desc")
    void defaultPopularitySort() {
        Pageable p = PageRequest.of(0, 10);
        Page<ClothingCard> page = service.filter(
                null, null, null, null, null, null,
                null, null, null,
                null, p
        );
        assertEquals(5, page.getTotalElements());

        // With seed above, expected order:
        // 1) Silk Dress (rating 5)
        // 2–4) rating 4 by date desc: Floral Skirt (19th) > Evening Gown (15th) > Denim Jacket (12th)
        // 5) T-Shirt (rating 3)
        assertEquals(
                List.of("Silk Dress", "Floral Skirt", "Evening Gown", "Denim Jacket", "T-Shirt"),
                page.map(ClothingCard::getTitle).toList()
        );

        // Unknown key falls back to default
        Page<ClothingCard> page2 = service.filter(
                null, null, null, null, null, null,
                null, null, null,
                "whatever", p
        );
        assertEquals(page.map(ClothingCard::getTitle).toList(), page2.map(ClothingCard::getTitle).toList());
    }

    @Test
    @DisplayName("Keyword 'zara' matches brand; verify count and order")
    void keywordBrandLike() {
        Pageable p = PageRequest.of(0, 10);
        Page<ClothingCard> page = service.filter(
                "zara", null, null, null, null, null,
                null, null, null,
                null, p
        );
        assertEquals(2, page.getTotalElements());
        assertEquals(List.of("Silk Dress", "Floral Skirt"), page.map(ClothingCard::getTitle).toList());
    }

    @Test
    @DisplayName("Availability + tag + minCost combine; verify count query mirrors content")
    void availabilityTagMinCost() {
        Pageable p = PageRequest.of(0, 10);
        Page<ClothingCard> page = service.filter(
                null, true, "female", null, null, null,
                46, null, null,
                null, p
        );
        assertEquals(1, page.getTotalElements());
        assertEquals("Silk Dress", page.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("Location + size + maxCost + availableFrom")
    void locationSizeCostAvailableFrom() {
        Pageable p = PageRequest.of(0, 10);
        Page<ClothingCard> page = service.filter(
                null, null, null, "sydney", "l", null,
                null, 30, LocalDateTime.of(2025, 10, 29, 0, 0),
                null, p
        );
        assertEquals(1, page.getTotalElements());
        assertEquals("T-Shirt", page.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("Sort: price_asc and price_desc")
    void sortPriceAscDesc() {
        Pageable p = PageRequest.of(0, 5);

        Page<ClothingCard> asc = service.filter(
                null, null, null, null, null, null,
                null, null, null,
                "price_asc", p
        );
        assertEquals(List.of("T-Shirt", "Floral Skirt", "Silk Dress", "Denim Jacket", "Evening Gown"),
                asc.map(ClothingCard::getTitle).toList());

        Page<ClothingCard> desc = service.filter(
                null, null, null, null, null, null,
                null, null, null,
                "price_desc", p
        );
        assertEquals(List.of("Evening Gown", "Denim Jacket", "Silk Dress", "Floral Skirt", "T-Shirt"),
                desc.map(ClothingCard::getTitle).toList());
    }

    @Test
    @DisplayName("Pagination: page 1 (zero-based) size 2 with popularity default")
    void paginationSecondPage() {
        Pageable p = PageRequest.of(1, 2);
        Page<ClothingCard> page = service.filter(
                null, null, null, null, null, null,
                null, null, null,
                null, p
        );
        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertEquals(List.of("Evening Gown", "Denim Jacket"),
                page.map(ClothingCard::getTitle).toList());
    }

    @Test
    @DisplayName("Blank strings are ignored (q/brand/size blank)")
    void blanksIgnored() {
        Pageable p = PageRequest.of(0, 10);
        Page<ClothingCard> page = service.filter(
                "   ", null, " ", " ", " ",
                " ", null, null, null, "newest", p
        );
        assertEquals(5, page.getTotalElements());
    }
}
