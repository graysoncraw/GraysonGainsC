import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FirebaseAuthService } from '../../auth/firebase-auth.service';
import { RouterLink, RouterOutlet } from '@angular/router';
import { SessionService } from '../../auth/session.service';

@Component({
  selector: 'app-setup-shell',
  imports: [RouterOutlet],
  templateUrl: './setup-shell.component.html',
  styleUrl: './setup-shell.component.scss',
})
export class SetupShellComponent {
  constructor(
    private readonly session: SessionService,
    private readonly firebaseAuth: FirebaseAuthService,
    private readonly router: Router,
  ){}
  
  protected async signOut(): Promise<void> {
    await this.firebaseAuth.signOut();
    this.session.clearSessionState();
    await this.router.navigateByUrl('/auth');
  }
}
