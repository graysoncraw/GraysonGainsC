package com.graysoncraw.ggainsbackend.dto;

import com.graysoncraw.ggainsbackend.model.LiftType;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PrescribedWorkoutDTO {
    private LocalDate date;
    private Integer weekNumber;
    private LiftType liftType;
    private Double trainingMax;
    private List<PrescribedSetDTO> sets;
    private Boolean isDeload;
}
