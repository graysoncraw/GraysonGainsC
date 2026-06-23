package com.graysoncraw.ggainsbackend.dto.workoutsession;

import com.graysoncraw.ggainsbackend.dto.workoutexercise.WorkoutExerciseRequestDTO;
import com.graysoncraw.ggainsbackend.model.LiftType;
import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class WorkoutSessionRequestDTO {
    private LocalDate date;
    private LiftType mainLiftType;
    private Integer weekNumber;
    private String notes;
    @Valid
    private List<WorkoutExerciseRequestDTO> exercises;
}
