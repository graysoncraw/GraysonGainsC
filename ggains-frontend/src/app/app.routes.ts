import { Routes } from '@angular/router';

import { appGuard } from './auth/app.guard';
import { guestGuard } from './auth/guest.guard';
import { setupGuard } from './auth/setup.guard';
import { AppShellComponent } from './pages/app-shell.component';
import { AuthPageComponent } from './pages/auth-page.component';
import { CyclesPageComponent } from './pages/cycles-page.component';
import { DashboardPageComponent } from './pages/dashboard-page.component';
import { PersonalRecordsPageComponent } from './pages/personal-records-page.component';
import { ProfilePageComponent } from './pages/profile-page.component';
import { SetupFinishPageComponent } from './pages/setup-finish-page.component';
import { SetupShellComponent } from './pages/setup-shell.component';
import { SchedulePageComponent } from './pages/schedule-page.component';
import { SettingsPageComponent } from './pages/settings-page.component';

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
        path: 'schedule',
        component: SchedulePageComponent,
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
        path: 'schedule',
        component: SchedulePageComponent,
      },
      {
        path: 'cycles',
        component: CyclesPageComponent,
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
