import { Injectable } from '@angular/core';

import { UserApiService, UserProfileRequest } from '../api/user-api.service';

@Injectable({ providedIn: 'root' })
export class BackendUserSyncService {
  constructor(private readonly userApi: UserApiService) {}

  async ensureUser(firebaseUid: string, profile: UserProfileRequest): Promise<'existing' | 'created'> {
    try {
      await this.userApi.getUserByFirebaseUid(firebaseUid);
      return 'existing';
    } catch (error) {
      if (!this.isNotFound(error)) {
        throw error;
      }

      await this.userApi.createUser(profile);
      return 'created';
    }
  }

  private isNotFound(error: unknown): boolean {
    return Boolean(error && typeof error === 'object' && 'status' in error && (error as { status?: number }).status === 404);
  }
}
