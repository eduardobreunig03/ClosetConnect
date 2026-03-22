package com.example.closetconnect.entities;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.closetconnect.entities.ClothingImage;

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
    "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ClothingImageEntityTest {

    @Autowired private TestEntityManager em;

    private static final AtomicInteger SEQ = new AtomicInteger();
    private User owner;
    private ClothingCard card;

    @BeforeEach
    void setup() {
        owner = persistUser();
        card  = persistClothingCard(owner);
    }

    // ----------------- helpers -----------------

    private User persistUser() {
        // <= 50 chars email, unique:
        // example: uky3d9m7@t.io (always short)
        String suffix = Long.toString(System.nanoTime(), 36) + SEQ.getAndIncrement();
        User u = new User();
        u.setUserName("u" + suffix);          // short username
        u.setEmail("u" + suffix + "@t.io");   // keep under 50 chars
        u.setPassword("pw");                  // satisfy NOT NULL if present
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
            "Nice item desc",    // description
            LocalDateTime.now().plusDays(3),
            "tag",
            "brand",
            "Sydney",
            0,                   // rating
            false                // gender
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
    void saveAndLoad_image_ok() {
        byte[] content = new byte[] { 'P', 'N', 'G', 0, 1 }; // PNG + 0x00 0x01
    
        ClothingImage img = new ClothingImage();
        img.setClothingCard(card);
        img.setImage(content);
        em.persist(img);
        flushAndClear();
    
        ClothingImage found = em.find(ClothingImage.class, img.getImageId());
        assertThat(found.getImage()).isEqualTo(content);
    }

    @Test
    void base64Getter_returnsExpectedString() {
        byte[] content = "abc".getBytes(StandardCharsets.UTF_8); // base64 = YWJj
        ClothingImage img = new ClothingImage();
        img.setImage(content);
        assertThat(img.getImageData()).isEqualTo("YWJj");
    }
    
    @Test
    void explicitPosition_persists() {
        ClothingImage img = new ClothingImage();
        img.setClothingCard(card);
        img.setImage(new byte[]{9,9});
        img.setPosition(3);
        em.persist(img);
        flushAndClear();

        ClothingImage found = em.find(ClothingImage.class, img.getImageId());
        assertThat(found.getPosition()).isEqualTo(3);
    }

    @Test
    void defaultPosition_isZeroWhenNotSet() {
        ClothingImage img = new ClothingImage();
        img.setClothingCard(card);
        img.setImage(new byte[]{7});
        // no position set
        em.persist(img);
        flushAndClear();

        ClothingImage found = em.find(ClothingImage.class, img.getImageId());
        assertThat(found.getPosition()).isEqualTo(0);
    }

    @Test
    void createdAt_isSetOnInsert() {
        ClothingImage img = new ClothingImage();
        img.setClothingCard(card);
        img.setImage(new byte[]{1});
        em.persist(img);
        flushAndClear();

        ClothingImage found = em.find(ClothingImage.class, img.getImageId());
        assertThat(found.getCreatedAt()).isNotNull();
    }
}
