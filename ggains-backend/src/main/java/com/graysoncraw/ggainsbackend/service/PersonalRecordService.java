package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.model.PersonalRecord;
import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import com.graysoncraw.ggainsbackend.repository.PersonalRecordRepository;
import com.graysoncraw.ggainsbackend.repository.UserRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutCycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonalRecordService {

    private final UserRepository userRepository;
    private final PersonalRecordRepository personalRecordRepository;
    private final WorkoutCycleRepository workoutCycleRepository;
    private final WorkoutCycleService workoutCycleService;

    public PersonalRecord createPersonalRecord(String firebaseUid, PersonalRecord personalRecord) {
        User user = userRepository.findById(firebaseUid)
                .orElseThrow(() -> new NoSuchElementException("User with Firebase UID " + firebaseUid + " not found"));

        personalRecord.setUser(user);
        PersonalRecord saved = personalRecordRepository.save(personalRecord);

        return saved;
    }

    public Optional<PersonalRecord> getPersonalRecordByUserFirebaseUid(String firebaseUid) {
        return personalRecordRepository.findByUser_FirebaseUid(firebaseUid);
    }

    // For when a user hits a new PR
    public PersonalRecord updatePersonalRecord(PersonalRecord personalRecord) {
        if (!personalRecordRepository.existsById(personalRecord.getId())) {
            throw new NoSuchElementException("Personal record with ID " + personalRecord.getId() + " not found");
        }
        var activeWorkoutCycle = workoutCycleService.getActiveCycle(personalRecord.getUser().getFirebaseUid());
        activeWorkoutCycle.setBenchTrainingMax(workoutCycleService.calcTrainingMax(personalRecord.getBenchPressPR()));
        activeWorkoutCycle.setSquatTrainingMax(workoutCycleService.calcTrainingMax(personalRecord.getSquatPR()));
        activeWorkoutCycle.setDeadliftTrainingMax(workoutCycleService.calcTrainingMax(personalRecord.getDeadliftPR()));
        activeWorkoutCycle.setShoulderPressTrainingMax(workoutCycleService.calcTrainingMax(personalRecord.getShoulderPressPR()));
        workoutCycleRepository.save(activeWorkoutCycle);

        return personalRecordRepository.save(personalRecord);
    }
}
