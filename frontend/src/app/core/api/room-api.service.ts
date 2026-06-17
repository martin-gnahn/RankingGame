import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import {
  CreateRoomRequest,
  JoinRoomRequest,
  RoomActionResponse,
  RoomCode,
  RoomResponse,
  StartGameRequest,
  StartGameResponse,
} from './room.models';

@Injectable({
  providedIn: 'root',
})
export class RoomApiService {
  private readonly roomsUrl = `${environment.apiBaseUrl}/rooms`;

  constructor(private readonly http: HttpClient) {}

  createRoom(request: CreateRoomRequest): Observable<RoomActionResponse> {
    return this.http.post<RoomActionResponse>(this.roomsUrl, request);
  }

  joinRoom(roomCode: RoomCode, request: JoinRoomRequest): Observable<RoomActionResponse> {
    return this.http.post<RoomActionResponse>(
      `${this.roomsUrl}/${encodeURIComponent(roomCode)}/players`,
      request,
    );
  }

  getRoom(roomCode: RoomCode): Observable<RoomResponse> {
    return this.http.get<RoomResponse>(`${this.roomsUrl}/${encodeURIComponent(roomCode)}`);
  }

  startRankingGame(roomCode: RoomCode, request: StartGameRequest): Observable<StartGameResponse> {
    return this.http.post<StartGameResponse>(
      `${this.roomsUrl}/${encodeURIComponent(roomCode)}/ranking-game/start`,
      request,
    );
  }
}
