package com.graysoncraw.ggainsbackend.repository;

import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import com.graysoncraw.ggainsbackend.model.WorkoutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutSessionRepository extends JpaRepository<WorkoutSession, Long> {

    List<WorkoutSession> findByUser(User user);
    List<WorkoutSession> findByUser_FirebaseUid(String firebaseUid);
    List<WorkoutSession> findByWorkoutCycle(WorkoutCycle workoutCycle);
    List<WorkoutSession> findByUser_FirebaseUidAndDateBetween(String firebaseUid, LocalDate startDate, LocalDate endDate);
    Optional<WorkoutSession> findByUser_FirebaseUidAndDate(String firebaseUid, LocalDate date);
}
