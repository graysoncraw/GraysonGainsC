package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.workoutcycle.PrescribedWorkoutDTO;
import com.graysoncraw.ggainsbackend.dto.workoutcycle.WorkoutCycleOutcomeRequestDTO;
import com.graysoncraw.ggainsbackend.dto.workoutcycle.WorkoutCycleResponseDTO;
import com.graysoncraw.ggainsbackend.exception.GlobalExceptionHandler;
import com.graysoncraw.ggainsbackend.mapper.WorkoutCycleMapper;
import com.graysoncraw.ggainsbackend.model.LiftType;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import com.graysoncraw.ggainsbackend.security.AuthenticatedUserGuard;
import com.graysoncraw.ggainsbackend.service.WorkoutCycleService;
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

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkoutCycleController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration")
class WorkoutCycleControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkoutCycleService workoutCycleService;

    @MockitoBean
    private WorkoutCycleMapper workoutCycleMapper;

    @MockitoBean
    private AuthenticatedUserGuard authenticatedUserGuard;

    @Test
    void getActiveCycleReturnsForbiddenWhenUidMismatch() throws Exception {
        doThrow(new AccessDeniedException("You are not allowed to access this user's data"))
                .when(authenticatedUserGuard)
                .requireUidMatches("uid-other");

        mockMvc.perform(get("/api/users/uid-other/cycles/active"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getPrescribedWorkoutReturnsExpectedPayload() throws Exception {
        PrescribedWorkoutDTO prescribedWorkoutDTO = new PrescribedWorkoutDTO();
        prescribedWorkoutDTO.setDate(LocalDate.of(2026, 4, 21));
        prescribedWorkoutDTO.setWeekNumber(1);
        prescribedWorkoutDTO.setLiftType(LiftType.BENCH);
        prescribedWorkoutDTO.setTrainingMax(200.0);
        prescribedWorkoutDTO.setIsDeload(false);

        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");
        when(workoutCycleService.calculatePrescribedWorkout("uid-123", LocalDate.of(2026, 4, 21))).thenReturn(prescribedWorkoutDTO);

        mockMvc.perform(get("/api/users/uid-123/cycles/prescribed")
                        .param("date", "2026-04-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekNumber").value(1))
                .andExpect(jsonPath("$.liftType").value("BENCH"));
    }

    @Test
    void updateActiveCycleOutcomesReturnsMappedCycle() throws Exception {
        WorkoutCycle updatedCycle = WorkoutCycle.builder().id(9L).build();
        WorkoutCycleResponseDTO response = new WorkoutCycleResponseDTO();
        response.setId(9L);
        response.setFirebaseUid("uid-123");
        response.setBenchCompleted(true);
        response.setSquatCompleted(false);
        response.setDeadliftCompleted(false);
        response.setShoulderPressCompleted(true);

        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");
        when(workoutCycleService.updateActiveCycleOutcomes(any(String.class), any(WorkoutCycleOutcomeRequestDTO.class)))
                .thenReturn(updatedCycle);
        when(workoutCycleMapper.toResponse(updatedCycle)).thenReturn(response);

        mockMvc.perform(put("/api/users/uid-123/cycles/active/outcomes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "benchCompleted": true,
                                  "squatCompleted": false,
                                  "deadliftCompleted": false,
                                  "shoulderPressCompleted": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.benchCompleted").value(true))
                .andExpect(jsonPath("$.shoulderPressCompleted").value(true));
    }

    @Test
    void progressToNextCycleReturnsMappedCycle() throws Exception {
        WorkoutCycle cycle = WorkoutCycle.builder().id(12L).build();
        WorkoutCycleResponseDTO response = new WorkoutCycleResponseDTO();
        response.setId(12L);
        response.setFirebaseUid("uid-123");

        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");
        when(workoutCycleService.progressToNextCycle("uid-123")).thenReturn(cycle);
        when(workoutCycleMapper.toResponse(cycle)).thenReturn(response);

        mockMvc.perform(post("/api/users/uid-123/cycles/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12));
    }

    @Test
    void getCycleHistoryReturnsMappedResponses() throws Exception {
        WorkoutCycle cycle = WorkoutCycle.builder().id(1L).build();
        WorkoutCycleResponseDTO response = new WorkoutCycleResponseDTO();
        response.setId(1L);
        response.setFirebaseUid("uid-123");
        response.setCycleNumber(2);

        doNothing().when(authenticatedUserGuard).requireUidMatches("uid-123");
        when(workoutCycleService.getCycleHistory("uid-123")).thenReturn(List.of(cycle));
        when(workoutCycleMapper.toResponse(cycle)).thenReturn(response);

        mockMvc.perform(get("/api/users/uid-123/cycles/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].cycleNumber").value(2));
    }
}
