import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { BACKEND_API_BASE_URL } from '../auth/app-config';

export interface PersonalRecordRequest {
  benchPressPR: number;
  squatPR: number;
  deadliftPR: number;
  shoulderPressPR: number;
}

export interface PersonalRecordResponse extends PersonalRecordRequest {
  id: number;
  firebaseUid: string;
}

@Injectable({ providedIn: 'root' })
export class PersonalRecordApiService {
  constructor(private readonly http: HttpClient) {}

  getPersonalRecord(firebaseUid: string): Promise<PersonalRecordResponse> {
    return firstValueFrom(
      this.http.get<PersonalRecordResponse>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/personal-record`),
    );
  }

  createPersonalRecord(firebaseUid: string, request: PersonalRecordRequest): Promise<PersonalRecordResponse> {
    return firstValueFrom(
      this.http.post<PersonalRecordResponse>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/personal-record`, request),
    );
  }

  updatePersonalRecord(firebaseUid: string, request: PersonalRecordRequest): Promise<PersonalRecordResponse> {
    return firstValueFrom(
      this.http.put<PersonalRecordResponse>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/personal-record`, request),
    );
  }

  updateSpecificPr(firebaseUid: string, liftType: string, newPR: number): Promise<PersonalRecordResponse> {
    return firstValueFrom(
      this.http.patch<PersonalRecordResponse>(
        `${BACKEND_API_BASE_URL}/api/users/${firebaseUid}/personal-record/${liftType}`,
        null,
        { params: { newPR } },
      ),
    );
  }
}
