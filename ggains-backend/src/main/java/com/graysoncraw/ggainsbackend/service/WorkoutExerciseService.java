package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.dto.workoutexercise.WorkoutExerciseResponseDTO;
import com.graysoncraw.ggainsbackend.model.WorkoutExercise;

public class WorkoutExerciseService {

    private WorkoutExerciseResponseDTO toResponse(WorkoutExercise exercise) {
        WorkoutExerciseResponseDTO response = new WorkoutExerciseResponseDTO();
        response.setId(exercise.getId());
        response.setExerciseName(exercise.getExerciseName());
        response.setWeight(exercise.getWeight());
        response.setReps(exercise.getReps());
        response.setSetNumber(exercise.getSetNumber());
        response.setIsMainLift(exercise.getIsMainLift());
        return response;
    }
}
