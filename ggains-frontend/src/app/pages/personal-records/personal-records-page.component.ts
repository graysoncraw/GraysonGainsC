import { Component, computed, effect, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';

import { OnboardingFlowService } from '../../auth/onboarding-flow.service';
import { SessionService } from '../../auth/session.service';
import {
  PersonalRecordApiService,
  PersonalRecordRequest,
  PersonalRecordResponse,
} from '../../api/personal-record-api.service';

type LiftType = 'benchPressPR' | 'squatPR' | 'deadliftPR' | 'shoulderPressPR';

@Component({
  selector: 'app-personal-records-page',
  imports: [FormsModule],
  templateUrl: './personal-records-page.component.html',
  styleUrl: './personal-records-page.component.scss',
})
export class PersonalRecordsPageComponent {
  protected readonly form = signal<PersonalRecordRequest>({
    benchPressPR: 0,
    squatPR: 0,
    deadliftPR: 0,
    shoulderPressPR: 0,
  });
  protected readonly record = signal<PersonalRecordResponse | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly success = signal('');
  protected readonly patchLift = signal<LiftType>('benchPressPR');
  protected readonly patchValue = signal(0);
  protected readonly firebaseUid = computed(() => this.session.firebaseUid());
  protected readonly onboardingMode = signal(false);

  constructor(
    private readonly session: SessionService,
    private readonly personalRecordApi: PersonalRecordApiService,
    private readonly onboardingFlow: OnboardingFlowService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
  ) {
    this.onboardingMode.set(this.route.snapshot.data['onboarding'] === true);
    effect(() => {
      const firebaseUid = this.firebaseUid();
      if (firebaseUid) {
        void this.load(firebaseUid);
      }
    });
  }

  protected updateField<K extends keyof PersonalRecordRequest>(field: K, value: PersonalRecordRequest[K]): void {
    this.form.update((current) => ({ ...current, [field]: value }));
  }

  protected updateNumberField(field: LiftType, value: string): void {
    this.form.update((current) => ({
      ...current,
      [field]: value === '' ? 0 : Number(value),
    }));
  }

  protected async load(firebaseUid = this.firebaseUid()): Promise<void> {
    if (!firebaseUid) {
      return;
    }

    this.loading.set(true);
    this.error.set('');

    try {
      const record = await this.personalRecordApi.getPersonalRecord(firebaseUid);
      this.record.set(record);
      this.form.set({
        benchPressPR: record.benchPressPR,
        squatPR: record.squatPR,
        deadliftPR: record.deadliftPR,
        shoulderPressPR: record.shoulderPressPR,
      });
    } catch (error) {
      this.record.set(null);
      if (!this.isNotFound(error)) {
        this.error.set(error instanceof Error ? error.message : 'Unable to load personal records.');
      }
    } finally {
      this.loading.set(false);
    }
  }

  protected async save(): Promise<void> {
    const firebaseUid = this.firebaseUid();
    if (!firebaseUid) {
      return;
    }

    this.loading.set(true);
    this.error.set('');
    this.success.set('');

    try {
      const wasExisting = Boolean(this.record());
      const request = this.form();
      const updated = this.record()
        ? await this.personalRecordApi.updatePersonalRecord(firebaseUid, request)
        : await this.personalRecordApi.createPersonalRecord(firebaseUid, request);

      this.record.set(updated);
      this.success.set(wasExisting ? 'Personal records saved.' : 'Personal records created.');

      if (this.onboardingMode()) {
        await this.router.navigateByUrl(await this.onboardingFlow.resolveRoute());
      }
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Unable to save personal records.');
    } finally {
      this.loading.set(false);
    }
  }
  
  private isNotFound(error: unknown): boolean {
    return Boolean(error && typeof error === 'object' && 'status' in error && (error as { status?: number }).status === 404);
  }
}
