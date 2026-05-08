package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.user.UserRequestDTO;
import com.graysoncraw.ggainsbackend.dto.user.UserResponseDTO;
import com.graysoncraw.ggainsbackend.mapper.UserMapper;
import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.security.AuthenticatedUserGuard;
import com.graysoncraw.ggainsbackend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;
    private final AuthenticatedUserGuard authenticatedUserGuard;

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO request) {
        String authenticatedFirebaseUid = authenticatedUserGuard.getAuthenticatedFirebaseUid();
        User userToCreate = userMapper.toEntity(request);
        userToCreate.setFirebaseUid(authenticatedFirebaseUid);
        User createdUser = userService.createUser(userToCreate);

        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(createdUser));
    }

    @GetMapping("/{firebaseUid}")
    public UserResponseDTO getUserByFirebaseUid(@PathVariable String firebaseUid) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        User user = userService.getUserByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new NoSuchElementException("User not found for firebaseUid: " + firebaseUid));
        return userMapper.toResponse(user);
    }

    // ?email=...
    @GetMapping(params = "email")
    public UserResponseDTO getUserByEmail(@RequestParam String email) {
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found for email: " + email));
        authenticatedUserGuard.requireUidMatches(user.getFirebaseUid());
        return userMapper.toResponse(user);
    }

    @PutMapping(value = "/{firebaseUid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponseDTO updateUser(@PathVariable String firebaseUid, @Valid @RequestBody UserRequestDTO request) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        User userToUpdate = userMapper.toEntity(request);
        userToUpdate.setFirebaseUid(firebaseUid);

        User updatedUser = userService.updateUser(userToUpdate);
        return userMapper.toResponse(updatedUser);
    }

    @DeleteMapping("/{firebaseUid}")
    public ResponseEntity<Void> deleteUser(@PathVariable String firebaseUid) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        userService.deleteUser(firebaseUid);
        return ResponseEntity.noContent().build();
    }
}
