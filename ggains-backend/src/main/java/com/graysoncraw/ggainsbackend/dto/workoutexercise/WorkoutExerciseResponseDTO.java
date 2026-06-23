package com.graysoncraw.ggainsbackend.dto.workoutexercise;

import lombok.Data;

@Data
public class WorkoutExerciseResponseDTO {
    private Long id;
    private String exerciseName;
    private Double weight;
    private Integer reps;
    private Integer setNumber;
    private Boolean isMainLift;
}
