package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.workoutsession.WorkoutSessionRequestDTO;
import com.graysoncraw.ggainsbackend.dto.workoutsession.WorkoutSessionResponseDTO;
import com.graysoncraw.ggainsbackend.security.AuthenticatedUserGuard;
import com.graysoncraw.ggainsbackend.service.WorkoutSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users/{firebaseUid}/workout-sessions")
@RequiredArgsConstructor
public class WorkoutSessionController {

    private final WorkoutSessionService workoutSessionService;
    private final AuthenticatedUserGuard authenticatedUserGuard;

    @GetMapping("/{date}")
    public WorkoutSessionResponseDTO getWorkoutSession(
            @PathVariable String firebaseUid,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        return workoutSessionService.getWorkoutSession(firebaseUid, date);
    }

    @GetMapping
    public List<WorkoutSessionResponseDTO> getWorkoutHistory(@PathVariable String firebaseUid) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        return workoutSessionService.getWorkoutHistory(firebaseUid);
    }

    @PutMapping("/{date}")
    public WorkoutSessionResponseDTO upsertWorkoutSession(
            @PathVariable String firebaseUid,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody WorkoutSessionRequestDTO request
    ) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        request.setDate(date);
        return workoutSessionService.upsertWorkoutSession(firebaseUid, request);
    }
}
