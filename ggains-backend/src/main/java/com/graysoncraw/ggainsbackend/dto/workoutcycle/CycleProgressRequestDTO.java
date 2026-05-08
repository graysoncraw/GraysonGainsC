package com.graysoncraw.ggainsbackend.dto.workoutcycle;

import com.graysoncraw.ggainsbackend.model.LiftType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CycleProgressRequestDTO {

    @NotNull(message = "liftOutcomes is required")
    private Map<LiftType, Boolean> liftOutcomes;
}
