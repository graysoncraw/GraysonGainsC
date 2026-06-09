import { Component, computed } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { FirebaseAuthService } from '../auth/firebase-auth.service';
import { SessionService } from '../auth/session.service';

@Component({
  selector: 'app-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.scss',
})
export class AppShellComponent {
  protected readonly user = computed(() => this.session.backendUser());
  protected readonly status = computed(() => this.session.status());
  protected readonly statusMessage = computed(() => this.session.message());

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
