import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { SessionService } from '../../auth/session.service';
import { FirebaseAuthService } from '../../auth/firebase-auth.service';

@Component({
  selector: 'app-settings-page',
  imports: [FormsModule],
  templateUrl: './settings-page.component.html',
  styleUrl: './settings-page.component.scss',
})
export class SettingsPageComponent {
  protected readonly status = signal('');
  protected readonly loading = signal(false);

  constructor(
    private readonly session: SessionService,
    private readonly firebaseAuth: FirebaseAuthService,
    private readonly router: Router,
  ) {}

  protected async signOut(): Promise<void> {
    await this.firebaseAuth.signOut();
    this.session.clearSessionState();
    await this.router.navigateByUrl('/auth');
  }
}
