package com.example.closetconnect.repositories.specs;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.example.closetconnect.entities.ClothingCard;

public class ClothingCardSpecs {
    public static Specification<ClothingCard> availability(Boolean available) {
        return available == null ? null : (root, q, cb) -> cb.equal(root.get("availability"), available);
    }
    public static Specification<ClothingCard> locationLike(String location) {
        return (location == null || location.isBlank()) ? null
            : (root, q, cb) -> cb.like(cb.lower(root.get("locationOfClothing")), "%" + location.toLowerCase() + "%");
    }
    public static Specification<ClothingCard> tagLike(String tag) {
        // Use tag for gender for now (e.g., "men", "women", "unisex") until a dedicated field exists.
        return (tag == null || tag.isBlank()) ? null
            : (root, q, cb) -> cb.like(cb.lower(root.get("tag")), "%" + tag.toLowerCase() + "%");
    }
    public static Specification<ClothingCard> sizeEq(String size) {
        return (size == null || size.isBlank()) ? null
            : (root, q, cb) -> cb.equal(cb.lower(root.get("size")), size.toLowerCase());
    }
    public static Specification<ClothingCard> brandLike(String brand) {
        return (brand == null || brand.isBlank()) ? null
            : (root, q, cb) -> cb.like(cb.lower(root.get("brand")), "%" + brand.toLowerCase() + "%");
    }
    public static Specification<ClothingCard> search(String qStr) {
        return (qStr == null || qStr.isBlank()) ? null : (root, q, cb) -> {
            var ql = "%" + qStr.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("title")), ql),
                cb.like(cb.lower(root.get("description")), ql),
                cb.like(cb.lower(root.get("brand")), ql),
                cb.like(cb.lower(root.get("tag")), ql),
                cb.like(cb.lower(root.get("locationOfClothing")), ql)
            );
        };
    }
    public static Specification<ClothingCard> costBetween(Integer min, Integer max) {
        return (min == null && max == null) ? null : (root, q, cb) -> {
            if (min != null && max != null) return cb.between(root.get("cost"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("cost"), min);
            return cb.lessThanOrEqualTo(root.get("cost"), max);
        };
    }
    public static Specification<ClothingCard> availableOnOrAfter(LocalDateTime day) {
        return day == null ? null : (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("availabilityDay"), day);
    }
}
