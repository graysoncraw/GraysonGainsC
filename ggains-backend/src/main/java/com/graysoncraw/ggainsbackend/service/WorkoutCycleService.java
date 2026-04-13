package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.dto.PrescribedSetDTO;
import com.graysoncraw.ggainsbackend.dto.PrescribedWorkoutDTO;
import com.graysoncraw.ggainsbackend.model.*;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutCycleService {

    private final WorkoutCycleRepository workoutCycleRepository;
    private final PersonalRecordRepository personalRecordRepository;
    private final WorkoutScheduleRepository workoutScheduleRepository;
    private final UserRepository userRepository;

    // 5-3-1 percentages and reps for each week
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

    /**
     * Create the first workout cycle for a user
     */
    public WorkoutCycle createFirstCycle(String firebaseUid) {
        if (!workoutCycleRepository.findByUser_FirebaseUid(firebaseUid).isEmpty()) {
            throw new IllegalStateException("First cycle already exists for user");
        }

        // Verify user exists
        User user = userRepository.findById(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get user's PRs
        PersonalRecord pr = personalRecordRepository.findByUser_FirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("Personal records not found for user"));

        // Get user's workout schedule for start date
        WorkoutSchedule schedule = workoutScheduleRepository.findByUser_FirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("Workout schedule not found for user"));

        // Calculate training maxes (90% of PRs)
        double benchTM = roundToNearest5(pr.getBenchPressPR() * 0.90);
        double squatTM = roundToNearest5(pr.getSquatPR() * 0.90);
        double deadliftTM = roundToNearest5(pr.getDeadliftPR() * 0.90);
        double shoulderPressTM = roundToNearest5(pr.getShoulderPressPR() * 0.90);

        // Calculate end date (4 weeks from start)
        LocalDate startDate = schedule.getCycleStartDate();
        LocalDate endDate = startDate.plusWeeks(4);

        // Deactivate any existing active cycles (shouldn't exist for first cycle, but safety check)
        deactivateAllCycles(firebaseUid);

        // Create the cycle
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

    /**
     * Get the active workout cycle for a user
     */
    public WorkoutCycle getActiveCycle(String firebaseUid) {
        return workoutCycleRepository.findByUser_FirebaseUidAndIsActiveTrue(firebaseUid)
                .orElseThrow(() -> new IllegalStateException("No active workout cycle found for user"));
    }

    /**
     * Calculate the prescribed workout for a given date
     */
    public PrescribedWorkoutDTO calculatePrescribedWorkout(String firebaseUid, LocalDate date) {
        // Get active cycle
        WorkoutCycle cycle = getActiveCycle(firebaseUid);

        // Get workout schedule
        WorkoutSchedule schedule = workoutScheduleRepository.findByUser_FirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("Workout schedule not found"));

        // Determine which week we're in (1-4)
        int weekNumber = calculateWeekNumber(cycle.getStartDate(), date);

        if (weekNumber > 4) {
            throw new IllegalStateException("Current cycle has expired. Please progress to the next cycle.");
        }

        // Determine which lift is scheduled for this day
        LiftType todaysLift = getLiftForDay(schedule, date.getDayOfWeek());

        if (todaysLift == null) {
            throw new IllegalArgumentException("No lift scheduled for " + date.getDayOfWeek());
        }

        // Get the training max for this lift
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

    /**
     * Progress to the next workout cycle
     */
    public WorkoutCycle progressToNextCycle(String firebaseUid) {
        // Get current active cycle
        WorkoutCycle currentCycle = getActiveCycle(firebaseUid);

        // Verify user exists
        User user = userRepository.findById(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Calculate new training maxes with progression
        // Upper body (bench, shoulder press): +5 lbs
        // Lower body (squat, deadlift): +10 lbs
        double newBenchTM = roundToNearest5(currentCycle.getBenchTrainingMax() + 5);
        double newSquatTM = roundToNearest5(currentCycle.getSquatTrainingMax() + 10);
        double newDeadliftTM = roundToNearest5(currentCycle.getDeadliftTrainingMax() + 10);
        double newShoulderPressTM = roundToNearest5(currentCycle.getShoulderPressTrainingMax() + 5);

        // New cycle starts today
        LocalDate newStartDate = currentCycle.getEndDate().plusDays(1);
        LocalDate newEndDate = newStartDate.plusWeeks(4);

        // CRITICAL: Deactivate all existing cycles before creating new one
        deactivateAllCycles(firebaseUid);

        // Create new cycle
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

    /**
     * Get all workout cycles for a user, ordered by cycle number (most recent first)
     */
    public List<WorkoutCycle> getCycleHistory(String firebaseUid) {
        return workoutCycleRepository.findByUser_FirebaseUidOrderByCycleNumberDesc(firebaseUid);
    }

    // ==================== Helper Methods ====================

    /**
     * Deactivate all cycles for a user
     */
    private void deactivateAllCycles(String firebaseUid) {
        List<WorkoutCycle> activeCycles = workoutCycleRepository.findByUser_FirebaseUid(firebaseUid);
        for (WorkoutCycle cycle : activeCycles) {
            if (cycle.getIsActive()) {
                cycle.setIsActive(false);
                workoutCycleRepository.save(cycle);
            }
        }
    }

    /**
     * Calculate which week (1-4) we're in based on start date and current date
     */
    private int calculateWeekNumber(LocalDate startDate, LocalDate currentDate) {
        long daysBetween = ChronoUnit.DAYS.between(startDate, currentDate);
        return (int) (daysBetween / 7) + 1;
    }

    /**
     * Determine which lift is scheduled for a given day of the week
     */
    private LiftType getLiftForDay(WorkoutSchedule schedule, DayOfWeek dayOfWeek) {
        if (schedule.getBenchDay() == dayOfWeek) return LiftType.BENCH;
        if (schedule.getSquatDay() == dayOfWeek) return LiftType.SQUAT;
        if (schedule.getDeadliftDay() == dayOfWeek) return LiftType.DEADLIFT;
        if (schedule.getShoulderPressDay() == dayOfWeek) return LiftType.SHOULDER_PRESS;
        return null;
    }

    /**
     * Get the training max for a specific lift from the cycle
     */
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

    /**
     * Calculate the 3 sets for a given training max and week number
     */
    private List<PrescribedSetDTO> calculateSets(double trainingMax, int weekNumber) {
        int weekIndex = weekNumber - 1;  // Convert to 0-based index
        double[] percentages = WEEK_PERCENTAGES[weekIndex];
        int[] reps = WEEK_REPS[weekIndex];

        var sets = new java.util.ArrayList<PrescribedSetDTO>();

        for (int i = 0; i < 3; i++) {
            double weight = roundToNearest5(trainingMax * percentages[i]);
            int repCount = reps[i];
            boolean isAmrap = (weekNumber != 4 && i == 2);  // Last set is AMRAP except on deload week

            var set = new PrescribedSetDTO();
            set.setSetNumber(i + 1);
            set.setWeight(weight);
            set.setReps(repCount);
            set.setIsAmrap(isAmrap);  // "As Many Reps As Possible"

            sets.add(set);
        }

        return sets;
    }

    /**
     * Round weight to nearest 5 lbs
     */
    private double roundToNearest5(double weight) {
        return Math.round(weight / 5) * 5;
    }
}
