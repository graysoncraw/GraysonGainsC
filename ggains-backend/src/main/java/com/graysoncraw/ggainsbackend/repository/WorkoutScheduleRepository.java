package com.graysoncraw.ggainsbackend.repository;

import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.model.WorkoutSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkoutScheduleRepository extends JpaRepository<WorkoutSchedule, Long> {

    Optional<WorkoutSchedule> findByUser(User user);
    Optional<WorkoutSchedule> findByUser_FirebaseUid(String firebaseUid);
}
