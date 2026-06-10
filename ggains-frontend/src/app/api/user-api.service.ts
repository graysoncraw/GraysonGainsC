import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { BACKEND_API_BASE_URL } from '../auth/app-config';

export interface UserProfileRequest {
  firstName: string;
  lastName: string;
  email: string;
  gender: string;
  heightFt: number | null;
  heightIn: number | null;
  weight: number | null;
}

export interface UserProfileResponse {
  firebaseUid: string;
  firstName: string;
  lastName: string;
  email: string;
  dateCreated: string;
  gender?: string | null;
  heightFt?: number | null;
  heightIn?: number | null;
  weight?: number | null;
}

@Injectable({ providedIn: 'root' })
export class UserApiService {
  constructor(private readonly http: HttpClient) {}

  getUserByFirebaseUid(firebaseUid: string): Promise<UserProfileResponse> {
    return firstValueFrom(this.http.get<UserProfileResponse>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}`));
  }

  getUserByEmail(email: string): Promise<UserProfileResponse> {
    return firstValueFrom(this.http.get<UserProfileResponse>(`${BACKEND_API_BASE_URL}/api/users`, { params: { email } }));
  }

  createUser(profile: UserProfileRequest): Promise<UserProfileResponse> {
    return firstValueFrom(this.http.post<UserProfileResponse>(`${BACKEND_API_BASE_URL}/api/users`, profile));
  }

  updateUser(firebaseUid: string, profile: UserProfileRequest): Promise<UserProfileResponse> {
    return firstValueFrom(this.http.put<UserProfileResponse>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}`, profile));
  }

  deleteUser(firebaseUid: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${BACKEND_API_BASE_URL}/api/users/${firebaseUid}`));
  }
}
