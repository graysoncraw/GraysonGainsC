package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.model.WorkoutSchedule;
import com.graysoncraw.ggainsbackend.repository.PersonalRecordRepository;
import com.graysoncraw.ggainsbackend.repository.UserRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutCycleRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutScheduleService {

    private final WorkoutScheduleRepository workoutScheduleRepository;
    private final UserRepository userRepository;
    private final PersonalRecordRepository personalRecordRepository;
    private final WorkoutCycleRepository workoutCycleRepository;
    private final WorkoutCycleService workoutCycleService;

    public WorkoutSchedule createWorkoutSchedule(String firebaseUid, WorkoutSchedule workoutSchedule) {
        User user = userRepository.findById(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        workoutSchedule.setUser(user);
        WorkoutSchedule saved = workoutScheduleRepository.save(workoutSchedule);

        // Automatically create first cycle once both onboarding prerequisites exist.
        if (personalRecordRepository.findByUser_FirebaseUid(firebaseUid).isPresent()
                && workoutCycleRepository.findByUser_FirebaseUid(firebaseUid).isEmpty()) {
            workoutCycleService.createFirstCycle(firebaseUid);
        }

        return saved;
    }

    public Optional<WorkoutSchedule> getWorkoutScheduleByUser(String firebaseUid) {
        return workoutScheduleRepository.findByUser_FirebaseUid(firebaseUid);
    }

    public WorkoutSchedule updateWorkoutSchedule(WorkoutSchedule workoutSchedule) {
        if (!workoutScheduleRepository.existsById(workoutSchedule.getId())) {
            throw new IllegalArgumentException("Workout schedule not found");
        }
        return workoutScheduleRepository.save(workoutSchedule);
    }
}
