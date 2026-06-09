import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { BACKEND_API_BASE_URL } from '../auth/app-config';

export type LiftType = 'BENCH' | 'SQUAT' | 'DEADLIFT' | 'SHOULDER_PRESS';

export interface CycleProgressRequest {
  liftOutcomes: Record<LiftType, boolean>;
}

export interface WorkoutCycleResponse {
  id: number;
  firebaseUid: string;
  cycleNumber: number;
  startDate: string;
  endDate: string;
  benchTrainingMax: number;
  squatTrainingMax: number;
  deadliftTrainingMax: number;
  shoulderPressTrainingMax: number;
  isActive: boolean;
}

export interface PrescribedSet {
  setNumber: number;
  weight: number;
  reps: number;
  isAmrap: boolean;
}

export interface PrescribedWorkout {
  date: string;
  weekNumber: number;
  liftType: LiftType;
  trainingMax: number;
  sets: PrescribedSet[];
  isDeload: boolean;
}

@Injectable({ providedIn: 'root' })
export class WorkoutCycleApiService {
  constructor(private readonly http: HttpClient) {}

  getActiveCycle(firebaseUid: string): Promise<WorkoutCycleResponse> {
    return firstValueFrom(this.http.get<WorkoutCycleResponse>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/cycles/active`));
  }

  getCycleHistory(firebaseUid: string): Promise<WorkoutCycleResponse[]> {
    return firstValueFrom(this.http.get<WorkoutCycleResponse[]>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/cycles/history`));
  }

  getPrescribedWorkout(firebaseUid: string, date: string): Promise<PrescribedWorkout> {
    return firstValueFrom(
      this.http.get<PrescribedWorkout>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/cycles/prescribed`, {
        params: { date },
      }),
    );
  }

  progressToNextCycle(firebaseUid: string, request: CycleProgressRequest): Promise<WorkoutCycleResponse> {
    return firstValueFrom(
      this.http.post<WorkoutCycleResponse>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/cycles/progress`, request),
    );
  }
}
