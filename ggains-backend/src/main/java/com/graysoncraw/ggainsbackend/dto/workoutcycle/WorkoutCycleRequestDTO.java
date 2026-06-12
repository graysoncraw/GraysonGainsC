package com.graysoncraw.ggainsbackend.dto.workoutcycle;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WorkoutCycleRequestDTO {
    private LocalDate startDate;
    private String benchDay;
    private String squatDay;
    private String deadliftDay;
    private String shoulderPressDay;
}
