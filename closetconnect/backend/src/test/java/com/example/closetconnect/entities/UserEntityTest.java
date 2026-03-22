package com.example.closetconnect.entities;

import static org.assertj.core.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import jakarta.persistence.PersistenceException;

import org.h2.jdbc.JdbcSQLDataException;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.sql.init.mode=never",
    "spring.flyway.enabled=false",
    // Enable PostgreSQL mode so H2 accepts columnDefinition = 'bytea'
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UserEntityTest {

  @Autowired private TestEntityManager em;

  private static String uniqueEmail() {
    // Very short email to respect VARCHAR(50)
    return "u" + Long.toString(System.nanoTime(), 36) + "@t.io";
  }

  private void flushAndClear() {
    em.flush();
    em.clear();
  }

  @Test
  void saveAndLoad_ok_defaultsPresent() {
    User u = new User("alice", uniqueEmail(), "pw");
    u.setAddress("Sydney");
    u.setBio("hello");
    u.setProfileImage("PNG".getBytes(StandardCharsets.ISO_8859_1));

    em.persist(u);
    flushAndClear();

    User found = em.find(User.class, u.getUserId());
    assertThat(found).isNotNull();
    assertThat(found.getUserName()).isEqualTo("alice");
    assertThat(found.getEmail()).isEqualTo(u.getEmail());
    assertThat(found.getPassword()).isEqualTo("pw");
    assertThat(found.getRating()).isEqualTo(0);
    assertThat(found.getVerificationStatus()).isFalse();
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getProfileImage()).isNotNull();
  }
  
  @Test
  void emailMustBeUnique_violatesUniqueConstraint() {
    String email = uniqueEmail();
    User u1 = new User("a", email, "pw");
    User u2 = new User("b", email, "pw2");
  
    em.persist(u1);
  
    assertThatThrownBy(() -> { em.persist(u2); em.flush(); })
      // depending on stack, outer may be Hibernate's ConstraintViolationException,
      // or Spring's DataIntegrityViolationException, or a JPA PersistenceException
      .isInstanceOfAny(
          org.hibernate.exception.ConstraintViolationException.class,
          org.springframework.dao.DataIntegrityViolationException.class,
          jakarta.persistence.PersistenceException.class
      )
      .hasRootCauseInstanceOf(org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException.class);
  }

  @Test
  void nullUserName_violatesNotNull() {
    User u = new User(null, uniqueEmail(), "pw");
  
    assertThatThrownBy(() -> { em.persist(u); em.flush(); })
      .isInstanceOfAny(
          org.hibernate.exception.ConstraintViolationException.class,
          org.springframework.dao.DataIntegrityViolationException.class,
          jakarta.persistence.PersistenceException.class
      )
      .hasRootCauseInstanceOf(org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException.class);
  }
  

  @Test
  void nullPassword_violatesNotNull() {
    User u = new User("bob", uniqueEmail(), null);
  
    assertThatThrownBy(() -> { em.persist(u); em.flush(); })
      .isInstanceOfAny(
          org.hibernate.exception.ConstraintViolationException.class,
          org.springframework.dao.DataIntegrityViolationException.class,
          jakarta.persistence.PersistenceException.class
      )
      .hasRootCauseInstanceOf(org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException.class);
  }

  @Test
  void emailTooLong_over50_violatesLength() {
    // 51 chars before domain to exceed total length
    String local = "a".repeat(47); // 47 + "@t.io"(5) = 52 (>50) with at least one char more
    String email = local + "@t.io";
    User u = new User("bob", email, "pw");
    assertThatThrownBy(() -> { em.persist(u); em.flush(); })
      .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class)
      // dialect may throw DataException -> map to JdbcSQLDataException
      .hasRootCauseInstanceOf(JdbcSQLDataException.class);
  }

  @Test
  void passwordTooLong_over255_violatesLength() {
    String longPw = "x".repeat(256);
    User u = new User("bob", uniqueEmail(), longPw);
    assertThatThrownBy(() -> { em.persist(u); em.flush(); })
      .isInstanceOfAny(DataIntegrityViolationException.class, PersistenceException.class)
      .hasRootCauseInstanceOf(JdbcSQLDataException.class);
  }

  @Test
  void updatedAt_canBeBumpedAndPersists() {
    User u = new User("bob", uniqueEmail(), "pw");
    em.persist(u);
    flushAndClear();

    User found = em.find(User.class, u.getUserId());
    Instant before = found.getUpdatedAt();
    found.setUpdatedAt(Instant.now().plusSeconds(3));
    em.merge(found);
    flushAndClear();

    User after = em.find(User.class, u.getUserId());
    assertThat(after.getUpdatedAt()).isAfter(before);
  }
}
