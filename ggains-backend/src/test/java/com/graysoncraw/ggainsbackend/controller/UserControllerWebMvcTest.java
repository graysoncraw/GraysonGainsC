package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.user.UserRequestDTO;
import com.graysoncraw.ggainsbackend.dto.user.UserResponseDTO;
import com.graysoncraw.ggainsbackend.exception.GlobalExceptionHandler;
import com.graysoncraw.ggainsbackend.mapper.UserMapper;
import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.security.AuthenticatedUserGuard;
import com.graysoncraw.ggainsbackend.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration")
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private AuthenticatedUserGuard authenticatedUserGuard;

    @Test
    void createUserReturnsCreatedWhenRequestValid() throws Exception {
        UserRequestDTO request = new UserRequestDTO();
        request.setFirstName("Grayson");
        request.setLastName("Crawford");
        request.setEmail("grayson@example.com");
        request.setGender("Male");
        request.setHeightFt(6);
        request.setHeightIn(0);
        request.setWeight(185.0);

        User mappedUser = User.builder().build();
        User createdUser = User.builder().firebaseUid("uid-123").firstName("Grayson").lastName("Crawford").email("grayson@example.com").build();
        UserResponseDTO response = new UserResponseDTO();
        response.setFirebaseUid("uid-123");
        response.setFirstName("Grayson");
        response.setLastName("Crawford");
        response.setEmail("grayson@example.com");

        when(authenticatedUserGuard.getAuthenticatedFirebaseUid()).thenReturn("uid-123");
        when(userMapper.toEntity(any(UserRequestDTO.class))).thenReturn(mappedUser);
        when(userService.createUser(any(User.class))).thenReturn(createdUser);
        when(userMapper.toResponse(createdUser)).thenReturn(response);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Grayson",
                                  "lastName":"Crawford",
                                  "email":"grayson@example.com",
                                  "gender":"Male",
                                  "heightFt":6,
                                  "heightIn":0,
                                  "weight":185.0
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firebaseUid").value("uid-123"));
    }

    @Test
    void createUserReturnsBadRequestWhenGenderMissing() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Grayson",
                                  "lastName":"Crawford",
                                  "email":"grayson@example.com",
                                  "heightFt":6,
                                  "heightIn":0,
                                  "weight":185.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("gender: Gender is required"));
    }

    @Test
    void createUserReturnsBadRequestWhenGenderInvalid() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName":"Grayson",
                                  "lastName":"Crawford",
                                  "email":"grayson@example.com",
                                  "gender":"Other",
                                  "heightFt":6,
                                  "heightIn":0,
                                  "weight":185.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("gender: Gender must be Male or Female"));
    }

    @Test
    void getUserReturnsForbiddenWhenUidMismatch() throws Exception {
        doThrow(new AccessDeniedException("You are not allowed to access this user's data"))
                .when(authenticatedUserGuard)
                .requireUidMatches("uid-other");

        mockMvc.perform(get("/api/users/uid-other"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getUserReturnsNotFoundWhenUserMissing() throws Exception {
        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");
        when(userService.getUserByFirebaseUid("uid-123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/uid-123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getUserByEmailReturnsUserWhenFoundAndAuthorized() throws Exception {
        User user = User.builder().firebaseUid("uid-123").email("grayson@example.com").build();
        UserResponseDTO response = new UserResponseDTO();
        response.setFirebaseUid("uid-123");
        response.setEmail("grayson@example.com");

        when(userService.getUserByEmail("grayson@example.com")).thenReturn(Optional.of(user));
        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");
        when(userMapper.toResponse(eq(user))).thenReturn(response);

        mockMvc.perform(get("/api/users").param("email", "grayson@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firebaseUid").value("uid-123"));
    }
}
