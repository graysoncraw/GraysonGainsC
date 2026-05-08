package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.WorkoutScheduleResponseDTO;
import com.graysoncraw.ggainsbackend.exception.GlobalExceptionHandler;
import com.graysoncraw.ggainsbackend.mapper.WorkoutScheduleMapper;
import com.graysoncraw.ggainsbackend.model.WorkoutSchedule;
import com.graysoncraw.ggainsbackend.security.AuthenticatedUserGuard;
import com.graysoncraw.ggainsbackend.service.WorkoutScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkoutScheduleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration")
class WorkoutScheduleControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkoutScheduleService workoutScheduleService;
    @MockitoBean
    private WorkoutScheduleMapper workoutScheduleMapper;
    @MockitoBean
    private AuthenticatedUserGuard authenticatedUserGuard;

    @Test
    void createWorkoutScheduleReturnsCreatedWhenValid() throws Exception {
        WorkoutSchedule mapped = WorkoutSchedule.builder().build();
        WorkoutSchedule created = WorkoutSchedule.builder().id(7L).build();
        WorkoutScheduleResponseDTO response = new WorkoutScheduleResponseDTO();
        response.setId(7L);

        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");
        when(workoutScheduleMapper.toEntity(any())).thenReturn(mapped);
        when(workoutScheduleService.createWorkoutSchedule("uid-123", mapped)).thenReturn(created);
        when(workoutScheduleMapper.toResponse(created)).thenReturn(response);

        mockMvc.perform(post("/api/users/uid-123/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cycleStartDate":"2026-04-20",
                                  "benchDay":"MONDAY",
                                  "squatDay":"TUESDAY",
                                  "deadliftDay":"THURSDAY",
                                  "shoulderPressDay":"FRIDAY"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void createWorkoutScheduleReturnsBadRequestWhenValidationFails() throws Exception {
        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");

        mockMvc.perform(post("/api/users/uid-123/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "benchDay":"MONDAY",
                                  "squatDay":"TUESDAY",
                                  "deadliftDay":"THURSDAY",
                                  "shoulderPressDay":"FRIDAY"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void getWorkoutScheduleReturnsNotFoundWhenMissing() throws Exception {
        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");
        when(workoutScheduleService.getWorkoutScheduleByUser("uid-123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/uid-123/schedule"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getWorkoutScheduleReturnsForbiddenWhenUidMismatch() throws Exception {
        doThrow(new AccessDeniedException("You are not allowed to access this user's data"))
                .when(authenticatedUserGuard)
                .requireUidMatches("uid-other");

        mockMvc.perform(get("/api/users/uid-other/schedule"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
