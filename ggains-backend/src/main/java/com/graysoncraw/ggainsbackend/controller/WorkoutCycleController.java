package com.graysoncraw.ggainsbackend.controller;

import com.graysoncraw.ggainsbackend.dto.PrescribedWorkoutDTO;
import com.graysoncraw.ggainsbackend.dto.WorkoutCycleResponseDTO;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import com.graysoncraw.ggainsbackend.service.WorkoutCycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/active")
    public WorkoutCycleResponseDTO getActiveCycle(@PathVariable String firebaseUid) {
        WorkoutCycle cycle = workoutCycleService.getActiveCycle(firebaseUid);
        return toResponse(cycle);
    }

    @GetMapping("/history")
    public List<WorkoutCycleResponseDTO> getCycleHistory(@PathVariable String firebaseUid) {
        return workoutCycleService.getCycleHistory(firebaseUid).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/prescribed")
    public PrescribedWorkoutDTO getPrescribedWorkout(
            @PathVariable String firebaseUid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return workoutCycleService.calculatePrescribedWorkout(firebaseUid, date);
    }

    @PostMapping("/progress")
    public WorkoutCycleResponseDTO progressToNextCycle(@PathVariable String firebaseUid) {
        WorkoutCycle cycle = workoutCycleService.progressToNextCycle(firebaseUid);
        return toResponse(cycle);
    }

    private WorkoutCycleResponseDTO toResponse(WorkoutCycle cycle) {
        WorkoutCycleResponseDTO response = new WorkoutCycleResponseDTO();
        response.setId(cycle.getId());
        response.setFirebaseUid(cycle.getUser().getFirebaseUid());
        response.setCycleNumber(cycle.getCycleNumber());
        response.setStartDate(cycle.getStartDate());
        response.setEndDate(cycle.getEndDate());
        response.setBenchTrainingMax(cycle.getBenchTrainingMax());
        response.setSquatTrainingMax(cycle.getSquatTrainingMax());
        response.setDeadliftTrainingMax(cycle.getDeadliftTrainingMax());
        response.setShoulderPressTrainingMax(cycle.getShoulderPressTrainingMax());
        response.setIsActive(cycle.getIsActive());
        return response;
    }
}
