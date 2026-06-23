import { Component, HostListener, computed, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { OnboardingFlowService } from '../../auth/onboarding-flow.service';
import { SessionService } from '../../auth/session.service';
import {
  DayOfWeek,
  LiftType,
  PrescribedWorkout,
  WorkoutCycleApiService,
  WorkoutCycleRequest,
  WorkoutCycleResponse,
} from '../../api/workout-cycle-api.service';

const LIFTS: LiftType[] = ['BENCH', 'SQUAT', 'DEADLIFT', 'SHOULDER_PRESS'];
const DAYS: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const WEEKDAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const DAY_TO_INDEX: Record<DayOfWeek, number> = {
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
  isSelected: boolean;
  isToday: boolean;
}

@Component({
  selector: 'app-cycles-page',
  imports: [FormsModule],
  templateUrl: './cycles-page.component.html',
  styleUrl: './cycles-page.component.scss',
})
export class CyclesPageComponent {
  protected readonly activeCycle = signal<WorkoutCycleResponse | null>(null);
  protected readonly history = signal<WorkoutCycleResponse[]>([]);
  protected readonly prescribedWorkout = signal<PrescribedWorkout | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly success = signal('');
  protected readonly selectedDate = signal(this.todayIso());
  protected readonly displayedMonth = signal(this.startOfMonthIso(this.todayIso()));
  protected readonly onboardingMode = signal(false);
  protected readonly form = signal<WorkoutCycleRequest>({
    startDate: new Date().toISOString().slice(0, 10),
    benchDay: 'MONDAY',
    squatDay: 'WEDNESDAY',
    deadliftDay: 'FRIDAY',
    shoulderPressDay: 'TUESDAY',
  });
  protected readonly lifts = LIFTS;
  protected readonly weekdayLabels = WEEKDAY_LABELS;
  protected readonly firebaseUid = computed(() => this.session.firebaseUid());
  protected readonly days = DAYS;
  protected readonly workoutDates = computed(() => this.getWorkoutDates(this.activeCycle()));
  protected readonly monthLabel = computed(() => this.formatMonthLabel(this.displayedMonth()));
  protected readonly selectedDateLabel = computed(() => this.formatDateLabel(this.selectedDate()));
  protected readonly calendarDays = computed(() =>
    this.buildCalendarDays(this.displayedMonth(), this.workoutDates(), this.selectedDate()),
  );

  constructor(
    private readonly session: SessionService,
    private readonly workoutCycleApi: WorkoutCycleApiService,
    private readonly route: ActivatedRoute,
    private readonly onboardingFlow: OnboardingFlowService,
    private readonly router: Router,
  ) {
    this.onboardingMode.set(this.route.snapshot.data['onboarding'] === true);
    effect(() => {
      const firebaseUid = this.firebaseUid();
      if (firebaseUid) {
        void this.load(firebaseUid);
      }
    });
    effect(() => {
      const firebaseUid = this.firebaseUid();
      const activeCycle = this.activeCycle();
      if (!firebaseUid || !activeCycle) {
        return;
      }

      const selectedDate = this.selectedDate();
      this.displayedMonth.set(this.startOfMonthIso(selectedDate));

      if (this.isWorkoutDate(selectedDate, activeCycle)) {
        void this.refreshPrescribedWorkout();
      } else {
        this.prescribedWorkout.set(null);
      }
    });
  }

  protected updateField<K extends keyof WorkoutCycleRequest>(field: K, value: WorkoutCycleRequest[K]): void {
    this.form.update((current) => ({ ...current, [field]: value }));
  }

  protected updateNumberField(field: LiftType, value: string): void {
    this.form.update((current) => ({
      ...current,
      [field]: value === '' ? 0 : Number(value),
    }));
  }

  protected selectDate(date: string): void {
    this.selectedDate.set(date);
    this.displayedMonth.set(this.startOfMonthIso(date));
  }

  protected changeMonth(offset: number): void {
    const month = this.parseIsoDate(this.displayedMonth());
    month.setUTCMonth(month.getUTCMonth() + offset);
    this.displayedMonth.set(this.startOfMonthIso(this.toIsoDate(month)));
  }

  protected goToAdjacentWorkout(direction: 1 | -1): void {
    const adjacent = this.getAdjacentWorkoutDate(this.selectedDate(), direction);
    if (!adjacent) {
      return;
    }

    this.selectDate(adjacent);
  }

  protected async saveSchedule(): Promise<void> {
    const firebaseUid = this.firebaseUid();
    if (!firebaseUid) {
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    try {
      const request = this.form();
      const updated = this.activeCycle()
        ? await this.workoutCycleApi.updateActiveWorkoutCycle(firebaseUid, request)
        : await this.workoutCycleApi.createWorkoutCycle(firebaseUid, request);

      this.activeCycle.set(updated);
      this.success.set('Schedule saved.');

      if (this.onboardingMode()) {
        const nextRoute = await this.onboardingFlow.resolveRoute();
        await this.router.navigateByUrl(nextRoute);
      }
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to save schedule.');
    } finally {
      this.loading.set(false);
    }
  }

  protected async load(firebaseUid = this.firebaseUid()): Promise<void> {
    if (!firebaseUid) {
      return;
    }

    this.loading.set(true);
    this.error.set('');

    try {
      const [activeCycle, history] = await Promise.all([
        this.workoutCycleApi.getActiveCycle(firebaseUid).catch(() => null),
        this.workoutCycleApi.getCycleHistory(firebaseUid).catch(() => []),
      ]);

      this.activeCycle.set(activeCycle);
      this.history.set(history);
      this.ensureSelectedDate(activeCycle);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to load cycle data.');
    } finally {
      this.loading.set(false);
    }
  }

  protected async refreshPrescribedWorkout(): Promise<void> {
    const firebaseUid = this.firebaseUid();
    const activeCycle = this.activeCycle();
    const selectedDate = this.selectedDate();

    if (!firebaseUid || !activeCycle || !this.isWorkoutDate(selectedDate, activeCycle)) {
      this.prescribedWorkout.set(null);
      return;
    }

    try {
      const prescribedWorkout = await this.workoutCycleApi.getPrescribedWorkout(firebaseUid, selectedDate);
      this.prescribedWorkout.set(prescribedWorkout);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to load prescribed workout.');
      this.prescribedWorkout.set(null);
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

  private ensureSelectedDate(activeCycle: WorkoutCycleResponse | null): void {
    if (!activeCycle) {
      this.selectedDate.set(this.todayIso());
      this.displayedMonth.set(this.startOfMonthIso(this.todayIso()));
      this.prescribedWorkout.set(null);
      return;
    }

    const currentSelection = this.selectedDate();
    const workoutDates = this.getWorkoutDates(activeCycle);
    const initialSelection = this.isWorkoutDate(currentSelection, activeCycle)
      ? currentSelection
      : this.getAdjacentWorkoutDate(currentSelection, 1) ?? workoutDates[0] ?? activeCycle.startDate;

    this.selectedDate.set(initialSelection);
    this.displayedMonth.set(this.startOfMonthIso(initialSelection));
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

  private buildCalendarDays(displayedMonthIso: string, workoutDates: string[], selectedDateIso: string): CalendarDay[] {
    const monthAnchor = this.parseIsoDate(displayedMonthIso);
    const monthStart = new Date(Date.UTC(monthAnchor.getUTCFullYear(), monthAnchor.getUTCMonth(), 1));
    const monthEnd = new Date(Date.UTC(monthAnchor.getUTCFullYear(), monthAnchor.getUTCMonth() + 1, 0));

    const calendarStart = new Date(monthStart);
    calendarStart.setUTCDate(calendarStart.getUTCDate() - calendarStart.getUTCDay());

    const calendarEnd = new Date(monthEnd);
    calendarEnd.setUTCDate(calendarEnd.getUTCDate() + (6 - calendarEnd.getUTCDay()));

    const workoutSet = new Set(workoutDates);
    const today = this.todayIso();
    const cells: CalendarDay[] = [];

    for (let cursor = new Date(calendarStart); cursor <= calendarEnd; cursor.setUTCDate(cursor.getUTCDate() + 1)) {
      const isoDate = this.toIsoDate(cursor);
      cells.push({
        isoDate,
        dayNumber: cursor.getUTCDate(),
        inCurrentMonth: cursor.getUTCMonth() === monthStart.getUTCMonth(),
        hasWorkout: workoutSet.has(isoDate),
        isSelected: isoDate === selectedDateIso,
        isToday: isoDate === today,
      });
    }

    return cells;
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
