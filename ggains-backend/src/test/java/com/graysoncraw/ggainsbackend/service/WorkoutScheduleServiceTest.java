package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.model.WorkoutSchedule;
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
class WorkoutScheduleServiceTest {

    @Mock
    private WorkoutScheduleRepository workoutScheduleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PersonalRecordRepository personalRecordRepository;
    @Mock
    private WorkoutCycleRepository workoutCycleRepository;
    @Mock
    private WorkoutCycleService workoutCycleService;

    @InjectMocks
    private WorkoutScheduleService workoutScheduleService;

    @Test
    void createWorkoutScheduleAutoCreatesFirstCycleWhenPersonalRecordExistsAndNoCycle() {
        String uid = "uid-123";
        User user = User.builder().firebaseUid(uid).build();
        WorkoutSchedule request = WorkoutSchedule.builder().build();

        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(workoutScheduleRepository.save(any(WorkoutSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(personalRecordRepository.findByUser_FirebaseUid(uid)).thenReturn(Optional.of(new com.graysoncraw.ggainsbackend.model.PersonalRecord()));
        when(workoutCycleRepository.findByUser_FirebaseUid(uid)).thenReturn(List.of());

        workoutScheduleService.createWorkoutSchedule(uid, request);

        assertEquals(user, request.getUser());
        verify(workoutCycleService).createFirstCycle(uid);
    }

    @Test
    void createWorkoutScheduleDoesNotAutoCreateFirstCycleWhenPersonalRecordMissing() {
        String uid = "uid-123";
        User user = User.builder().firebaseUid(uid).build();
        WorkoutSchedule request = WorkoutSchedule.builder().build();

        when(userRepository.findById(uid)).thenReturn(Optional.of(user));
        when(workoutScheduleRepository.save(any(WorkoutSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(personalRecordRepository.findByUser_FirebaseUid(uid)).thenReturn(Optional.empty());

        workoutScheduleService.createWorkoutSchedule(uid, request);

        verify(workoutCycleService, never()).createFirstCycle(uid);
    }

    @Test
    void updateWorkoutScheduleThrowsWhenScheduleMissing() {
        WorkoutSchedule request = WorkoutSchedule.builder().id(88L).build();
        when(workoutScheduleRepository.existsById(88L)).thenReturn(false);

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> workoutScheduleService.updateWorkoutSchedule(request)
        );

        assertEquals("Workout schedule not found", exception.getMessage());
    }
}
