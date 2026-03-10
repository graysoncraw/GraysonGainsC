package com.graysoncraw.ggainsbackend.repository;

import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkoutCycleRepository extends JpaRepository<WorkoutCycle, Long> {

    List<WorkoutCycle> findByUser(User user);
    List<WorkoutCycle> findByUser_FirebaseUid(String firebaseUid);
    Optional<WorkoutCycle> findByUserAndIsActiveTrue(User user);
    Optional<WorkoutCycle> findByUser_FirebaseUidAndIsActiveTrue(String firebaseUid);
    List<WorkoutCycle> findByUser_FirebaseUidOrderByCycleNumberDesc(String firebaseUid);
}
