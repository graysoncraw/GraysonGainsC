import { Routes } from '@angular/router';

import { appGuard } from './auth/guards/app.guard';
import { guestGuard } from './auth/guards/guest.guard';
import { setupGuard } from './auth/guards/setup.guard';
import { AppShellComponent } from './pages/shell/app-shell.component';
import { AuthPageComponent } from './pages/auth/auth-page.component';
import { CyclesPageComponent } from './pages/cycles/cycles-page.component';
import { DashboardPageComponent } from './pages/dashboard/dashboard-page.component';
import { PersonalRecordsPageComponent } from './pages/personal-records/personal-records-page.component';
import { ProfilePageComponent } from './pages/profile/profile-page.component';
import { WorkoutLogsPageComponent } from './pages/workout-logs/workout-logs-page.component';
import { SetupFinishPageComponent } from './pages/setup/setup-finish-page.component';
import { SetupShellComponent } from './pages/setup/setup-shell.component';
import { SettingsPageComponent } from './pages/settings/settings-page.component';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'auth',
  },
  {
    path: 'auth',
    canActivate: [guestGuard],
    component: AuthPageComponent,
  },
  {
    path: 'setup',
    canActivate: [setupGuard],
    component: SetupShellComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'profile',
      },
      {
        path: 'profile',
        component: ProfilePageComponent,
        data: { onboarding: true },
      },
      {
        path: 'personal-records',
        component: PersonalRecordsPageComponent,
        data: { onboarding: true },
      },
      {
        path: 'cycles',
        component: CyclesPageComponent,
        data: { onboarding: true },
      },
      {
        path: 'finish',
        component: SetupFinishPageComponent,
        data: { onboarding: true },
      },
    ],
  },
  {
    path: 'app',
    canActivate: [appGuard],
    component: AppShellComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        component: DashboardPageComponent,
      },
      {
        path: 'profile',
        component: ProfilePageComponent,
      },
      {
        path: 'personal-records',
        component: PersonalRecordsPageComponent,
      },
      {
        path: 'cycles',
        component: CyclesPageComponent,
      },
      {
        path: 'logs',
        component: WorkoutLogsPageComponent,
      },
      {
        path: 'settings',
        component: SettingsPageComponent,
      },
    ],
  },
  {
    path: '**',
    redirectTo: 'auth',
  },
];
