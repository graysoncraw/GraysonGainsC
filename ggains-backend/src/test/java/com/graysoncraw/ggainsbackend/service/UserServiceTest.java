package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserThrowsWhenEmailAlreadyExists() {
        User request = User.builder()
                .firebaseUid("uid-1")
                .email("user@example.com")
                .firstName("A")
                .lastName("B")
                .build();
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(request)
        );

        assertEquals("User with email user@example.com already exists", exception.getMessage());
    }

    @Test
    void createUserSavesWhenEmailDoesNotExist() {
        User request = User.builder()
                .firebaseUid("uid-1")
                .email("user@example.com")
                .firstName("A")
                .lastName("B")
                .build();
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.createUser(request);

        assertEquals("uid-1", saved.getFirebaseUid());
        assertEquals("user@example.com", saved.getEmail());
    }
}
