package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.CreateUserRequestDTO;
import com.graysoncraw.ggainsbackend.model.User;
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

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody CreateUserRequestDTO request) {
        User createdUser = userService.createUser(User.builder()
                .firebaseUid(request.getFirebaseUid())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .gender(request.getGender())
                .heightFt(request.getHeightFt())
                .heightIn(request.getHeightIn())
                .weight(request.getWeight())
                .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("/{firebaseUid}")
    public User getUserByFirebaseUid(@PathVariable String firebaseUid) {
        return userService.getUserByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new NoSuchElementException("User not found for firebaseUid: " + firebaseUid));
    }

    // ?email=...
    @GetMapping(params = "email")
    public User getUserByEmail(@RequestParam String email) {
        return userService.getUserByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found for email: " + email));
    }

    @PutMapping(value = "/{firebaseUid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public User updateUser(@PathVariable String firebaseUid, @Valid @RequestBody CreateUserRequestDTO request) {
        User userToUpdate = User.builder()
                .firebaseUid(firebaseUid)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .gender(request.getGender())
                .heightFt(request.getHeightFt())
                .heightIn(request.getHeightIn())
                .weight(request.getWeight())
                .build();

        return userService.updateUser(userToUpdate);
    }

    @DeleteMapping("/{firebaseUid}")
    public ResponseEntity<Void> deleteUser(@PathVariable String firebaseUid) {
        userService.deleteUser(firebaseUid);
        return ResponseEntity.noContent().build();
    }
}
