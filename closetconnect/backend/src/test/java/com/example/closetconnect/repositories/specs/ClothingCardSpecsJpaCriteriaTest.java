package com.example.closetconnect.repositories.specs;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

import com.example.closetconnect.entities.ClothingCard;
import com.example.closetconnect.repositories.ClothingCardRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.flyway.enabled=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ClothingCardSpecsJpaCriteriaTest {

    @Autowired private EntityManager em;
    @Autowired private ClothingCardRepository clothingRepo;

    private Object ownerRef;

    @BeforeEach
    void seed() {
        ownerRef = seedOwner();

        persist(cc(
            "Red Dress", "Zara", "women", "S", "Sydney",
            true, 25, LocalDateTime.now().minusDays(1),
            "Light summer dress"
        ));

        persist(cc(
            "Blue Jacket", "Uniqlo", "men", "M", "Melbourne",
            false, 40, LocalDateTime.now().plusDays(2),
            "Warm jacket for winter"
        ));

        persist(cc(
            "Running Hoodie", "Nike", "unisex", "L", "Sydney CBD",
            true, 60, LocalDateTime.now(),
            "Hoodie for jogging"
        ));

        persist(cc(
            "Silk Blouse", "Aritzia", "women", "XS", "Perth",
            true, 100, LocalDateTime.now().plusDays(5),
            "Formal blouse"
        ));

        persist(cc(
            "Tailored Suit", "Gucci", "men", "L", "sYdNeY nsw",
            false, 250, LocalDateTime.now().plusDays(10),
            "Two-piece suit"
        ));
    }

    // ---------- tests ----------

    @Test
    @DisplayName("availability(true) → only available")
    void availability_true_onlyAvailable() {
        Specification<ClothingCard> spec = ClothingCardSpecs.availability(true);
        List<ClothingCard> out = findAllBySpec(spec);

        for (ClothingCard c : out) {
            assertThat(c.getAvailability()).isTrue();
        }
    }

    @Test
    @DisplayName("availableOnOrAfter uses >= boundary")
    void availableOnOrAfter_boundaryInclusive() {
        LocalDateTime probe = LocalDateTime.now();
        Specification<ClothingCard> spec = ClothingCardSpecs.availableOnOrAfter(probe);
        List<ClothingCard> out = findAllBySpec(spec);

        assertThat(out.size()).isGreaterThanOrEqualTo(3);
        for (ClothingCard c : out) {
            LocalDateTime d = c.getAvailabilityDay();
            assertThat(d == null || !d.isBefore(probe)).isTrue();
        }
    }

    @Test
    @DisplayName("brandLike LIKE + lower")
    void brandLike_likeCaseInsensitive() {
        Specification<ClothingCard> spec = ClothingCardSpecs.brandLike("ni");
        List<ClothingCard> out = findAllBySpec(spec);

        boolean found = false;
        for (ClothingCard c : out) {
            String b = c.getBrand();
            if (b != null && b.toLowerCase().contains("ni")) {
                found = true; break;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("locationLike is case-insensitive substring")
    void locationLike_caseInsensitiveSubstring() {
        Specification<ClothingCard> spec = ClothingCardSpecs.locationLike("sydney");
        List<ClothingCard> out = findAllBySpec(spec);

        assertThat(out.size()).isGreaterThan(0);
        for (ClothingCard c : out) {
            String loc = c.getLocationOfClothing();
            assertThat(loc != null && loc.toLowerCase().contains("sydney")).isTrue();
        }
    }

    @Test
    @DisplayName("tagLike filters gender/unisex")
    void tagLike_filtersTag() {
        Specification<ClothingCard> spec = ClothingCardSpecs.tagLike("women");
        List<ClothingCard> out = findAllBySpec(spec);

        assertThat(out.size()).isGreaterThan(0);
        for (ClothingCard c : out) {
            String t = c.getTag();
            assertThat(t != null && t.toLowerCase().contains("women")).isTrue();
        }
    }

    @Test
    @DisplayName("sizeEq is case-insensitive equality")
    void sizeEq_caseInsensitiveEq() {
        Specification<ClothingCard> spec = ClothingCardSpecs.sizeEq("l");
        List<ClothingCard> out = findAllBySpec(spec);

        assertThat(out.size()).isGreaterThan(0);
        for (ClothingCard c : out) {
            String s = c.getSize();
            assertThat(s != null && s.equalsIgnoreCase("l")).isTrue();
        }
    }

    @Test
    @DisplayName("search hits title/description/brand/tag/location")
    void search_hitsMultipleFields() {
        Specification<ClothingCard> spec = ClothingCardSpecs.search("hoodie");
        List<ClothingCard> out = findAllBySpec(spec);

        boolean hit = false;
        for (ClothingCard c : out) {
            if (containsIgnoreCase(c.getTitle(), "hoodie")
             || containsIgnoreCase(c.getDescription(), "hoodie")
             || containsIgnoreCase(c.getBrand(), "hoodie")
             || containsIgnoreCase(c.getTag(), "hoodie")
             || containsIgnoreCase(c.getLocationOfClothing(), "hoodie")) {
                hit = true; break;
            }
        }
        assertThat(hit).isTrue();
    }

    @Test
    @DisplayName("costBetween supports (min,max), min-only, max-only")
    void costBetween_ranges() {
        // (min,max)
        List<ClothingCard> out1 = findAllBySpec(ClothingCardSpecs.costBetween(30, 100));
        assertThat(out1.size()).isGreaterThan(0);
        for (ClothingCard c : out1) assertThat(c.getCost() >= 30 && c.getCost() <= 100).isTrue();

        // min-only
        List<ClothingCard> out2 = findAllBySpec(ClothingCardSpecs.costBetween(100, null));
        assertThat(out2.size()).isGreaterThan(0);
        for (ClothingCard c : out2) assertThat(c.getCost() >= 100).isTrue();

        // max-only
        List<ClothingCard> out3 = findAllBySpec(ClothingCardSpecs.costBetween(null, 40));
        assertThat(out3.size()).isGreaterThan(0);
        for (ClothingCard c : out3) assertThat(c.getCost() <= 40).isTrue();
    }

    @Test
    @DisplayName("Chained AND filters combine correctly")
    void chained_and_combine() {
        Specification<ClothingCard> spec = Specification
            .where(ClothingCardSpecs.availability(true))
            .and(ClothingCardSpecs.locationLike("sydney"))
            .and(ClothingCardSpecs.brandLike("nike"));

        List<ClothingCard> out = findAllBySpec(spec);
        assertThat(out.size()).isEqualTo(1);
        assertThat(out.get(0).getBrand()).isEqualTo("Nike");
    }

    @Test
    @DisplayName("All-null/blank specs collapse to 'no filter' (returns all)")
    void nullSpecs_returnAll() {
        Specification<ClothingCard> spec = Specification
            .where(ClothingCardSpecs.availability(null))
            .and(ClothingCardSpecs.locationLike(null))
            .and(ClothingCardSpecs.brandLike(""))
            .and(ClothingCardSpecs.sizeEq("   "))
            .and(ClothingCardSpecs.costBetween(null, null))
            .and(ClothingCardSpecs.search(""));

        List<ClothingCard> out = findAllBySpec(spec);
        List<ClothingCard> all = clothingRepo.findAll();
        assertThat(out.size()).isEqualTo(all.size());
    }

    // ---------- Specification runner (no prod change required) ----------

    private List<ClothingCard> findAllBySpec(Specification<ClothingCard> spec) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ClothingCard> cq = cb.createQuery(ClothingCard.class);
        Root<ClothingCard> root = cq.from(ClothingCard.class);

        if (spec != null) {
            Predicate p = spec.toPredicate(root, cq, cb);
            if (p != null) cq.where(p);
        }
        cq.select(root);
        return em.createQuery(cq).getResultList();
    }

    // ---------- helpers (unchanged) ----------

    private boolean containsIgnoreCase(String s, String needle) {
        return s != null && needle != null && s.toLowerCase().contains(needle.toLowerCase());
    }

    private ClothingCard cc(
        String title, String brand, String tag, String size, String location,
        boolean available, int cost, LocalDateTime availabilityDay, String description
    ) {
        ClothingCard c = new ClothingCard();
        c.setTitle(title);
        c.setDescription(description);
        c.setBrand(brand);
        c.setTag(tag);
        c.setSize(size);
        c.setLocationOfClothing(location);
        c.setAvailability(available);
        c.setCost(cost);
        c.setAvailabilityDay(availabilityDay);

        setDepositZero(c);
        setTimestampsIfPresent(c);
        setGenderIfPresent(c, tag);
        setRatingIfPresent(c, 0);
        attachOwner(c);

        return c;
    }

    private void persist(Object entity) {
        em.persist(entity);
        em.flush();
        em.clear();
    }

    private Object seedOwner() {
        try {
            Class<?> userCls = Class.forName("com.example.closetconnect.entities.User");
            Object u = userCls.getDeclaredConstructor().newInstance();

            if (!trySet(u, "setUserName", "specs_owner", String.class)) {
              trySet(u, "setUsername", "specs_owner", String.class); 
            }
            trySet(u, "setEmail", "specs_owner@example.com", String.class);
            trySet(u, "setPassword", "pw", String.class);
            trySet(u, "setCreatedAt", Instant.now(), Instant.class);
            trySet(u, "setUpdatedAt", Instant.now(), Instant.class);

            em.persist(u);
            em.flush();
            return u;
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed owner user", e);
        }
    }

    private void attachOwner(ClothingCard c) {
        if (ownerRef == null) ownerRef = seedOwner();

        try {
            for (Method m : c.getClass().getMethods()) {
                if (m.getName().equals("setOwner") && m.getParameterCount() == 1) {
                    Class<?> p = m.getParameterTypes()[0];
                    if (p.isInstance(ownerRef)) {
                        m.invoke(c, ownerRef);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to call setOwner on ClothingCard", e);
        }

        Long ownerId = extractId(ownerRef);
        if (ownerId != null) {
            trySet(c, "setOwnerId", ownerId, Long.class);
            return;
        }

        throw new IllegalStateException("Could not set owner for ClothingCard.");
    }

    private Long extractId(Object entity) {
        try {
            for (Method gm : entity.getClass().getMethods()) {
                if ((gm.getName().equals("getId") || gm.getName().equals("getUserId")) && gm.getParameterCount() == 0) {
                    Object id = gm.invoke(entity);
                    if (id instanceof Long) return (Long) id;
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private void setDepositZero(ClothingCard c) {
        if (trySet(c, "setDeposit", 0, int.class)) return;
        if (trySet(c, "setDeposit", Integer.valueOf(0), Integer.class)) return;
        if (trySet(c, "setDeposit", Long.valueOf(0), Long.class)) return;
        if (trySet(c, "setDeposit", Double.valueOf(0), Double.class)) return;
        try {
            Method m = c.getClass().getMethod("setDeposit", BigDecimal.class);
            m.invoke(c, new BigDecimal("0"));
        } catch (Exception e) {
            throw new RuntimeException("Failed setting deposit", e);
        }
    }

    private void setTimestampsIfPresent(Object target) {
        Instant now = Instant.now();
        trySet(target, "setCreatedAt", now, Instant.class);
        trySet(target, "setUpdatedAt", now, Instant.class);
        trySet(target, "setDatePosted", LocalDateTime.now(), LocalDateTime.class);
        trySet(target, "setDatePosted", now, Instant.class);
    }

    private void setGenderIfPresent(Object target, String val) {
        trySet(target, "setGender", (val == null ? "unisex" : val), String.class);
    }

    private void setRatingIfPresent(Object target, int val) {
        if (trySet(target, "setRating", val, int.class)) return;
        trySet(target, "setRating", Integer.valueOf(val), Integer.class);
    }

    private boolean trySet(Object target, String setter, Object value, Class<?> paramType) {
        try {
            Method m = target.getClass().getMethod(setter, paramType);
            m.invoke(target, value);
            return true;
        } catch (NoSuchMethodException ignored) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error calling " + setter + "(" + paramType.getSimpleName() + ")", e);
        }
    }
}
