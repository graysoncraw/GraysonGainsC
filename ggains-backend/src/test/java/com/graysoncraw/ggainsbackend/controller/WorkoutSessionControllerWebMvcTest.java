package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.workoutexercise.WorkoutExerciseResponseDTO;
import com.graysoncraw.ggainsbackend.dto.workoutsession.WorkoutSessionResponseDTO;
import com.graysoncraw.ggainsbackend.exception.GlobalExceptionHandler;
import com.graysoncraw.ggainsbackend.model.LiftType;
import com.graysoncraw.ggainsbackend.security.AuthenticatedUserGuard;
import com.graysoncraw.ggainsbackend.service.WorkoutSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkoutSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration")
class WorkoutSessionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkoutSessionService workoutSessionService;

    @MockitoBean
    private AuthenticatedUserGuard authenticatedUserGuard;

    @Test
    void getWorkoutSessionReturnsExpectedPayload() throws Exception {
        WorkoutSessionResponseDTO response = new WorkoutSessionResponseDTO();
        response.setId(9L);
        response.setFirebaseUid("uid-123");
        response.setWorkoutCycleId(44L);
        response.setCycleNumber(3);
        response.setDate(LocalDate.of(2026, 6, 15));
        response.setMainLiftType(LiftType.BENCH);
        response.setWeekNumber(2);

        WorkoutExerciseResponseDTO exercise = new WorkoutExerciseResponseDTO();
        exercise.setId(1L);
        exercise.setExerciseName("Bench Press");
        exercise.setWeight(185.0);
        exercise.setReps(3);
        exercise.setSetNumber(1);
        exercise.setIsMainLift(true);

        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");
        when(workoutSessionService.getWorkoutSession("uid-123", LocalDate.of(2026, 6, 15))).thenReturn(response);

        mockMvc.perform(get("/api/users/uid-123/workout-sessions/2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void upsertWorkoutSessionReturnsMappedResponse() throws Exception {
        WorkoutSessionResponseDTO response = new WorkoutSessionResponseDTO();
        response.setId(11L);
        response.setFirebaseUid("uid-123");

        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");
        when(workoutSessionService.upsertWorkoutSession(any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/users/uid-123/workout-sessions/2026-06-15")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mainLiftType": "BENCH",
                                  "weekNumber": 2,
                                  "notes": "solid",
                                  "exercises": [
                                    {
                                      "exerciseName": "Bench Press",
                                      "weight": 185.0,
                                      "reps": 3,
                                      "setNumber": 1,
                                      "isMainLift": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11));
    }
}
