package com.graysoncraw.ggainsbackend.repository;

import com.graysoncraw.ggainsbackend.model.WorkoutExercise;
import com.graysoncraw.ggainsbackend.model.WorkoutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutExerciseRepository extends JpaRepository<WorkoutExercise, Long> {

    List<WorkoutExercise> findByWorkoutSession(WorkoutSession workoutSession);
    List<WorkoutExercise> findByWorkoutSession_Id(Long workoutSessionId);
    List<WorkoutExercise> findByWorkoutSessionAndIsMainLiftTrue(WorkoutSession workoutSession);
}
