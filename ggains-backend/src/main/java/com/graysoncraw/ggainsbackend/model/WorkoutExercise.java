package com.graysoncraw.ggainsbackend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "workout_exercises")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workout_session_id", nullable = false)
    private WorkoutSession workoutSession;

    @Column(name = "exercise_name", nullable = false)
    @NotBlank(message = "Exercise name is required")
    private String exerciseName;

    @Column(name = "weight", nullable = false)
    @NotNull(message = "Weight is required")
    @Min(value = 0, message = "Weight must be positive")
    private Double weight;

    @Column(name = "reps", nullable = false)
    @NotNull(message = "Reps is required")
    @Min(value = 1, message = "Reps must be at least 1")
    private Integer reps;

    @Column(name = "set_number", nullable = false)
    @NotNull(message = "Set number is required")
    @Min(value = 1, message = "Set number must be at least 1")
    private Integer setNumber;

    @Column(name = "is_main_lift", nullable = false)
    @NotNull(message = "Main lift status is required")
    private Boolean isMainLift;
}
