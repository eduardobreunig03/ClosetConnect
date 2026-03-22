package com.example.closetconnect.services;

import org.springframework.stereotype.Service;
import com.example.closetconnect.entities.User;
import com.example.closetconnect.repositories.UserRepository;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository repo) {
        this.userRepository = repo;
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Add this method
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
