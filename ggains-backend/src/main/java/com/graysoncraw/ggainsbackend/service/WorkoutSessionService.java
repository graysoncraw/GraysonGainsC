package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.dto.workoutsession.WorkoutExerciseRequestDTO;
import com.graysoncraw.ggainsbackend.dto.workoutsession.WorkoutExerciseResponseDTO;
import com.graysoncraw.ggainsbackend.dto.workoutsession.WorkoutSessionRequestDTO;
import com.graysoncraw.ggainsbackend.dto.workoutsession.WorkoutSessionResponseDTO;
import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import com.graysoncraw.ggainsbackend.model.WorkoutExercise;
import com.graysoncraw.ggainsbackend.model.WorkoutSession;
import com.graysoncraw.ggainsbackend.repository.UserRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutCycleRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutSessionService {

    private final WorkoutSessionRepository workoutSessionRepository;
    private final WorkoutCycleRepository workoutCycleRepository;
    private final UserRepository userRepository;

    public WorkoutSessionResponseDTO getWorkoutSession(String firebaseUid, LocalDate date) {
        return workoutSessionRepository.findByUser_FirebaseUidAndDate(firebaseUid, date)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("Workout session not found"));
    }

    public List<WorkoutSessionResponseDTO> getWorkoutHistory(String firebaseUid) {
        return workoutSessionRepository.findByUser_FirebaseUid(firebaseUid).stream()
                .sorted(Comparator.comparing(WorkoutSession::getDate).reversed())
                .map(this::toResponse)
                .toList();
    }

    public WorkoutSessionResponseDTO upsertWorkoutSession(String firebaseUid, WorkoutSessionRequestDTO request) {
        User user = userRepository.findById(firebaseUid)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        WorkoutCycle workoutCycle = workoutCycleRepository
                .findFirstByUser_FirebaseUidAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByCycleNumberDesc(
                        firebaseUid,
                        request.getDate(),
                        request.getDate()
                )
                .orElseThrow(() -> new NoSuchElementException("No workout cycle found for selected date"));

        WorkoutSession session = workoutSessionRepository.findByUser_FirebaseUidAndDate(firebaseUid, request.getDate())
                .orElseGet(WorkoutSession::new);

        session.setUser(user);
        session.setWorkoutCycle(workoutCycle);
        session.setDate(request.getDate());
        session.setMainLiftType(request.getMainLiftType());
        session.setWeekNumber(request.getWeekNumber());
        session.setNotes(request.getNotes());
        if (session.getExercises() == null) {
            session.setExercises(new ArrayList<>());
        } else {
            session.getExercises().clear();
        }

        for (WorkoutExerciseRequestDTO exerciseRequest : request.getExercises() == null ? List.<WorkoutExerciseRequestDTO>of() : request.getExercises()) {
            WorkoutExercise exercise = toEntity(exerciseRequest);
            exercise.setWorkoutSession(session);
            session.getExercises().add(exercise);
        }

        return toResponse(workoutSessionRepository.save(session));
    }

    private WorkoutExercise toEntity(WorkoutExerciseRequestDTO request) {
        WorkoutExercise exercise = new WorkoutExercise();
        exercise.setExerciseName(request.getExerciseName());
        exercise.setWeight(request.getWeight());
        exercise.setReps(request.getReps());
        exercise.setSetNumber(request.getSetNumber());
        exercise.setIsMainLift(request.getIsMainLift());
        return exercise;
    }

    private WorkoutSessionResponseDTO toResponse(WorkoutSession session) {
        WorkoutSessionResponseDTO response = new WorkoutSessionResponseDTO();
        response.setId(session.getId());
        response.setFirebaseUid(session.getUser().getFirebaseUid());
        response.setWorkoutCycleId(session.getWorkoutCycle().getId());
        response.setCycleNumber(session.getWorkoutCycle().getCycleNumber());
        response.setDate(session.getDate());
        response.setMainLiftType(session.getMainLiftType());
        response.setWeekNumber(session.getWeekNumber());
        response.setNotes(session.getNotes());
        response.setExercises(session.getExercises() == null ? List.of() : session.getExercises().stream()
                .sorted(Comparator.comparing(WorkoutExercise::getSetNumber))
                .map(this::toResponse)
                .toList());
        return response;
    }

    private WorkoutExerciseResponseDTO toResponse(WorkoutExercise exercise) {
        WorkoutExerciseResponseDTO response = new WorkoutExerciseResponseDTO();
        response.setId(exercise.getId());
        response.setExerciseName(exercise.getExerciseName());
        response.setWeight(exercise.getWeight());
        response.setReps(exercise.getReps());
        response.setSetNumber(exercise.getSetNumber());
        response.setIsMainLift(exercise.getIsMainLift());
        return response;
    }
}
