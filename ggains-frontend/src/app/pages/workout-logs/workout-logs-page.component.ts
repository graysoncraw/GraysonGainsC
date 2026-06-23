import { Component, HostListener, computed, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SessionService } from '../../auth/session.service';
import { LiftType, PrescribedWorkout, WorkoutCycleApiService, WorkoutCycleResponse } from '../../api/workout-cycle-api.service';
import {
  WorkoutExerciseRequest,
  WorkoutSessionApiService,
  WorkoutSessionRequest,
  WorkoutSessionResponse,
} from '../../api/workout-session-api.service';

const WEEKDAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const DAY_TO_INDEX: Record<string, number> = {
  SUNDAY: 0,
  MONDAY: 1,
  TUESDAY: 2,
  WEDNESDAY: 3,
  THURSDAY: 4,
  FRIDAY: 5,
  SATURDAY: 6,
};

interface CalendarDay {
  isoDate: string;
  dayNumber: number;
  inCurrentMonth: boolean;
  hasWorkout: boolean;
  hasLog: boolean;
  isSelected: boolean;
  isToday: boolean;
}

interface ExerciseDraft {
  exerciseName: string;
  weight: string;
  reps: string;
  setNumber: string;
  isMainLift: boolean;
  completed: boolean;
}

@Component({
  selector: 'app-workout-logs-page',
  imports: [FormsModule],
  templateUrl: './workout-logs-page.component.html',
  styleUrl: './workout-logs-page.component.scss',
})
export class WorkoutLogsPageComponent {
  protected readonly activeCycle = signal<WorkoutCycleResponse | null>(null);
  protected readonly sessions = signal<WorkoutSessionResponse[]>([]);
  protected readonly selectedSession = signal<WorkoutSessionResponse | null>(null);
  protected readonly prescribedWorkout = signal<PrescribedWorkout | null>(null);
  protected readonly mainLiftDrafts = signal<ExerciseDraft[]>([]);
  protected readonly accessoryDrafts = signal<ExerciseDraft[]>([]);
  protected readonly notes = signal('');
  protected readonly loading = signal(true);
  protected readonly saving = signal(false);
  protected readonly error = signal('');
  protected readonly success = signal('');
  protected readonly selectedDate = signal(this.todayIso());
  protected readonly displayedMonth = signal(this.startOfMonthIso(this.todayIso()));

  protected readonly firebaseUid = computed(() => this.session.firebaseUid());
  protected readonly weekdayLabels = WEEKDAY_LABELS;
  protected readonly workoutDates = computed(() => this.getWorkoutDates(this.activeCycle()));
  protected readonly loggedDates = computed(() => this.sessions().map((session) => session.date));
  protected readonly monthLabel = computed(() => this.formatMonthLabel(this.displayedMonth()));
  protected readonly selectedDateLabel = computed(() => this.formatDateLabel(this.selectedDate()));
  protected readonly calendarDays = computed(() =>
    this.buildCalendarDays(this.displayedMonth(), this.workoutDates(), this.loggedDates(), this.selectedDate()),
  );
  protected readonly selectedSummary = computed(() => {
    const session = this.selectedSession();
    if (session) {
      return session.mainLiftType ? `Logged ${session.mainLiftType} · Cycle #${session.cycleNumber}` : `Logged workout · Cycle #${session.cycleNumber}`;
    }

    const prescribed = this.prescribedWorkout();
    if (prescribed) {
      return `${prescribed.liftType} · Week ${prescribed.weekNumber}`;
    }

    return 'No prescribed workout';
  });

  constructor(
    private readonly session: SessionService,
    private readonly workoutCycleApi: WorkoutCycleApiService,
    private readonly workoutSessionApi: WorkoutSessionApiService,
  ) {
    effect(() => {
      const firebaseUid = this.firebaseUid();
      if (firebaseUid) {
        void this.load(firebaseUid);
      }
    });
  }

  protected selectDate(date: string): void {
    this.selectedDate.set(date);
    this.displayedMonth.set(this.startOfMonthIso(date));
    void this.refreshSelectedDay();
  }

  protected changeMonth(offset: number): void {
    const month = this.parseIsoDate(this.displayedMonth());
    month.setUTCMonth(month.getUTCMonth() + offset);
    this.displayedMonth.set(this.startOfMonthIso(this.toIsoDate(month)));
  }

