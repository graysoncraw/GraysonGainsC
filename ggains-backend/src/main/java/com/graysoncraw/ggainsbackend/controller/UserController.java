package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.UserRequestDTO;
import com.graysoncraw.ggainsbackend.dto.UserResponseDTO;
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
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody UserRequestDTO request) {
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

        return ResponseEntity.status(HttpStatus.CREATED).body(toUserResponse(createdUser));
    }

    @GetMapping("/{firebaseUid}")
    public UserResponseDTO getUserByFirebaseUid(@PathVariable String firebaseUid) {
        User user = userService.getUserByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new NoSuchElementException("User not found for firebaseUid: " + firebaseUid));
        return toUserResponse(user);
    }

    // ?email=...
    @GetMapping(params = "email")
    public UserResponseDTO getUserByEmail(@RequestParam String email) {
        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new NoSuchElementException("User not found for email: " + email));
        return toUserResponse(user);
    }

    @PutMapping(value = "/{firebaseUid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UserResponseDTO updateUser(@PathVariable String firebaseUid, @Valid @RequestBody UserRequestDTO request) {
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

        User updatedUser = userService.updateUser(userToUpdate);
        return toUserResponse(updatedUser);
    }

    @DeleteMapping("/{firebaseUid}")
    public ResponseEntity<Void> deleteUser(@PathVariable String firebaseUid) {
        userService.deleteUser(firebaseUid);
        return ResponseEntity.noContent().build();
    }

    private UserResponseDTO toUserResponse(User user) {
        UserResponseDTO response = new UserResponseDTO();
        response.setFirebaseUid(user.getFirebaseUid());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setDateCreated(user.getDateCreated());
        response.setGender(user.getGender());
        response.setHeightFt(user.getHeightFt());
        response.setHeightIn(user.getHeightIn());
        response.setWeight(user.getWeight());
        return response;
    }
}
