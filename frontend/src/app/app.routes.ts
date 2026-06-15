import { Routes } from '@angular/router';

import { Home } from './home/home';
import { Lobby } from './lobby/lobby';

export const routes: Routes = [
  { path: '', component: Home },
  { path: 'lobby/:roomCode', component: Lobby },
  { path: '**', redirectTo: '' },
];
