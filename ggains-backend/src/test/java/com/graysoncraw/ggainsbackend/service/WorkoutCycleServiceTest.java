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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutCycleServiceTest {

    @Mock
    private WorkoutCycleRepository workoutCycleRepository;
    @Mock
    private PersonalRecordRepository personalRecordRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkoutCycleService workoutCycleService;

    @Test
    void progressToNextCycleProgressesOnlySuccessfulLifts() {
        String firebaseUid = "uid-123";
        User user = User.builder().firebaseUid(firebaseUid).build();
        PersonalRecord personalRecord = PersonalRecord.builder()
                .id(10L)
                .user(user)
                .benchPressPR(220.0)
                .squatPR(335.0)
                .deadliftPR(390.0)
                .shoulderPressPR(135.0)
                .build();
        WorkoutCycle currentCycle = WorkoutCycle.builder()
                .user(user)
                .cycleNumber(2)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 4, 29))
                .benchTrainingMax(200.0)
                .squatTrainingMax(300.0)
                .deadliftTrainingMax(350.0)
                .shoulderPressTrainingMax(120.0)
                .benchCompleted(true)
                .squatCompleted(false)
                .deadliftCompleted(false)
                .shoulderPressCompleted(true)
                .isActive(true)
                .build();

        when(workoutCycleRepository.findByUser_FirebaseUidAndIsActiveTrue(firebaseUid)).thenReturn(Optional.of(currentCycle));
        when(workoutCycleRepository.findByUser_FirebaseUid(firebaseUid)).thenReturn(List.of(currentCycle));
        when(userRepository.findById(firebaseUid)).thenReturn(Optional.of(user));
        when(personalRecordRepository.findByUser_FirebaseUid(firebaseUid)).thenReturn(Optional.of(personalRecord));
        when(personalRecordRepository.save(any(PersonalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutCycleRepository.save(any(WorkoutCycle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutCycle nextCycle = workoutCycleService.progressToNextCycle(firebaseUid);

        assertEquals(3, nextCycle.getCycleNumber());
        assertEquals(LocalDate.of(2026, 4, 30), nextCycle.getStartDate());
        assertEquals(LocalDate.of(2026, 5, 28), nextCycle.getEndDate());
        assertEquals(205.0, nextCycle.getBenchTrainingMax());
        assertEquals(300.0, nextCycle.getSquatTrainingMax());
        assertEquals(350.0, nextCycle.getDeadliftTrainingMax());
        assertEquals(125.0, nextCycle.getShoulderPressTrainingMax());
        assertFalse(currentCycle.getIsActive());

        verify(personalRecordRepository).save(argThat(saved ->
                saved.getBenchPressPR().equals(225.0)
                        && saved.getSquatPR().equals(335.0)
                        && saved.getDeadliftPR().equals(390.0)
                        && saved.getShoulderPressPR().equals(140.0)
        ));
        verify(workoutCycleRepository).save(currentCycle);
        verify(workoutCycleRepository).save(nextCycle);
    }

    @Test
    void calculatePrescribedWorkoutThrowsForDateBeforeCycleStart() {
        WorkoutCycle activeCycle = WorkoutCycle.builder()
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 4, 29))
                .isActive(true)
                .build();
        when(workoutCycleRepository.findByUser_FirebaseUidAndIsActiveTrue("uid")).thenReturn(Optional.of(activeCycle));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workoutCycleService.calculatePrescribedWorkout("uid", LocalDate.of(2026, 3, 31))
        );

        assertEquals("Date is before cycle start date", exception.getMessage());
    }

    @Test
    void calculatePrescribedWorkoutThrowsForDateAfterCycleEnd() {
        WorkoutCycle activeCycle = WorkoutCycle.builder()
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 4, 29))
                .isActive(true)
                .build();
        when(workoutCycleRepository.findByUser_FirebaseUidAndIsActiveTrue("uid")).thenReturn(Optional.of(activeCycle));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> workoutCycleService.calculatePrescribedWorkout("uid", LocalDate.of(2026, 5, 1))
        );

        assertEquals("Current cycle has expired. Please progress to the next cycle.", exception.getMessage());
    }

    @Test
    void progressToNextCycleUsesUncheckedFlagsAsFalse() {
        String firebaseUid = "uid-123";
        User user = User.builder().firebaseUid(firebaseUid).build();
        PersonalRecord personalRecord = PersonalRecord.builder()
                .id(12L)
                .user(user)
                .benchPressPR(225.0)
                .squatPR(315.0)
                .deadliftPR(405.0)
                .shoulderPressPR(135.0)
                .build();
        WorkoutCycle currentCycle = WorkoutCycle.builder()
                .user(user)
                .cycleNumber(1)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 4, 29))
                .benchTrainingMax(205.0)
                .squatTrainingMax(285.0)
                .deadliftTrainingMax(365.0)
                .shoulderPressTrainingMax(120.0)
                .benchCompleted(false)
                .squatCompleted(false)
                .deadliftCompleted(false)
                .shoulderPressCompleted(false)
                .isActive(true)
                .build();

        when(workoutCycleRepository.findByUser_FirebaseUidAndIsActiveTrue(firebaseUid)).thenReturn(Optional.of(currentCycle));
        when(workoutCycleRepository.findByUser_FirebaseUid(firebaseUid)).thenReturn(List.of(currentCycle));
        when(userRepository.findById(firebaseUid)).thenReturn(Optional.of(user));
        when(personalRecordRepository.findByUser_FirebaseUid(firebaseUid)).thenReturn(Optional.of(personalRecord));
        when(personalRecordRepository.save(any(PersonalRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(workoutCycleRepository.save(any(WorkoutCycle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutCycle nextCycle = workoutCycleService.progressToNextCycle(firebaseUid);

        assertEquals(2, nextCycle.getCycleNumber());
        assertEquals(205.0, nextCycle.getBenchTrainingMax());
        assertEquals(285.0, nextCycle.getSquatTrainingMax());
        assertEquals(365.0, nextCycle.getDeadliftTrainingMax());
        assertEquals(120.0, nextCycle.getShoulderPressTrainingMax());
        verify(personalRecordRepository).save(argThat(saved ->
                saved.getBenchPressPR().equals(225.0)
                        && saved.getSquatPR().equals(315.0)
                        && saved.getDeadliftPR().equals(405.0)
                        && saved.getShoulderPressPR().equals(135.0)
        ));
    }
}
