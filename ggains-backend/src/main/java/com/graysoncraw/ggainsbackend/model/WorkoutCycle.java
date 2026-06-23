package com.graysoncraw.ggainsbackend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "workout_cycles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "firebase_uid", nullable = false)
    private User user;

    @Column(name = "cycle_number", nullable = false)
    @NotNull(message = "Cycle number is required")
    @Min(value = 1, message = "Cycle number must be at least 1")
    private Integer cycleNumber;

    @Column(name = "start_date", nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @Column(name = "bench_training_max", nullable = false)
    @NotNull(message = "Bench training max is required")
    @Min(value = 0, message = "Bench training max must be positive")
    private Double benchTrainingMax;

    @Column(name = "squat_training_max", nullable = false)
    @NotNull(message = "Squat training max is required")
    @Min(value = 0, message = "Squat training max must be positive")
    private Double squatTrainingMax;

    @Column(name = "deadlift_training_max", nullable = false)
    @NotNull(message = "Deadlift training max is required")
    @Min(value = 0, message = "Deadlift training max must be positive")
    private Double deadliftTrainingMax;

    @Column(name = "shoulder_press_training_max", nullable = false)
    @NotNull(message = "Shoulder press training max is required")
    @Min(value = 0, message = "Shoulder press training max must be positive")
    private Double shoulderPressTrainingMax;

    @Column(name = "bench_completed")
    private Boolean benchCompleted;

    @Column(name = "squat_completed")
    private Boolean squatCompleted;

    @Column(name = "deadlift_completed")
    private Boolean deadliftCompleted;

    @Column(name = "shoulder_press_completed")
    private Boolean shoulderPressCompleted;

    @Enumerated(EnumType.STRING)
    @Column(name = "bench_day", nullable = false)
    @NotNull(message = "Bench day is required")
    private DayOfWeek benchDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "squat_day", nullable = false)
    @NotNull(message = "Squat day is required")
    private DayOfWeek squatDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "deadlift_day", nullable = false)
    @NotNull(message = "Deadlift day is required")
    private DayOfWeek deadliftDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "shoulder_press_day", nullable = false)
    @NotNull(message = "Shoulder press day is required")
    private DayOfWeek shoulderPressDay;

    @Column(name = "is_active", nullable = false)
    @NotNull(message = "Active status is required")
    private Boolean isActive;

    // Relationship
    @OneToMany(mappedBy = "workoutCycle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkoutSession> workoutSessions;
}
