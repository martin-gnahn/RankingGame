import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { RoomCode } from './room.models';
import { GameSessionPlayerResponse } from './game.models';

@Injectable({
  providedIn: 'root',
})
export class GameApiService {
  private readonly roomsUrl = `${environment.apiBaseUrl}/rooms`;

  constructor(private readonly http: HttpClient) {}

  getActivePlayers(roomCode: RoomCode): Observable<GameSessionPlayerResponse[]> {
    return this.http
      .get<GameSessionPlayerResponse[]>(
        `${this.roomsUrl}/${encodeURIComponent(roomCode)}/ranking-game/current-round/players`,
      )
      .pipe(
        tap((players) => {
          console.log('Active game players DTO response', players);
        }),
      );
  }
}
