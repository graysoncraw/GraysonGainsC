package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.personalrecord.PersonalRecordRequestDTO;
import com.graysoncraw.ggainsbackend.dto.personalrecord.PersonalRecordResponseDTO;
import com.graysoncraw.ggainsbackend.dto.workoutcycle.PrescribedWorkoutDTO;
import com.graysoncraw.ggainsbackend.dto.workoutcycle.WorkoutCycleOutcomeRequestDTO;
import com.graysoncraw.ggainsbackend.dto.workoutcycle.WorkoutCycleRequestDTO;
import com.graysoncraw.ggainsbackend.dto.workoutcycle.WorkoutCycleResponseDTO;
import com.graysoncraw.ggainsbackend.mapper.WorkoutCycleMapper;
import com.graysoncraw.ggainsbackend.model.PersonalRecord;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import com.graysoncraw.ggainsbackend.security.AuthenticatedUserGuard;
import com.graysoncraw.ggainsbackend.service.WorkoutCycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/users/{firebaseUid}/cycles")
@RequiredArgsConstructor
public class WorkoutCycleController {

    private final WorkoutCycleService workoutCycleService;
    private final WorkoutCycleMapper workoutCycleMapper;
    private final AuthenticatedUserGuard authenticatedUserGuard;

    @PostMapping
    public ResponseEntity<WorkoutCycleResponseDTO> createWorkoutCycle(
            @PathVariable String firebaseUid,
            @Valid @RequestBody WorkoutCycleRequestDTO request
    ) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        WorkoutCycle workoutCycle = workoutCycleMapper.toEntity(request);

        WorkoutCycle createdCycle = workoutCycleService.createFirstCycle(firebaseUid, workoutCycle);
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutCycleMapper.toResponse(createdCycle));
    }

    @PutMapping
    public WorkoutCycleResponseDTO updateActiveWorkoutCycle(
            @PathVariable String firebaseUid,
            @Valid @RequestBody WorkoutCycleRequestDTO request
    ) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        WorkoutCycle existingRecord = workoutCycleService.getActiveCycle(firebaseUid);
        WorkoutCycle workoutCycle = workoutCycleMapper.toEntity(request);
        workoutCycle.setId(existingRecord.getId());
        workoutCycle.setUser(existingRecord.getUser());

        WorkoutCycle updatedRecord = workoutCycleService.updateActiveWorkoutCycle(workoutCycle);
        return workoutCycleMapper.toResponse(updatedRecord);
    }

    @PutMapping("/active/outcomes")
    public WorkoutCycleResponseDTO updateActiveCycleOutcomes(
            @PathVariable String firebaseUid,
            @Valid @RequestBody WorkoutCycleOutcomeRequestDTO request
    ) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        WorkoutCycle updatedCycle = workoutCycleService.updateActiveCycleOutcomes(firebaseUid, request);
        return workoutCycleMapper.toResponse(updatedCycle);
    }

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
            @PathVariable String firebaseUid
    ) {
        authenticatedUserGuard.requireUidMatches(firebaseUid);
        WorkoutCycle cycle = workoutCycleService.progressToNextCycle(firebaseUid);
        return workoutCycleMapper.toResponse(cycle);
    }
}
