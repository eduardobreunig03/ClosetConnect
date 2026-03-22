package com.example.closetconnect.services;

import com.example.closetconnect.entities.User;
import com.example.closetconnect.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @InjectMocks private UserService service;

  @Test
  void getUserById_present() {
    User u = new User();
    // If your User uses setId/getId instead, adjust the next line
    u.setUserId(42L);

    when(userRepository.findById(42L)).thenReturn(Optional.of(u));

    Optional<User> out = service.getUserById(42L);

    assertTrue(out.isPresent());
    assertSame(u, out.get());
    verify(userRepository).findById(42L);
    verifyNoMoreInteractions(userRepository);
  }

  @Test
  void getUserById_absent() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<User> out = service.getUserById(99L);

    assertTrue(out.isEmpty());
    verify(userRepository).findById(99L);
    verifyNoMoreInteractions(userRepository);
  }

  @Test
  void saveUser_delegatesToRepo() {
    User in = new User();
    when(userRepository.save(in)).thenReturn(in);

    User saved = service.saveUser(in);

    assertSame(in, saved);
    verify(userRepository).save(in);
    verifyNoMoreInteractions(userRepository);
  }
}
