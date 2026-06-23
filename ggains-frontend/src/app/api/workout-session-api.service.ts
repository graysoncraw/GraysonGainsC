import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { BACKEND_API_BASE_URL } from '../auth/app-config';
import { LiftType } from './workout-cycle-api.service';

export interface WorkoutExerciseRequest {
  exerciseName: string;
  weight: number;
  reps: number;
  setNumber: number;
  isMainLift: boolean;
}

export interface WorkoutExerciseResponse {
  id: number;
  exerciseName: string;
  weight: number;
  reps: number;
  setNumber: number;
  isMainLift: boolean;
}

export interface WorkoutSessionRequest {
  mainLiftType?: LiftType | null;
  weekNumber?: number | null;
  notes?: string | null;
  exercises: WorkoutExerciseRequest[];
}

export interface WorkoutSessionResponse {
  id: number;
  firebaseUid: string;
  workoutCycleId: number;
  cycleNumber: number;
  date: string;
  mainLiftType: LiftType | null;
  weekNumber: number | null;
  notes: string | null;
  exercises: WorkoutExerciseResponse[];
}

@Injectable({ providedIn: 'root' })
export class WorkoutSessionApiService {
  constructor(private readonly http: HttpClient) {}

  getWorkoutSession(firebaseUid: string, date: string): Promise<WorkoutSessionResponse> {
    return firstValueFrom(this.http.get<WorkoutSessionResponse>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/workout-sessions/${date}`));
  }

  getWorkoutHistory(firebaseUid: string): Promise<WorkoutSessionResponse[]> {
    return firstValueFrom(this.http.get<WorkoutSessionResponse[]>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/workout-sessions`));
  }

  upsertWorkoutSession(firebaseUid: string, date: string, request: WorkoutSessionRequest): Promise<WorkoutSessionResponse> {
    return firstValueFrom(this.http.put<WorkoutSessionResponse>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/workout-sessions/${date}`, request));
  }
}
