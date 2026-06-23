package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.model.PersonalRecord;
import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import com.graysoncraw.ggainsbackend.repository.PersonalRecordRepository;
import com.graysoncraw.ggainsbackend.repository.UserRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutCycleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalRecordServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PersonalRecordRepository personalRecordRepository;
    @Mock
    private WorkoutCycleRepository workoutCycleRepository;
    @Mock
    private WorkoutCycleService workoutCycleService;

    @InjectMocks
    private PersonalRecordService personalRecordService;

    @Test
    void createPersonalRecordAssignsTheAuthenticatedUserAndSavesTheRecord() {
        String uid = "uid-123";
        User user = User.builder().firebaseUid(uid).build();
        PersonalRecord request = PersonalRecord.builder().benchPressPR(225.0).build();

        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(personalRecordRepository.save(any(PersonalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersonalRecord saved = personalRecordService.createPersonalRecord(uid, request);

        assertEquals(user, request.getUser());
        assertEquals(user, saved.getUser());
        verify(personalRecordRepository).save(request);
    }

    @Test
    void getPersonalRecordByUserFirebaseUidReturnsTheSavedRecordWhenPresent() {
        PersonalRecord record = PersonalRecord.builder().id(1L).build();
        when(personalRecordRepository.findByUser_FirebaseUid("uid-123")).thenReturn(Optional.of(record));

        Optional<PersonalRecord> loaded = personalRecordService.getPersonalRecordByUserFirebaseUid("uid-123");

        assertEquals(Optional.of(record), loaded);
    }

    @Test
    void getPersonalRecordByUserFirebaseUidReturnsEmptyWhenMissing() {
        when(personalRecordRepository.findByUser_FirebaseUid("uid-404")).thenReturn(Optional.empty());

        assertEquals(Optional.empty(), personalRecordService.getPersonalRecordByUserFirebaseUid("uid-404"));
    }

    @Test
    void updatePersonalRecordSyncsTheActiveCycleTrainingMaxes() {
        String uid = "uid-123";
        User user = User.builder().firebaseUid(uid).build();
        PersonalRecord request = PersonalRecord.builder()
                .id(8L)
                .user(user)
                .benchPressPR(230.0)
                .squatPR(345.0)
                .deadliftPR(390.0)
                .shoulderPressPR(140.0)
                .build();
        WorkoutCycle activeCycle = WorkoutCycle.builder()
                .id(5L)
                .user(user)
                .benchTrainingMax(200.0)
                .squatTrainingMax(300.0)
                .deadliftTrainingMax(350.0)
                .shoulderPressTrainingMax(120.0)
                .build();

        when(personalRecordRepository.existsById(8L)).thenReturn(true);
        when(workoutCycleService.getActiveCycle(uid)).thenReturn(activeCycle);
        when(workoutCycleService.calcTrainingMax(230.0)).thenReturn(205.0);
        when(workoutCycleService.calcTrainingMax(345.0)).thenReturn(310.0);
        when(workoutCycleService.calcTrainingMax(390.0)).thenReturn(350.0);
        when(workoutCycleService.calcTrainingMax(140.0)).thenReturn(125.0);
        when(workoutCycleRepository.save(any(WorkoutCycle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(personalRecordRepository.save(any(PersonalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PersonalRecord saved = personalRecordService.updatePersonalRecord(request);

        assertEquals(205.0, activeCycle.getBenchTrainingMax());
        assertEquals(310.0, activeCycle.getSquatTrainingMax());
        assertEquals(350.0, activeCycle.getDeadliftTrainingMax());
        assertEquals(125.0, activeCycle.getShoulderPressTrainingMax());
        assertEquals(request, saved);
        verify(workoutCycleRepository).save(activeCycle);
        verify(personalRecordRepository).save(request);
    }
}
