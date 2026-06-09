import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { BACKEND_API_BASE_URL } from '../auth/app-config';

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export interface WorkoutScheduleRequest {
  cycleStartDate: string;
  benchDay: DayOfWeek;
  squatDay: DayOfWeek;
  deadliftDay: DayOfWeek;
  shoulderPressDay: DayOfWeek;
}

export interface WorkoutScheduleResponse extends WorkoutScheduleRequest {
  id: number;
  firebaseUid: string;
}

@Injectable({ providedIn: 'root' })
export class WorkoutScheduleApiService {
  constructor(private readonly http: HttpClient) {}

  getWorkoutSchedule(firebaseUid: string): Promise<WorkoutScheduleResponse> {
    return firstValueFrom(this.http.get<WorkoutScheduleResponse>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/schedule`));
  }

  createWorkoutSchedule(firebaseUid: string, request: WorkoutScheduleRequest): Promise<WorkoutScheduleResponse> {
    return firstValueFrom(
      this.http.post<WorkoutScheduleResponse>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/schedule`, request),
    );
  }

  updateWorkoutSchedule(firebaseUid: string, request: WorkoutScheduleRequest): Promise<WorkoutScheduleResponse> {
    return firstValueFrom(
      this.http.put<WorkoutScheduleResponse>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/schedule`, request),
    );
  }
}
