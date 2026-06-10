package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.model.PersonalRecord;
import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.repository.PersonalRecordRepository;
import com.graysoncraw.ggainsbackend.repository.UserRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutCycleRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutScheduleRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalRecordServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PersonalRecordRepository personalRecordRepository;
    @Mock
    private WorkoutScheduleRepository workoutScheduleRepository;
    @Mock
    private WorkoutCycleRepository workoutCycleRepository;
    @Mock
    private WorkoutCycleService workoutCycleService;

    @InjectMocks
    private PersonalRecordService personalRecordService;

    @Test
    void createPersonalRecordAutoCreatesFirstCycleWhenScheduleExistsAndNoCycle() {
        String uid = "uid-123";
        User user = User.builder().firebaseUid(uid).build();
        PersonalRecord request = PersonalRecord.builder().benchPressPR(225.0).build();

        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(personalRecordRepository.save(any(PersonalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutScheduleRepository.findByUser_FirebaseUid(uid)).thenReturn(Optional.of(new com.graysoncraw.ggainsbackend.model.WorkoutSchedule()));
        when(workoutCycleRepository.findByUser_FirebaseUid(uid)).thenReturn(List.of());

        personalRecordService.createPersonalRecord(uid, request);

        assertEquals(user, request.getUser());
        verify(workoutCycleService).createFirstCycle(uid);
    }

    @Test
    void createPersonalRecordDoesNotAutoCreateFirstCycleWhenPrerequisitesMissing() {
        String uid = "uid-123";
        User user = User.builder().firebaseUid(uid).build();
        PersonalRecord request = PersonalRecord.builder().benchPressPR(225.0).build();

        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(personalRecordRepository.save(any(PersonalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutScheduleRepository.findByUser_FirebaseUid(uid)).thenReturn(Optional.empty());

        personalRecordService.createPersonalRecord(uid, request);

        verify(workoutCycleService, never()).createFirstCycle(uid);
    }

    @Test
    void getPersonalRecordByUserFirebaseUidThrowsWhenMissing() {
        when(personalRecordRepository.findByUser_FirebaseUid("uid-404")).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> personalRecordService.getPersonalRecordByUserFirebaseUid("uid-404")
        );

        assertEquals("Personal record for user with Firebase UID uid-404 not found", exception.getMessage());
    }

    @Test
    void updateSpecificPRThrowsForInvalidLiftType() {
        PersonalRecord existing = PersonalRecord.builder().id(1L).benchPressPR(200.0).build();
        when(personalRecordRepository.findByUser_FirebaseUid("uid-123")).thenReturn(Optional.of(existing));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> personalRecordService.updateSpecificPR("uid-123", "ROW", 100.0)
        );

        assertEquals("Invalid lift type: ROW", exception.getMessage());
    }
}
