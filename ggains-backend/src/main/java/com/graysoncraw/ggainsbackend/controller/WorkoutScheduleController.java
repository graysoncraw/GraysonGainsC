package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.WorkoutScheduleRequestDTO;
import com.graysoncraw.ggainsbackend.dto.WorkoutScheduleResponseDTO;
import com.graysoncraw.ggainsbackend.mapper.WorkoutScheduleMapper;
import com.graysoncraw.ggainsbackend.model.WorkoutSchedule;
import com.graysoncraw.ggainsbackend.service.WorkoutScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/users/{firebaseUid}/schedule")
@RequiredArgsConstructor
public class WorkoutScheduleController {

    private final WorkoutScheduleService workoutScheduleService;
    private final WorkoutScheduleMapper workoutScheduleMapper;

    @PostMapping
    public ResponseEntity<WorkoutScheduleResponseDTO> createWorkoutSchedule(
            @PathVariable String firebaseUid,
            @Valid @RequestBody WorkoutScheduleRequestDTO request
    ) {
        WorkoutSchedule workoutSchedule = workoutScheduleMapper.toEntity(request);

        WorkoutSchedule createdSchedule = workoutScheduleService.createWorkoutSchedule(firebaseUid, workoutSchedule);
        return ResponseEntity.status(HttpStatus.CREATED).body(workoutScheduleMapper.toResponse(createdSchedule));
    }

    @GetMapping
    public WorkoutScheduleResponseDTO getWorkoutSchedule(@PathVariable String firebaseUid) {
        WorkoutSchedule schedule = workoutScheduleService.getWorkoutScheduleByUser(firebaseUid)
                .orElseThrow(() -> new NoSuchElementException("Workout schedule not found for user " + firebaseUid));
        return workoutScheduleMapper.toResponse(schedule);
    }

    @PutMapping
    public WorkoutScheduleResponseDTO updateWorkoutSchedule(
            @PathVariable String firebaseUid,
            @Valid @RequestBody WorkoutScheduleRequestDTO request
    ) {
        WorkoutSchedule existingSchedule = workoutScheduleService.getWorkoutScheduleByUser(firebaseUid)
                .orElseThrow(() -> new NoSuchElementException("Workout schedule not found for user " + firebaseUid));

        WorkoutSchedule scheduleToUpdate = workoutScheduleMapper.toEntity(request);
        scheduleToUpdate.setId(existingSchedule.getId());
        scheduleToUpdate.setUser(existingSchedule.getUser());

        WorkoutSchedule updatedSchedule = workoutScheduleService.updateWorkoutSchedule(scheduleToUpdate);
        return workoutScheduleMapper.toResponse(updatedSchedule);
    }
}
