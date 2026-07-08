import {Routes} from '@angular/router';

import {Game} from './game/game';
import {Home} from './home/home';

export const routes: Routes = [
  {path: '', component: Home},
  {
    path: 'lobby/:roomCode',
    loadComponent: () => import('./lobby/lobby').then((m) => m.Lobby),
  },
  {
    path: 'game/:roomCode',
    loadComponent: () => import('./game/game').then((m) => m.Game),
  },
  { path: 'game/:roomCode', component: Game },
  { path: '**', redirectTo: '' },
];
