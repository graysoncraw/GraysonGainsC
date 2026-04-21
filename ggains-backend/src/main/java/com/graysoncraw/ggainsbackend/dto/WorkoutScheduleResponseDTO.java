package com.graysoncraw.ggainsbackend.dto;

import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Data
public class WorkoutScheduleResponseDTO {
    private Long id;
    private String firebaseUid;
    private LocalDate cycleStartDate;
    private DayOfWeek benchDay;
    private DayOfWeek squatDay;
    private DayOfWeek deadliftDay;
    private DayOfWeek shoulderPressDay;
}
