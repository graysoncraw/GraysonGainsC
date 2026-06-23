package com.graysoncraw.ggainsbackend.dto.workoutsession;

import com.graysoncraw.ggainsbackend.dto.workoutexercise.WorkoutExerciseResponseDTO;
import com.graysoncraw.ggainsbackend.model.LiftType;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class WorkoutSessionResponseDTO {
    private Long id;
    private String firebaseUid;
    private Long workoutCycleId;
    private Integer cycleNumber;
    private LocalDate date;
    private LiftType mainLiftType;
    private Integer weekNumber;
    private String notes;
}
