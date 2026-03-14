package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;

    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("User with email " + user.getEmail() + " already exists");
        }
        return userRepository.save(user);
    }

    public Optional<User> getUserByFirebaseUid(String firebaseUid) {
        return userRepository.findById(firebaseUid);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User updateUser(User user) {
        if (!userRepository.existsById(user.getFirebaseUid())) {
            throw new IllegalArgumentException("User with Firebase UID " + user.getFirebaseUid() + " does not exist");
        }
        return userRepository.save(user);
    }

    public void deleteUser(String firebaseUid) {
        if (!userRepository.existsById(firebaseUid)) {
            throw new IllegalArgumentException("User with Firebase UID " + firebaseUid + " does not exist");
        }
        userRepository.deleteById(firebaseUid);
    }

    public boolean userExists(String firebaseUid) {
        return userRepository.existsById(firebaseUid);
    }
}
