import { Component, computed, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SessionService } from '../../auth/session.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [RouterLink],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent {
  protected readonly loading = signal(true);
  protected readonly error = signal('');

  protected readonly user = computed(() => this.session.backendUser());

  constructor(
    private readonly session: SessionService,
  ) {
    void this.load();
  }

  protected async load(): Promise<void> {
    const firebaseUid = this.session.firebaseUid();
    if (!firebaseUid) {
      this.loading.set(false);
      return;
    }

    this.loading.set(true);
    this.error.set('');
  }
}
