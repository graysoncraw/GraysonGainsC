package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.dto.workoutexercise.WorkoutExerciseRequestDTO;
import com.graysoncraw.ggainsbackend.dto.workoutsession.WorkoutSessionRequestDTO;
import com.graysoncraw.ggainsbackend.model.LiftType;
import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import com.graysoncraw.ggainsbackend.model.WorkoutExercise;
import com.graysoncraw.ggainsbackend.model.WorkoutSession;
import com.graysoncraw.ggainsbackend.repository.UserRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutCycleRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutSessionRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutSessionServiceTest {

    @Mock
    private WorkoutSessionRepository workoutSessionRepository;

    @Mock
    private WorkoutCycleRepository workoutCycleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WorkoutSessionService workoutSessionService;

    @Test
    void upsertWorkoutSessionPersistsExercisesAndSnapshotsCycleData() {
        String firebaseUid = "uid-123";
        User user = User.builder().firebaseUid(firebaseUid).build();
        WorkoutCycle cycle = WorkoutCycle.builder()
                .id(44L)
                .user(user)
                .cycleNumber(3)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 28))
                .isActive(true)
                .build();

        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO();
        request.setDate(LocalDate.of(2026, 6, 15));
        request.setMainLiftType(LiftType.BENCH);
        request.setWeekNumber(2);
        request.setNotes("Felt good");

        WorkoutExerciseRequestDTO mainSet = new WorkoutExerciseRequestDTO();
        mainSet.setExerciseName("Bench Press");
        mainSet.setWeight(185.0);
        mainSet.setReps(3);
        mainSet.setSetNumber(1);
        mainSet.setIsMainLift(true);

        WorkoutExerciseRequestDTO accessorySet = new WorkoutExerciseRequestDTO();
        accessorySet.setExerciseName("Triceps Pushdown");
        accessorySet.setWeight(45.0);
        accessorySet.setReps(12);
        accessorySet.setSetNumber(1);
        accessorySet.setIsMainLift(false);

        request.setExercises(List.of(mainSet, accessorySet));

        when(userRepository.findById(firebaseUid)).thenReturn(Optional.of(user));
        when(workoutCycleRepository.findActiveCycleForDate(
                firebaseUid,
                request.getDate()
        )).thenReturn(Optional.of(cycle));
        when(workoutSessionRepository.findByUser_FirebaseUidAndDate(firebaseUid, request.getDate())).thenReturn(Optional.empty());
        when(workoutSessionRepository.save(any(WorkoutSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = workoutSessionService.upsertWorkoutSession(firebaseUid, request);

        assertEquals(firebaseUid, response.getFirebaseUid());
        assertEquals(44L, response.getWorkoutCycleId());
        assertEquals(3, response.getCycleNumber());
        assertEquals(2, response.getWeekNumber());
    }

    @Test
    void upsertWorkoutSessionReusesExistingCollectionWhenUpdatingSession() {
        String firebaseUid = "uid-123";
        User user = User.builder().firebaseUid(firebaseUid).build();
        WorkoutCycle cycle = WorkoutCycle.builder()
                .id(44L)
                .user(user)
                .cycleNumber(3)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 28))
                .isActive(true)
                .build();

        WorkoutSession existingSession = WorkoutSession.builder()
                .id(99L)
                .user(user)
                .workoutCycle(cycle)
                .date(LocalDate.of(2026, 6, 15))
                .mainLiftType(LiftType.BENCH)
                .weekNumber(2)
                .notes("Old note")
                .exercises(new java.util.ArrayList<>())
                .build();

        WorkoutExercise existingExercise = WorkoutExercise.builder()
                .exerciseName("Old Bench Press")
                .weight(175.0)
                .reps(5)
                .setNumber(1)
                .isMainLift(true)
                .workoutSession(existingSession)
                .build();
        existingSession.getExercises().add(existingExercise);

        WorkoutSessionRequestDTO request = new WorkoutSessionRequestDTO();
        request.setDate(LocalDate.of(2026, 6, 15));
        request.setMainLiftType(LiftType.BENCH);
        request.setWeekNumber(2);
        request.setNotes("Updated");

        WorkoutExerciseRequestDTO updatedMainSet = new WorkoutExerciseRequestDTO();
        updatedMainSet.setExerciseName("Bench Press");
        updatedMainSet.setWeight(185.0);
        updatedMainSet.setReps(3);
        updatedMainSet.setSetNumber(1);
        updatedMainSet.setIsMainLift(true);

        request.setExercises(List.of(updatedMainSet));

        when(userRepository.findById(firebaseUid)).thenReturn(Optional.of(user));
        when(workoutCycleRepository.findActiveCycleForDate(
                firebaseUid,
                request.getDate())
        ).thenReturn(Optional.of(cycle));
        when(workoutSessionRepository.findByUser_FirebaseUidAndDate(firebaseUid, request.getDate())).thenReturn(Optional.of(existingSession));
        when(workoutSessionRepository.save(any(WorkoutSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = workoutSessionService.upsertWorkoutSession(firebaseUid, request);

        assertEquals("Updated", response.getNotes());
    }

    @Test
    void getWorkoutSessionThrowsWhenMissing() {
        when(workoutSessionRepository.findByUser_FirebaseUidAndDate("uid", LocalDate.of(2026, 6, 15)))
                .thenReturn(Optional.empty());

        assertThrows(
                java.util.NoSuchElementException.class,
                () -> workoutSessionService.getWorkoutSession("uid", LocalDate.of(2026, 6, 15))
        );
    }
}
