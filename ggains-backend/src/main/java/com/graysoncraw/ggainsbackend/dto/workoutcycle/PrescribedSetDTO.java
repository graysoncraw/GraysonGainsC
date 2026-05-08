package com.graysoncraw.ggainsbackend.dto.workoutcycle;

import lombok.Data;

@Data
public class PrescribedSetDTO {
    private Integer setNumber;
    private Double weight;
    private Integer reps;
    private Boolean isAmrap;
}
