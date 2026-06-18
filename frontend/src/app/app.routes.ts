import { Routes } from '@angular/router';

import { Game } from './game/game';
import { Home } from './home/home';
import { Lobby } from './lobby/lobby';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'lobby/:roomCode', component: Lobby },
  { path: 'game/:roomCode', component: Game },
  { path: '**', redirectTo: '' },
];
