package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.dto.CycleProgressRequestDTO;
import com.graysoncraw.ggainsbackend.dto.PrescribedSetDTO;
import com.graysoncraw.ggainsbackend.dto.PrescribedWorkoutDTO;
import com.graysoncraw.ggainsbackend.model.LiftType;
import com.graysoncraw.ggainsbackend.model.PersonalRecord;
import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import com.graysoncraw.ggainsbackend.model.WorkoutSchedule;
import com.graysoncraw.ggainsbackend.repository.PersonalRecordRepository;
import com.graysoncraw.ggainsbackend.repository.UserRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutCycleRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutCycleService {

    private final WorkoutCycleRepository workoutCycleRepository;
    private final PersonalRecordRepository personalRecordRepository;
    private final WorkoutScheduleRepository workoutScheduleRepository;
    private final UserRepository userRepository;

    private static final double[][] WEEK_PERCENTAGES = {
            {0.65, 0.75, 0.85},  // Week 1
            {0.70, 0.80, 0.90},  // Week 2
            {0.75, 0.85, 0.95},  // Week 3
            {0.40, 0.50, 0.60}   // Week 4 (Deload)
    };

    private static final int[][] WEEK_REPS = {
            {5, 5, 5},  // Week 1 (last set is 5+)
            {3, 3, 3},  // Week 2 (last set is 3+)
            {5, 3, 1},  // Week 3 (last set is 1+)
            {5, 5, 5}   // Week 4 Deload (no + sets)
    };

    public WorkoutCycle createFirstCycle(String firebaseUid) {
        if (!workoutCycleRepository.findByUser_FirebaseUid(firebaseUid).isEmpty()) {
            throw new IllegalStateException("First cycle already exists for user");
        }

        User user = userRepository.findById(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PersonalRecord pr = personalRecordRepository.findByUser_FirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("Personal records not found for user"));

        WorkoutSchedule schedule = workoutScheduleRepository.findByUser_FirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("Workout schedule not found for user"));

        double benchTM = roundToNearest5(pr.getBenchPressPR() * 0.90);
        double squatTM = roundToNearest5(pr.getSquatPR() * 0.90);
        double deadliftTM = roundToNearest5(pr.getDeadliftPR() * 0.90);
        double shoulderPressTM = roundToNearest5(pr.getShoulderPressPR() * 0.90);

        LocalDate startDate = schedule.getCycleStartDate();
        LocalDate endDate = startDate.plusWeeks(4);

        deactivateAllCycles(firebaseUid);

        WorkoutCycle cycle = WorkoutCycle.builder()
                .user(user)
                .cycleNumber(1)
                .startDate(startDate)
                .endDate(endDate)
                .benchTrainingMax(benchTM)
                .squatTrainingMax(squatTM)
                .deadliftTrainingMax(deadliftTM)
                .shoulderPressTrainingMax(shoulderPressTM)
                .isActive(true)
                .build();

        return workoutCycleRepository.save(cycle);
    }

    public WorkoutCycle getActiveCycle(String firebaseUid) {
        return workoutCycleRepository.findByUser_FirebaseUidAndIsActiveTrue(firebaseUid)
                .orElseThrow(() -> new IllegalStateException("No active workout cycle found for user"));
    }

    public PrescribedWorkoutDTO calculatePrescribedWorkout(String firebaseUid, LocalDate date) {
        WorkoutCycle cycle = getActiveCycle(firebaseUid);

        if (date.isBefore(cycle.getStartDate())) {
            throw new IllegalArgumentException("Date is before cycle start date");
        }
        if (date.isAfter(cycle.getEndDate())) {
            throw new IllegalStateException("Current cycle has expired. Please progress to the next cycle.");
        }

        WorkoutSchedule schedule = workoutScheduleRepository.findByUser_FirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("Workout schedule not found"));

        int weekNumber = calculateWeekNumber(cycle.getStartDate(), date);

        if (weekNumber > 4) {
            throw new IllegalStateException("Current cycle has expired. Please progress to the next cycle.");
        }

        LiftType todaysLift = getLiftForDay(schedule, date.getDayOfWeek());

        if (todaysLift == null) {
            throw new IllegalArgumentException("No lift scheduled for " + date.getDayOfWeek());
        }

        double trainingMax = getTrainingMaxForLift(cycle, todaysLift);

        // Calculate the prescribed sets
        var workout = new PrescribedWorkoutDTO();
        workout.setDate(date);
        workout.setWeekNumber(weekNumber);
        workout.setLiftType(todaysLift);
        workout.setTrainingMax(trainingMax);
        workout.setSets(calculateSets(trainingMax, weekNumber));
        workout.setIsDeload(weekNumber == 4);

        return workout;
    }

    public WorkoutCycle progressToNextCycle(String firebaseUid, CycleProgressRequestDTO request) {
        WorkoutCycle currentCycle = getActiveCycle(firebaseUid);

        User user = userRepository.findById(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Map<LiftType, Boolean> outcomes = validateAndExtractOutcomes(request);

        double newBenchTM = progressedTrainingMax(
                currentCycle.getBenchTrainingMax(),
                5,
                outcomes.get(LiftType.BENCH)
        );
        double newSquatTM = progressedTrainingMax(
                currentCycle.getSquatTrainingMax(),
                10,
                outcomes.get(LiftType.SQUAT)
        );
        double newDeadliftTM = progressedTrainingMax(
                currentCycle.getDeadliftTrainingMax(),
                10,
                outcomes.get(LiftType.DEADLIFT)
        );
        double newShoulderPressTM = progressedTrainingMax(
                currentCycle.getShoulderPressTrainingMax(),
                5,
                outcomes.get(LiftType.SHOULDER_PRESS)
        );

        LocalDate newStartDate = currentCycle.getEndDate().plusDays(1);
        LocalDate newEndDate = newStartDate.plusWeeks(4);

        // CRITICAL: Deactivate all existing cycles before creating new one
        deactivateAllCycles(firebaseUid);

        WorkoutCycle newCycle = WorkoutCycle.builder()
                .user(user)
                .cycleNumber(currentCycle.getCycleNumber() + 1)
                .startDate(newStartDate)
                .endDate(newEndDate)
                .benchTrainingMax(newBenchTM)
                .squatTrainingMax(newSquatTM)
                .deadliftTrainingMax(newDeadliftTM)
                .shoulderPressTrainingMax(newShoulderPressTM)
                .isActive(true)
                .build();

        return workoutCycleRepository.save(newCycle);
    }

    public List<WorkoutCycle> getCycleHistory(String firebaseUid) {
        return workoutCycleRepository.findByUser_FirebaseUidOrderByCycleNumberDesc(firebaseUid);
    }

    // ==================== Helper Methods ====================

    private void deactivateAllCycles(String firebaseUid) {
        List<WorkoutCycle> activeCycles = workoutCycleRepository.findByUser_FirebaseUid(firebaseUid);
        for (WorkoutCycle cycle : activeCycles) {
            if (cycle.getIsActive()) {
                cycle.setIsActive(false);
                workoutCycleRepository.save(cycle);
            }
        }
    }

    private int calculateWeekNumber(LocalDate startDate, LocalDate currentDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, currentDate);
        return (int) (daysBetween / 7) + 1;
    }

    private LiftType getLiftForDay(WorkoutSchedule schedule, DayOfWeek dayOfWeek) {
        if (schedule.getBenchDay() == dayOfWeek) return LiftType.BENCH;
        if (schedule.getSquatDay() == dayOfWeek) return LiftType.SQUAT;
        if (schedule.getDeadliftDay() == dayOfWeek) return LiftType.DEADLIFT;
        if (schedule.getShoulderPressDay() == dayOfWeek) return LiftType.SHOULDER_PRESS;
        return null;
    }

    private double getTrainingMaxForLift(WorkoutCycle cycle, LiftType liftType) {
        switch (liftType) {
            case BENCH:
                return cycle.getBenchTrainingMax();
            case SQUAT:
                return cycle.getSquatTrainingMax();
            case DEADLIFT:
                return cycle.getDeadliftTrainingMax();
            case SHOULDER_PRESS:
                return cycle.getShoulderPressTrainingMax();
            default:
                throw new IllegalArgumentException("Invalid lift type");
        }
    }

    private List<PrescribedSetDTO> calculateSets(double trainingMax, int weekNumber) {
        int weekIndex = weekNumber - 1;
        double[] percentages = WEEK_PERCENTAGES[weekIndex];
        int[] reps = WEEK_REPS[weekIndex];

        List<PrescribedSetDTO> sets = new java.util.ArrayList<>();

        for (int i = 0; i < 3; i++) {
            double weight = roundToNearest5(trainingMax * percentages[i]);
            int repCount = reps[i];
            boolean isAmrap = (weekNumber != 4 && i == 2);

            PrescribedSetDTO set = new PrescribedSetDTO();
            set.setSetNumber(i + 1);
            set.setWeight(weight);
            set.setReps(repCount);
            set.setIsAmrap(isAmrap);

            sets.add(set);
        }

        return sets;
    }

    private double roundToNearest5(double weight) {
        return Math.round(weight / 5) * 5;
    }

    private Map<LiftType, Boolean> validateAndExtractOutcomes(CycleProgressRequestDTO request) {
        if (request == null || request.getLiftOutcomes() == null) {
            throw new IllegalArgumentException("liftOutcomes is required");
        }

        Map<LiftType, Boolean> outcomes = request.getLiftOutcomes();
        for (LiftType liftType : EnumSet.allOf(LiftType.class)) {
            if (!outcomes.containsKey(liftType)) {
                throw new IllegalArgumentException("Missing lift outcome for " + liftType);
            }
            if (outcomes.get(liftType) == null) {
                throw new IllegalArgumentException("Lift outcome cannot be null for " + liftType);
            }
        }
        return outcomes;
    }

    private double progressedTrainingMax(double currentTrainingMax, double increment, boolean successfulThisCycle) {
        double nextTrainingMax = successfulThisCycle
                ? currentTrainingMax + increment
                : currentTrainingMax;
        return roundToNearest5(nextTrainingMax);
    }
}
