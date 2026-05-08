package com.graysoncraw.ggainsbackend.dto.personalrecord;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PersonalRecordRequestDTO {

    @NotNull(message = "Bench press PR is required")
    @Min(value = 0, message = "Bench press PR must be positive")
    private Double benchPressPR;

    @NotNull(message = "Squat PR is required")
    @Min(value = 0, message = "Squat PR must be positive")
    private Double squatPR;

    @NotNull(message = "Deadlift PR is required")
    @Min(value = 0, message = "Deadlift PR must be positive")
    private Double deadliftPR;

    @NotNull(message = "Shoulder press PR is required")
    @Min(value = 0, message = "Shoulder press PR must be positive")
    private Double shoulderPressPR;
}
