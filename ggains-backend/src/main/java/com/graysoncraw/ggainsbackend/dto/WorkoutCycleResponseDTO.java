package com.graysoncraw.ggainsbackend.dto;

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
    private Boolean isActive;
}
