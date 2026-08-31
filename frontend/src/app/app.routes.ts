import {Routes} from '@angular/router';
import {Home} from './home/home';
import {authenticationGuard} from './authentication-guard';

export const routes: Routes = [
  {path: '', component: Home},
  {
    path: 'lobby/:roomCode',
    loadComponent: () => import('./lobby/lobby').then((m) => m.Lobby),
    canActivate: [authenticationGuard],
  },
  {
    path: 'game/:roomCode',
    loadComponent: () => import('./game/game').then((m) => m.Game),
    canActivate: [authenticationGuard],
  },
  {
    path: 'error',
    loadComponent: () => import('./error/error').then((m) => m.ErrorComponent),
  },
  { path: '**', redirectTo: '' },
];
