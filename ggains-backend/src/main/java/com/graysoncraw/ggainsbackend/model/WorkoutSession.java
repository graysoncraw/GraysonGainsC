package com.graysoncraw.ggainsbackend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "workout_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "firebase_uid", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "workout_cycle_id", nullable = false)
    private WorkoutCycle workoutCycle;

    @Column(name = "date", nullable = false)
    @NotNull(message = "Workout date is required")
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "main_lift_type", nullable = false)
    @NotNull(message = "Main lift type is required")
    private LiftType mainLiftType;

    @Column(name = "week_number", nullable = false)
    @NotNull(message = "Week number is required")
    @Min(value = 1, message = "Week number must be between 1 and 4")
    @Max(value = 4, message = "Week number must be between 1 and 4")
    private Integer weekNumber;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "workoutSession", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutExercise> exercises;
}
