import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-setup-shell',
  imports: [RouterLink, RouterOutlet],
  templateUrl: './setup-shell.component.html',
  styleUrl: './setup-shell.component.scss',
})
export class SetupShellComponent {}
