import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: 'rsvp',
    loadComponent: () =>
      import('./features/rsvp/pages/rsvp/rsvp').then(m => m.Rsvp),
  },
  { path: '', pathMatch: 'full', redirectTo: 'rsvp' },
];
