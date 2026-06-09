import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { BACKEND_API_BASE_URL } from './app-config';

export interface BackendUserProfile {
  firstName: string;
  lastName: string;
  email: string;
  gender?: string;
  heightFt?: number;
  heightIn?: number;
  weight?: number;
}

@Injectable({ providedIn: 'root' })
export class BackendUserSyncService {
  constructor(private readonly http: HttpClient) {}

  async ensureUser(firebaseUid: string, idToken: string, profile: BackendUserProfile): Promise<'existing' | 'created'> {
    const headers = new HttpHeaders({
      Authorization: `Bearer ${idToken}`,
    });
    const userUrl = `${BACKEND_API_BASE_URL}/api/users/${firebaseUid}`;

    try {
      await firstValueFrom(this.http.get(userUrl, { headers }));
      return 'existing';
    } catch (error) {
      if (!this.isNotFound(error)) {
        throw error;
      }

      await firstValueFrom(
        this.http.post(`${BACKEND_API_BASE_URL}/api/users`, profile, {
          headers,
        }),
      );
      return 'created';
    }
  }

  private isNotFound(error: unknown): boolean {
    return error instanceof HttpErrorResponse && error.status === 404;
  }
}
