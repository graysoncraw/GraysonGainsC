package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.model.PersonalRecord;
import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.repository.PersonalRecordRepository;
import com.graysoncraw.ggainsbackend.repository.UserRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutCycleRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonalRecordService {

    private final UserRepository userRepository;
    private final PersonalRecordRepository personalRecordRepository;
    private final WorkoutScheduleRepository workoutScheduleRepository;
    private final WorkoutCycleRepository workoutCycleRepository;
    private final WorkoutCycleService workoutCycleService;

    public PersonalRecord createPersonalRecord(String firebaseUid, PersonalRecord personalRecord) {
        User user = userRepository.findById(firebaseUid)
                // By using orElseThrow, we can set this Optional user obj to a personal record
                .orElseThrow(() -> new IllegalArgumentException("User with Firebase UID " + firebaseUid + " not found"));

        personalRecord.setUser(user);
        PersonalRecord saved = personalRecordRepository.save(personalRecord);

        // Automatically create first cycle once both onboarding prerequisites exist.
        if (workoutScheduleRepository.findByUser_FirebaseUid(firebaseUid).isPresent()
                && workoutCycleRepository.findByUser_FirebaseUid(firebaseUid).isEmpty()) {
            workoutCycleService.createFirstCycle(firebaseUid);
        }

        return saved;
    }

    public PersonalRecord getPersonalRecordByUserFirebaseUid(String firebaseUid) {
        return personalRecordRepository.findByUser_FirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("Personal record for user with Firebase UID " + firebaseUid + " not found"));

    }

    // For when a user hits a new PR
    public PersonalRecord updatePersonalRecord(PersonalRecord personalRecord) {
        if (!personalRecordRepository.existsById(personalRecord.getId())) {
            throw new IllegalArgumentException("Personal record with ID " + personalRecord.getId() + " not found");
        }
        return personalRecordRepository.save(personalRecord);
    }

    public PersonalRecord updateSpecificPR(String firebaseUid, String liftType, Double newPR) {
        PersonalRecord pr = personalRecordRepository.findByUser_FirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("Personal record for user with Firebase UID " + firebaseUid + " not found"));

        switch (liftType.toUpperCase()) {
            case "BENCH":
                pr.setBenchPressPR(newPR);
                break;
            case "SQUAT":
                pr.setSquatPR(newPR);
                break;
            case "DEADLIFT":
                pr.setDeadliftPR(newPR);
                break;
            case "SHOULDER_PRESS":
                pr.setShoulderPressPR(newPR);
                break;
            default:
                throw new IllegalArgumentException("Invalid lift type: " + liftType);
        }

        return personalRecordRepository.save(pr);
    }
}
