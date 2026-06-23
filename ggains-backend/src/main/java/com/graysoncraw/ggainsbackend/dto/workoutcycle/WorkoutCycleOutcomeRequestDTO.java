package com.graysoncraw.ggainsbackend.dto.workoutcycle;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WorkoutCycleOutcomeRequestDTO {
    @NotNull(message = "Bench completion status is required")
    private Boolean benchCompleted;

    @NotNull(message = "Squat completion status is required")
    private Boolean squatCompleted;

    @NotNull(message = "Deadlift completion status is required")
    private Boolean deadliftCompleted;

    @NotNull(message = "Shoulder press completion status is required")
    private Boolean shoulderPressCompleted;
}
