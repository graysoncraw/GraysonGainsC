package com.graysoncraw.ggainsbackend.service;

import com.graysoncraw.ggainsbackend.dto.workoutcycle.PrescribedSetDTO;
import com.graysoncraw.ggainsbackend.dto.workoutcycle.PrescribedWorkoutDTO;
import com.graysoncraw.ggainsbackend.model.LiftType;
import com.graysoncraw.ggainsbackend.model.PersonalRecord;
import com.graysoncraw.ggainsbackend.model.User;
import com.graysoncraw.ggainsbackend.model.WorkoutCycle;
import com.graysoncraw.ggainsbackend.repository.PersonalRecordRepository;
import com.graysoncraw.ggainsbackend.repository.UserRepository;
import com.graysoncraw.ggainsbackend.repository.WorkoutCycleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutCycleService {

    private final WorkoutCycleRepository workoutCycleRepository;
    private final PersonalRecordRepository personalRecordRepository;
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

    public WorkoutCycle createFirstCycle(String firebaseUid, WorkoutCycle workoutCycle) {
        if (!workoutCycleRepository.findByUser_FirebaseUid(firebaseUid).isEmpty()) {
            throw new IllegalStateException("First cycle already exists for user");
        }

        User user = userRepository.findById(firebaseUid)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        PersonalRecord pr = personalRecordRepository.findByUser_FirebaseUid(firebaseUid)
                .orElseThrow(() -> new NoSuchElementException("Personal records not found for user"));

        double benchTM = calcTrainingMax(pr.getBenchPressPR());
        double squatTM = calcTrainingMax(pr.getSquatPR());
        double deadliftTM = calcTrainingMax(pr.getDeadliftPR());
        double shoulderPressTM = calcTrainingMax(pr.getShoulderPressPR());

        LocalDate startDate = workoutCycle.getStartDate();
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
                .benchCompleted(false)
                .squatCompleted(false)
                .deadliftCompleted(false)
                .shoulderPressCompleted(false)
                .benchDay(workoutCycle.getBenchDay())
                .squatDay(workoutCycle.getSquatDay())
                .deadliftDay(workoutCycle.getDeadliftDay())
                .shoulderPressDay(workoutCycle.getShoulderPressDay())
                .isActive(true)
                .build();

        return workoutCycleRepository.save(cycle);
    }

    public WorkoutCycle getActiveCycle(String firebaseUid) {
        return workoutCycleRepository.findByUser_FirebaseUidAndIsActiveTrue(firebaseUid)
                .orElseThrow(() -> new NoSuchElementException("No active workout cycle found for user"));
    }

    public PrescribedWorkoutDTO calculatePrescribedWorkout(String firebaseUid, LocalDate date) {
        WorkoutCycle cycle = getActiveCycle(firebaseUid);

        if (date.isBefore(cycle.getStartDate())) {
            throw new IllegalArgumentException("Date is before cycle start date");
        }
        if (date.isAfter(cycle.getEndDate())) {
            throw new IllegalStateException("Current cycle has expired. Please progress to the next cycle.");
        }

        int weekNumber = calculateWeekNumber(cycle.getStartDate(), date);

        if (weekNumber > 4) {
            throw new IllegalStateException("Current cycle has expired. Please progress to the next cycle.");
        }

        LiftType todaysLift = getLiftForDay(cycle, date.getDayOfWeek());

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

    public WorkoutCycle progressToNextCycle(String firebaseUid) {
        WorkoutCycle currentCycle = getActiveCycle(firebaseUid);

        PersonalRecord pr = personalRecordRepository.findByUser_FirebaseUid(firebaseUid)
                .orElseThrow(() -> new NoSuchElementException("Personal records not found for user"));

        User user = userRepository.findById(firebaseUid)
                .orElseThrow(() -> new NoSuchElementException("User not found"));

        pr.setBenchPressPR(progressedPersonalRecord(pr.getBenchPressPR(), 5, Boolean.TRUE.equals(currentCycle.getBenchCompleted())));
        pr.setSquatPR(progressedPersonalRecord(pr.getSquatPR(), 10, Boolean.TRUE.equals(currentCycle.getSquatCompleted())));
        pr.setDeadliftPR(progressedPersonalRecord(pr.getDeadliftPR(), 10, Boolean.TRUE.equals(currentCycle.getDeadliftCompleted())));
        pr.setShoulderPressPR(progressedPersonalRecord(
                pr.getShoulderPressPR(),
                5,
                Boolean.TRUE.equals(currentCycle.getShoulderPressCompleted())
        ));
        personalRecordRepository.save(pr);

        LocalDate newStartDate = currentCycle.getEndDate().plusDays(1);
        LocalDate newEndDate = newStartDate.plusWeeks(4);

        // CRITICAL: Deactivate all existing cycles before creating new one
        deactivateAllCycles(firebaseUid);

        WorkoutCycle newCycle = WorkoutCycle.builder()
                .user(user)
                .cycleNumber(currentCycle.getCycleNumber() + 1)
                .startDate(newStartDate)
                .endDate(newEndDate)
                .benchTrainingMax(calcTrainingMax(pr.getBenchPressPR()))
                .squatTrainingMax(calcTrainingMax(pr.getSquatPR()))
                .deadliftTrainingMax(calcTrainingMax(pr.getDeadliftPR()))
                .shoulderPressTrainingMax(calcTrainingMax(pr.getShoulderPressPR()))
                .benchCompleted(false)
                .squatCompleted(false)
                .deadliftCompleted(false)
                .shoulderPressCompleted(false)
                .isActive(true)
                .build();

        return workoutCycleRepository.save(newCycle);
    }

    public List<WorkoutCycle> getCycleHistory(String firebaseUid) {
        return workoutCycleRepository.findByUser_FirebaseUidOrderByCycleNumberDesc(firebaseUid);
    }

    // For when a user hits a new PR
    public WorkoutCycle updateActiveWorkoutCycle(WorkoutCycle workoutCycle) {
        if (!workoutCycleRepository.existsById(workoutCycle.getId())) {
            throw new NoSuchElementException("Workout cycle with ID " + workoutCycle.getId() + " not found");
        }
        var activeCycle = getActiveCycle(workoutCycle.getUser().getFirebaseUid());
        activeCycle.setStartDate(workoutCycle.getStartDate());
        activeCycle.setBenchDay(workoutCycle.getBenchDay());
        activeCycle.setSquatDay(workoutCycle.getSquatDay());
        activeCycle.setDeadliftDay(workoutCycle.getDeadliftDay());
        activeCycle.setShoulderPressDay(workoutCycle.getShoulderPressDay());
        return workoutCycleRepository.save(activeCycle);
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

    private LiftType getLiftForDay(WorkoutCycle cycle, DayOfWeek dayOfWeek) {
        if (cycle.getBenchDay() == dayOfWeek) return LiftType.BENCH;
        if (cycle.getSquatDay() == dayOfWeek) return LiftType.SQUAT;
        if (cycle.getDeadliftDay() == dayOfWeek) return LiftType.DEADLIFT;
        if (cycle.getShoulderPressDay() == dayOfWeek) return LiftType.SHOULDER_PRESS;
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

    public double calcTrainingMax(double weight) {
        return roundToNearest5(weight * 0.90);
    }

    private double progressedPersonalRecord(double currentPersonalRecord, double increment, boolean successfulThisCycle) {
        if (!successfulThisCycle) {
            return currentPersonalRecord;
        }

        return roundToNearest5(currentPersonalRecord + increment);
    }
}
