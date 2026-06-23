package com.graysoncraw.ggainsbackend.dto.workoutexercise;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkoutExerciseRequestDTO {
    @NotBlank(message = "Exercise name is required")
    private String exerciseName;

    @NotNull(message = "Weight is required")
    @Min(value = 0, message = "Weight must be positive")
    private Double weight;

    @NotNull(message = "Reps are required")
    @Min(value = 1, message = "Reps must be at least 1")
    private Integer reps;

    @NotNull(message = "Set number is required")
    @Min(value = 1, message = "Set number must be at least 1")
    private Integer setNumber;

    @NotNull(message = "Main lift flag is required")
    private Boolean isMainLift;
}
