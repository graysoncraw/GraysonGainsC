package com.graysoncraw.ggainsbackend.dto.workoutschedule;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Data
public class WorkoutScheduleRequestDTO {

    @NotNull(message = "Cycle start date is required")
    private LocalDate cycleStartDate;

    @NotNull(message = "Bench day is required")
    private DayOfWeek benchDay;

    @NotNull(message = "Squat day is required")
    private DayOfWeek squatDay;

    @NotNull(message = "Deadlift day is required")
    private DayOfWeek deadliftDay;

    @NotNull(message = "Shoulder press day is required")
    private DayOfWeek shoulderPressDay;
}
