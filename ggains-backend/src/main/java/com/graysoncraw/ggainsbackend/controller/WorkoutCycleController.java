package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.workoutcycle.CycleProgressRequestDTO;
import com.graysoncraw.ggainsbackend.dto.workoutcycle.PrescribedWorkoutDTO;
import com.graysoncraw.ggainsbackend.dto.workoutcycle.WorkoutCycleResponseDTO;
import com.graysoncraw.ggainsbackend.mapper.WorkoutCycleMapper;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import com.graysoncraw.ggainsbackend.security.AuthenticatedUserGuard;
import com.graysoncraw.ggainsbackend.service.WorkoutCycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users/{firebaseUid}/cycles")
@RequiredArgsConstructor
public class WorkoutCycleController {

    private final WorkoutCycleService workoutCycleService;
    private final WorkoutCycleMapper workoutCycleMapper;
    private final AuthenticatedUserGuard authenticatedUserGuard;

    @GetMapping("/active")
    public WorkoutCycleResponseDTO getActiveCycle(@PathVariable String firebaseUid) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        WorkoutCycle cycle = workoutCycleService.getActiveCycle(firebaseUid);
        return workoutCycleMapper.toResponse(cycle);
    }

    @GetMapping("/history")
    public List<WorkoutCycleResponseDTO> getCycleHistory(@PathVariable String firebaseUid) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        return workoutCycleService.getCycleHistory(firebaseUid).stream()
                .map(workoutCycleMapper::toResponse)
                .toList();
    }

    @GetMapping("/prescribed")
    public PrescribedWorkoutDTO getPrescribedWorkout(
            @PathVariable String firebaseUid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        return workoutCycleService.calculatePrescribedWorkout(firebaseUid, date);
    }

    @PostMapping("/progress")
    public WorkoutCycleResponseDTO progressToNextCycle(
            @PathVariable String firebaseUid,
            @Valid @RequestBody CycleProgressRequestDTO request
    ) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        WorkoutCycle cycle = workoutCycleService.progressToNextCycle(firebaseUid, request);
        return workoutCycleMapper.toResponse(cycle);
    }
}
