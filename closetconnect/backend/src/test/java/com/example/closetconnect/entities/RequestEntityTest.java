package com.example.closetconnect.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.h2.jdbc.JdbcSQLDataException;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
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
class RequestEntityTest {

  @Autowired private TestEntityManager em;

  private void flushAndClear() {
    em.flush();
    em.clear();
  }

  @Test
  void saveAndLoad_ok_defaultsAndPrePersist() {
    Request req = new Request(
        111L,                        // clothingId
        10L,                         // fromUserId
        20L,                         // toUserId
        LocalDateTime.now().plusDays(3),
        "sms:+61-400-000-000",
        "Can I borrow it next week?"
    );

    // Even if someone sets approved before persist, @PrePersist should null it:
    req.setApproved(Boolean.TRUE);

    em.persist(req);
    flushAndClear();

    Request found = em.find(Request.class, req.getRequestId());
    assertThat(found).isNotNull();
    assertThat(found.getClothingId()).isEqualTo(111L);
    assertThat(found.getFromUserId()).isEqualTo(10L);
    assertThat(found.getToUserId()).isEqualTo(20L);
    assertThat(found.getRequesterContactInfo()).isEqualTo("sms:+61-400-000-000");
    assertThat(found.getCommentsToOwner()).isEqualTo("Can I borrow it next week?");
    // @PrePersist forces pending state:
    assertThat(found.getApproved()).isNull();
    // timestamps set:
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
  }

  @Test
  void prePersist_missingAvailability_throwsIllegalState() {
    Request req = new Request();
    req.setClothingId(1L);
    req.setFromUserId(2L);
    req.setToUserId(3L);
    req.setRequesterContactInfo("email:user@example.com");
    req.setCommentsToOwner("hi");

    assertThatThrownBy(() -> { em.persist(req); flushAndClear(); })
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("availabilityRangeRequest is null");
  }

  @Test
  void nullClothingId_violatesNotNull() {
    Request req = new Request(
        null, 2L, 3L,
        LocalDateTime.now().plusDays(1), "ok", "hi"
    );

    assertThatThrownBy(() -> { em.persist(req); flushAndClear(); })
      .isInstanceOf(ConstraintViolationException.class)
      .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
  }

  @Test
  void nullFromUserId_violatesNotNull() {
    Request req = new Request(
        1L, null, 3L,
        LocalDateTime.now().plusDays(1), "ok", "hi"
    );

    assertThatThrownBy(() -> { em.persist(req); flushAndClear(); })
      .isInstanceOf(ConstraintViolationException.class)
      .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
  }

  @Test
  void nullToUserId_violatesNotNull() {
    Request req = new Request(
        1L, 2L, null,
        LocalDateTime.now().plusDays(1), "ok", "hi"
    );

    assertThatThrownBy(() -> { em.persist(req); flushAndClear(); })
      .isInstanceOf(ConstraintViolationException.class)
      .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
  }

  @Test
  void nullRequesterContactInfo_violatesNotNull() {
    Request req = new Request(
        1L, 2L, 3L,
        LocalDateTime.now().plusDays(1), null, "hi"
    );

    assertThatThrownBy(() -> { em.persist(req); flushAndClear(); })
      .isInstanceOf(ConstraintViolationException.class)
      .hasRootCauseInstanceOf(JdbcSQLIntegrityConstraintViolationException.class);
  }

  @Test
  void requesterContactInfo_tooLong_violatesLength255() {
    String longStr = "x".repeat(256); // > 255
    Request req = new Request(
        1L, 2L, 3L,
        LocalDateTime.now().plusDays(1), longStr, "hi"
    );

    assertThatThrownBy(() -> { em.persist(req); flushAndClear(); })
      // top-level can vary by dialect; accept either Hibernate DataException or ConstraintViolationException
      .isInstanceOfAny(DataException.class, ConstraintViolationException.class)
      // H2 reports 22001 string truncation as JdbcSQLDataException
      .hasRootCauseInstanceOf(JdbcSQLDataException.class);
  }

  @Test
  void approveLater_preUpdateBumpsUpdatedAt() {
    Request req = new Request(
        1L, 2L, 3L,
        LocalDateTime.now().plusDays(1), "ok", "hi"
    );
    em.persist(req);
    flushAndClear();

    Request found = em.find(Request.class, req.getRequestId());
    LocalDateTime before = found.getUpdatedAt();

    // approve -> triggers @PreUpdate (and we also rely on updatedAt bump)
    found.setApproved(Boolean.TRUE);
    em.merge(found);
    flushAndClear();

    Request after = em.find(Request.class, req.getRequestId());
    assertThat(after.getApproved()).isTrue();
    assertThat(after.getUpdatedAt()).isAfter(before);
  }
}
