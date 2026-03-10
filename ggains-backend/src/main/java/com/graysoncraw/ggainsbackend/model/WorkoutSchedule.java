package com.graysoncraw.ggainsbackend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Entity
@Table(name = "workout_schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkoutSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "firebase_uid", nullable = false, unique = true)
    private User user;

    @Column(name = "cycle_start_date", nullable = false)
    @NotNull(message = "Cycle start date is required")
    private LocalDate cycleStartDate;

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
}