  protected addAccessoryDraft(): void {
    this.accessoryDrafts.update((current) => [
      ...current,
      {
        exerciseName: '',
        weight: '',
        reps: '',
        setNumber: `${current.length + 1}`,
        isMainLift: false,
        completed: false,
      },
    ]);
  }

  protected removeAccessoryDraft(index: number): void {
    this.accessoryDrafts.update((current) => current.filter((_, currentIndex) => currentIndex !== index));
  }

  protected async saveDayLog(): Promise<void> {
    const firebaseUid = this.firebaseUid();
    if (!firebaseUid) {
      return;
    }

    const accessoryValidationError = this.validateAccessoryDrafts();
    if (accessoryValidationError) {
      this.error.set(accessoryValidationError);
      return;
    }

    this.saving.set(true);
    this.error.set('');
    this.success.set('');

    try {
      const session = this.selectedSession();
      const prescribed = this.prescribedWorkout();
      const mainLiftExercises = this.mainLiftDrafts()
        .filter((exercise) => exercise.completed)
        .map((exercise) => this.toExerciseRequest(exercise, true));

      const request: WorkoutSessionRequest = {
        mainLiftType: session?.mainLiftType ?? prescribed?.liftType ?? null,
        weekNumber: session?.weekNumber ?? prescribed?.weekNumber ?? null,
        notes: this.notes().trim() || null,
        exercises: [
          ...mainLiftExercises,
          ...this.accessoryDrafts()
            .filter((exercise) => exercise.exerciseName.trim().length > 0)
            .map((exercise) => this.toExerciseRequest(exercise, false)),
        ],
      };

      await this.workoutSessionApi.upsertWorkoutSession(firebaseUid, this.selectedDate(), request);
      this.success.set('Workout log saved.');
      await this.load(firebaseUid);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to save workout log.');
    } finally {
      this.saving.set(false);
    }
  }

  protected async load(firebaseUid = this.firebaseUid()): Promise<void> {
    if (!firebaseUid) {
      return;
    }

    this.loading.set(true);
    this.error.set('');

    try {
      const [activeCycle, sessions] = await Promise.all([
        this.workoutCycleApi.getActiveCycle(firebaseUid).catch(() => null),
        this.workoutSessionApi.getWorkoutHistory(firebaseUid).catch(() => []),
      ]);

      this.activeCycle.set(activeCycle);
      this.sessions.set(sessions);
      this.ensureSelectedDate(activeCycle, sessions);
      await this.refreshSelectedDay();
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to load workout logs.');
    } finally {
      this.loading.set(false);
    }
  }

  protected async refreshSelectedDay(): Promise<void> {
    const firebaseUid = this.firebaseUid();
    const activeCycle = this.activeCycle();
    const selectedDate = this.selectedDate();
    const session = this.sessions().find((item) => item.date === selectedDate) ?? null;

    this.selectedSession.set(session);
    this.notes.set(session?.notes ?? '');

    if (!firebaseUid || !activeCycle || !this.isWorkoutDate(selectedDate, activeCycle)) {
      this.prescribedWorkout.set(null);
      if (session) {
        this.syncDraftsFromSession(session, null);
      } else {
        this.mainLiftDrafts.set([]);
        this.accessoryDrafts.set([]);
      }
      return;
    }

    try {
      const prescribed = await this.workoutCycleApi.getPrescribedWorkout(firebaseUid, selectedDate);
      this.prescribedWorkout.set(prescribed);
      this.syncDraftsFromSession(session, prescribed);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to load prescribed workout.');
      this.prescribedWorkout.set(null);
      if (session) {
        this.syncDraftsFromSession(session, null);
      } else {
        this.mainLiftDrafts.set([]);
        this.accessoryDrafts.set([]);
      }
    }
  }

  @HostListener('document:keydown', ['$event'])
  protected onDocumentKeydown(event: KeyboardEvent): void {
    if (event.altKey || event.ctrlKey || event.metaKey) {
      return;
    }

    const target = event.target as HTMLElement | null;
    if (target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.tagName === 'SELECT' || target.isContentEditable)) {
      return;
    }

