package com.example.closetconnect.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.closetconnect.entities.ClothingCard;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Service
public class ClothingCardQueryService {
    @PersistenceContext private EntityManager em;

    public Page<ClothingCard> filter(
            String q,
            Boolean availability,
            String genderOrTag,        // using existing "tag" as gender for now
            String location,
            String size,
            String brand,
            Integer minCost,
            Integer maxCost,
            LocalDateTime availableFrom,
            String sortKey,
            Pageable pageable
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<ClothingCard> cq = cb.createQuery(ClothingCard.class);
        Root<ClothingCard> root = cq.from(ClothingCard.class);
        List<Predicate> preds = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            preds.add(cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("description")), like),
                    cb.like(cb.lower(root.get("brand")), like),
                    cb.like(cb.lower(root.get("tag")), like),
                    cb.like(cb.lower(root.get("locationOfClothing")), like)
            ));
        }
        if (availability != null) preds.add(cb.equal(root.get("availability"), availability));
        if (genderOrTag != null && !genderOrTag.isBlank())
            preds.add(cb.like(cb.lower(root.get("tag")), "%" + genderOrTag.toLowerCase() + "%"));
        if (location != null && !location.isBlank())
            preds.add(cb.like(cb.lower(root.get("locationOfClothing")), "%" + location.toLowerCase() + "%"));
        if (size != null && !size.isBlank())
            preds.add(cb.equal(cb.lower(root.get("size")), size.toLowerCase()));
        if (brand != null && !brand.isBlank())
            preds.add(cb.like(cb.lower(root.get("brand")), "%" + brand.toLowerCase() + "%"));
        if (minCost != null) preds.add(cb.greaterThanOrEqualTo(root.get("cost"), minCost));
        if (maxCost != null) preds.add(cb.lessThanOrEqualTo(root.get("cost"), maxCost));
        if (availableFrom != null) preds.add(cb.greaterThanOrEqualTo(root.get("availabilityDay"), availableFrom));

        cq.where(preds.toArray(Predicate[]::new));

        // sort mapping
        List<Order> orders = switch (sortKey == null ? "popularity" : sortKey) {
            case "newest"     -> List.of(cb.desc(root.get("datePosted")));
            case "price_asc"  -> List.of(cb.asc(root.get("cost")));
            case "price_desc" -> List.of(cb.desc(root.get("cost")));
            case "rating_desc"-> List.of(cb.desc(root.get("rating")));
            default           -> List.of(cb.desc(root.get("rating")), cb.desc(root.get("datePosted"))); // popularity
        };
        cq.orderBy(orders);

        TypedQuery<ClothingCard> tq = em.createQuery(cq);
        tq.setFirstResult((int) pageable.getOffset());
        tq.setMaxResults(pageable.getPageSize());
        List<ClothingCard> content = tq.getResultList();

        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<ClothingCard> cr = countCq.from(ClothingCard.class);
        List<Predicate> countPreds = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String like = "%" + q.toLowerCase() + "%";
            countPreds.add(cb.or(
                    cb.like(cb.lower(cr.get("title")), like),
                    cb.like(cb.lower(cr.get("description")), like),
                    cb.like(cb.lower(cr.get("brand")), like),
                    cb.like(cb.lower(cr.get("tag")), like),
                    cb.like(cb.lower(cr.get("locationOfClothing")), like)
            ));
        }
        if (availability != null) countPreds.add(cb.equal(cr.get("availability"), availability));
        if (genderOrTag != null && !genderOrTag.isBlank())
            countPreds.add(cb.like(cb.lower(cr.get("tag")), "%" + genderOrTag.toLowerCase() + "%"));
        if (location != null && !location.isBlank())
            countPreds.add(cb.like(cb.lower(cr.get("locationOfClothing")), "%" + location.toLowerCase() + "%"));
        if (size != null && !size.isBlank())
            countPreds.add(cb.equal(cb.lower(cr.get("size")), size.toLowerCase()));
        if (brand != null && !brand.isBlank())
            countPreds.add(cb.like(cb.lower(cr.get("brand")), "%" + brand.toLowerCase() + "%"));
        if (minCost != null) countPreds.add(cb.greaterThanOrEqualTo(cr.get("cost"), minCost));
        if (maxCost != null) countPreds.add(cb.lessThanOrEqualTo(cr.get("cost"), maxCost));
        if (availableFrom != null) countPreds.add(cb.greaterThanOrEqualTo(cr.get("availabilityDay"), availableFrom));

        countCq.select(cb.count(cr)).where(countPreds.toArray(Predicate[]::new));
        long total = em.createQuery(countCq).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }
}
