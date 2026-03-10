package com.graysoncraw.ggainsbackend.repository;

import com.graysoncraw.ggainsbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // Spring Data JPA automatically provides:
    // - save(User user)
    // - findById(String firebaseUid)
    // - findAll()
    // - deleteById(String firebaseUid)
    // - count()
    // etc.

    // Custom query methods:
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