    if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
      event.preventDefault();
      this.goToAdjacentWorkout(1);
    }

    if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
      event.preventDefault();
      this.goToAdjacentWorkout(-1);
    }
  }

  protected goToAdjacentWorkout(direction: 1 | -1): void {
    const adjacent = this.getAdjacentWorkoutDate(this.selectedDate(), direction);
    if (!adjacent) {
      return;
    }

    this.selectDate(adjacent);
  }

  protected updateMainLiftCompleted(index: number, completed: boolean): void {
    this.mainLiftDrafts.update((current) =>
      current.map((draft, currentIndex) => (currentIndex === index ? { ...draft, completed } : draft)),
    );
  }

  private syncDraftsFromSession(session: WorkoutSessionResponse | null, prescribed: PrescribedWorkout | null): void {
    const savedMainLifts = session?.exercises.filter((exercise) => exercise.isMainLift) ?? [];
    const savedAccessories = session?.exercises.filter((exercise) => !exercise.isMainLift) ?? [];

    if (prescribed) {
      const checkedMainLifts = new Set(
        savedMainLifts.map((exercise) => this.exerciseKey(exercise.exerciseName, exercise.weight, exercise.reps, exercise.setNumber)),
      );
      const exerciseName = this.getLiftLabel(prescribed.liftType);

      this.mainLiftDrafts.set(
        prescribed.sets.map((set) => {
          const draftKey = this.exerciseKey(exerciseName, set.weight, set.reps, set.setNumber);
          return {
            exerciseName,
            weight: `${set.weight}`,
            reps: `${set.reps}`,
            setNumber: `${set.setNumber}`,
            isMainLift: true,
            completed: checkedMainLifts.has(draftKey),
          };
        }),
      );
    } else {
      this.mainLiftDrafts.set(
        savedMainLifts.map((exercise) => ({
          exerciseName: exercise.exerciseName,
          weight: `${exercise.weight}`,
          reps: `${exercise.reps}`,
          setNumber: `${exercise.setNumber}`,
          isMainLift: true,
          completed: true,
        })),
      );
    }

    this.accessoryDrafts.set(
      savedAccessories.map((exercise) => ({
        exerciseName: exercise.exerciseName,
        weight: `${exercise.weight}`,
        reps: `${exercise.reps}`,
        setNumber: `${exercise.setNumber}`,
        isMainLift: false,
        completed: false,
      })),
    );
  }

  private validateAccessoryDrafts(): string {
    for (const draft of this.accessoryDrafts()) {
      const hasAnyValue =
        draft.exerciseName.trim().length > 0 ||
        draft.weight.trim().length > 0 ||
        draft.reps.trim().length > 0 ||
        draft.setNumber.trim().length > 0;

      if (!hasAnyValue) {
        continue;
      }

      if (!draft.exerciseName.trim() || !draft.weight.trim() || !draft.reps.trim() || !draft.setNumber.trim()) {
        return 'Finish each accessory row or remove it before saving.';
      }
    }

    return '';
  }

  private toExerciseRequest(draft: ExerciseDraft, isMainLift: boolean): WorkoutExerciseRequest {
    return {
      exerciseName: draft.exerciseName.trim(),
      weight: Number(draft.weight),
      reps: Number(draft.reps),
      setNumber: Number(draft.setNumber),
      isMainLift,
    };
  }

  private exerciseKey(exerciseName: string, weight: string | number, reps: string | number, setNumber: string | number): string {
    return `${exerciseName.trim()}|${weight}|${reps}|${setNumber}`;
  }

  private ensureSelectedDate(activeCycle: WorkoutCycleResponse | null, sessions: WorkoutSessionResponse[]): void {
    const today = this.todayIso();
    const loggedDates = new Set(sessions.map((session) => session.date));

    if (!activeCycle) {
      const fallback = sessions[0]?.date ?? today;
      this.selectedDate.set(fallback);
      this.displayedMonth.set(this.startOfMonthIso(fallback));
      return;
    }

    const currentSelection = this.selectedDate();
    const workoutDates = this.getWorkoutDates(activeCycle);
    const initialSelection = workoutDates.includes(currentSelection) || loggedDates.has(currentSelection)
      ? currentSelection
      : this.getAdjacentWorkoutDate(currentSelection, 1) ?? sessions[0]?.date ?? workoutDates[0] ?? today;

    this.selectedDate.set(initialSelection);
    this.displayedMonth.set(this.startOfMonthIso(initialSelection));
  }

  private buildCalendarDays(
    displayedMonthIso: string,
    workoutDates: string[],
    loggedDates: string[],
    selectedDateIso: string,
  ): CalendarDay[] {
    const monthAnchor = this.parseIsoDate(displayedMonthIso);
    const monthStart = new Date(Date.UTC(monthAnchor.getUTCFullYear(), monthAnchor.getUTCMonth(), 1));
    const monthEnd = new Date(Date.UTC(monthAnchor.getUTCFullYear(), monthAnchor.getUTCMonth() + 1, 0));

    const calendarStart = new Date(monthStart);
    calendarStart.setUTCDate(calendarStart.getUTCDate() - calendarStart.getUTCDay());

    const calendarEnd = new Date(monthEnd);
    calendarEnd.setUTCDate(calendarEnd.getUTCDate() + (6 - calendarEnd.getUTCDay()));

    const workoutSet = new Set(workoutDates);
    const loggedSet = new Set(loggedDates);
    const today = this.todayIso();
    const cells: CalendarDay[] = [];

    for (let cursor = new Date(calendarStart); cursor <= calendarEnd; cursor.setUTCDate(cursor.getUTCDate() + 1)) {
      const isoDate = this.toIsoDate(cursor);
      cells.push({
        isoDate,
        dayNumber: cursor.getUTCDate(),
        inCurrentMonth: cursor.getUTCMonth() === monthStart.getUTCMonth(),
        hasWorkout: workoutSet.has(isoDate),
        hasLog: loggedSet.has(isoDate),
        isSelected: isoDate === selectedDateIso,
        isToday: isoDate === today,
      });
    }

    return cells;
  }

  private getWorkoutDates(activeCycle: WorkoutCycleResponse | null): string[] {
    if (!activeCycle) {
      return [];
    }

    const dates: string[] = [];
    const start = this.parseIsoDate(activeCycle.startDate);
    const end = this.parseIsoDate(activeCycle.endDate);

    for (let cursor = new Date(start); cursor <= end; cursor.setUTCDate(cursor.getUTCDate() + 1)) {
      const isoDate = this.toIsoDate(cursor);
      if (this.isWorkoutDate(isoDate, activeCycle)) {
        dates.push(isoDate);
      }
    }

    return dates;
  }

  private isWorkoutDate(dateIso: string, cycle: WorkoutCycleResponse | null): boolean {
    if (!cycle) {
      return false;
    }

    const date = this.parseIsoDate(dateIso);
    const cycleStart = this.parseIsoDate(cycle.startDate);
    const cycleEnd = this.parseIsoDate(cycle.endDate);

    if (date < cycleStart || date > cycleEnd) {
      return false;
    }

    const dayIndex = date.getUTCDay();
    return dayIndex === DAY_TO_INDEX[cycle.benchDay]
      || dayIndex === DAY_TO_INDEX[cycle.squatDay]
      || dayIndex === DAY_TO_INDEX[cycle.deadliftDay]
      || dayIndex === DAY_TO_INDEX[cycle.shoulderPressDay];
  }

  private getAdjacentWorkoutDate(currentDateIso: string, direction: 1 | -1): string | null {
    const activeCycle = this.activeCycle();
    if (!activeCycle) {
      return null;
    }

    const workoutDates = this.getWorkoutDates(activeCycle);
    if (!workoutDates.length) {
      return null;
    }

    if (direction > 0) {
      return workoutDates.find((date) => date > currentDateIso) ?? workoutDates[workoutDates.length - 1];
    }

    for (let index = workoutDates.length - 1; index >= 0; index -= 1) {
      if (workoutDates[index] < currentDateIso) {
        return workoutDates[index];
      }
    }

    return workoutDates[0];
  }

  private getLiftLabel(liftType: LiftType): string {
    switch (liftType) {
      case 'BENCH':
        return 'Bench Press';
      case 'SQUAT':
        return 'Squat';
      case 'DEADLIFT':
        return 'Deadlift';
      case 'SHOULDER_PRESS':
        return 'Shoulder Press';
    }
  }

  private startOfMonthIso(dateIso: string): string {
    const date = this.parseIsoDate(dateIso);
    return this.toIsoDate(new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), 1)));
  }

  private formatMonthLabel(dateIso: string): string {
    return this.parseIsoDate(dateIso).toLocaleDateString('en-US', {
      month: 'long',
      year: 'numeric',
      timeZone: 'UTC',
    });
  }

  private formatDateLabel(dateIso: string): string {
    return this.parseIsoDate(dateIso).toLocaleDateString('en-US', {
      weekday: 'long',
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      timeZone: 'UTC',
    });
  }

  private todayIso(): string {
    return this.toIsoDate(new Date());
  }

  private parseIsoDate(dateIso: string): Date {
    const [year, month, day] = dateIso.split('-').map(Number);
    return new Date(Date.UTC(year, month - 1, day));
  }

  private toIsoDate(date: Date): string {
    return date.toISOString().slice(0, 10);
  }
}
