package com.graysoncraw.ggainsbackend.dto.workoutcycle;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WorkoutCycleResponseDTO {
    private Long id;
    private String firebaseUid;
    private Integer cycleNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double benchTrainingMax;
    private Double squatTrainingMax;
    private Double deadliftTrainingMax;
    private Double shoulderPressTrainingMax;
    private String benchDay;
    private String squatDay;
    private String deadliftDay;
    private String shoulderPressDay;
    private Boolean isActive;
}
