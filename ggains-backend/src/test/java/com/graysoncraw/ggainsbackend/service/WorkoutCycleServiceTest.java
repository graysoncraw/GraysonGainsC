package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.dto.CycleProgressRequestDTO;
import com.graysoncraw.ggainsbackend.model.LiftType;
import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import com.graysoncraw.ggainsbackend.repository.PersonalRecordRepository;
import com.graysoncraw.ggainsbackend.repository.UserRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutCycleRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutScheduleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutCycleServiceTest {

    @Mock
    private WorkoutCycleRepository workoutCycleRepository;
    @Mock
    private PersonalRecordRepository personalRecordRepository;
    @Mock
    private WorkoutScheduleRepository workoutScheduleRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkoutCycleService workoutCycleService;

    @Test
    void progressToNextCycleProgressesOnlySuccessfulLifts() {
        String firebaseUid = "uid-123";
        User user = User.builder().firebaseUid(firebaseUid).build();
        WorkoutCycle currentCycle = WorkoutCycle.builder()
                .user(user)
                .cycleNumber(2)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 4, 29))
                .benchTrainingMax(200.0)
                .squatTrainingMax(300.0)
                .deadliftTrainingMax(350.0)
                .shoulderPressTrainingMax(120.0)
                .isActive(true)
                .build();

        Map<LiftType, Boolean> outcomes = new EnumMap<>(LiftType.class);
        outcomes.put(LiftType.BENCH, true);
        outcomes.put(LiftType.SQUAT, false);
        outcomes.put(LiftType.DEADLIFT, false);
        outcomes.put(LiftType.SHOULDER_PRESS, true);

        CycleProgressRequestDTO request = new CycleProgressRequestDTO();
        request.setLiftOutcomes(outcomes);

        when(workoutCycleRepository.findByUser_FirebaseUidAndIsActiveTrue(firebaseUid)).thenReturn(Optional.of(currentCycle));
        when(workoutCycleRepository.findByUser_FirebaseUid(firebaseUid)).thenReturn(List.of(currentCycle));
        when(userRepository.findById(firebaseUid)).thenReturn(Optional.of(user));
        when(workoutCycleRepository.save(any(WorkoutCycle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkoutCycle nextCycle = workoutCycleService.progressToNextCycle(firebaseUid, request);

        assertEquals(3, nextCycle.getCycleNumber());
        assertEquals(LocalDate.of(2026, 4, 30), nextCycle.getStartDate());
        assertEquals(LocalDate.of(2026, 5, 28), nextCycle.getEndDate());
        assertEquals(205.0, nextCycle.getBenchTrainingMax());
        assertEquals(300.0, nextCycle.getSquatTrainingMax());
        assertEquals(350.0, nextCycle.getDeadliftTrainingMax());
        assertEquals(125.0, nextCycle.getShoulderPressTrainingMax());
        assertFalse(currentCycle.getIsActive());

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
    void progressToNextCycleThrowsWhenLiftOutcomeMissing() {
        String firebaseUid = "uid-123";
        User user = User.builder().firebaseUid(firebaseUid).build();
        WorkoutCycle currentCycle = WorkoutCycle.builder()
                .user(user)
                .cycleNumber(1)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 4, 29))
                .benchTrainingMax(200.0)
                .squatTrainingMax(300.0)
                .deadliftTrainingMax(350.0)
                .shoulderPressTrainingMax(120.0)
                .isActive(true)
                .build();

        Map<LiftType, Boolean> outcomes = new EnumMap<>(LiftType.class);
        outcomes.put(LiftType.BENCH, true);
        outcomes.put(LiftType.SQUAT, true);
        outcomes.put(LiftType.SHOULDER_PRESS, true);

        CycleProgressRequestDTO request = new CycleProgressRequestDTO();
        request.setLiftOutcomes(outcomes);

        when(workoutCycleRepository.findByUser_FirebaseUidAndIsActiveTrue(firebaseUid)).thenReturn(Optional.of(currentCycle));
        when(userRepository.findById(firebaseUid)).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> workoutCycleService.progressToNextCycle(firebaseUid, request)
        );

        assertEquals("Missing lift outcome for DEADLIFT", exception.getMessage());
    }
}
