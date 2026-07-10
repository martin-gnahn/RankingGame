import {HttpClient} from '@angular/common/http';
import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';

import {environment} from '../../../environments/environment';
import {AnswerResponseDto, RoomCode} from './room.models';
import {
  AddRankingPositionRequest,
  GameSessionPlayerResponse,
  RankAnswerResultResponse,
  RankedAnswerListResponse,
} from './game.models';

@Injectable({
  providedIn: 'root',
})
export class GameApiService {
  private readonly roomsUrl = `${environment.apiBaseUrl}/rooms`;

  private readonly http = inject(HttpClient);

  getActivePlayers(roomCode: RoomCode): Observable<GameSessionPlayerResponse[]> {
    return this.http.get<GameSessionPlayerResponse[]>(
      `${this.roomsUrl}/${encodeURIComponent(roomCode)}/ranking-game/current-round/players`,
    );
  }

  getSubmittedAnswers(roomCode: RoomCode, roundId: string, playerId: string): Observable<AnswerResponseDto> {
    return this.http.get<AnswerResponseDto>(
      `${this.roomsUrl}/${encodeURIComponent(roomCode)}/ranking-game/rounds/${encodeURIComponent(roundId)}/answers`,
      {params: {playerId}},
    );
  }

  addRankingPosition(
    roomCode: RoomCode,
    roundId: string,
    request: AddRankingPositionRequest,
  ): Observable<RankAnswerResultResponse> {
    return this.http.post<RankAnswerResultResponse>(
      `${this.roomsUrl}/${encodeURIComponent(roomCode)}/ranking-game/rounds/${encodeURIComponent(roundId)}/answer/position/new`,
      request,
    );
  }

  getRankingPositions(
    roomCode: RoomCode,
    roundId: string,
    playerId: string,
  ): Observable<RankedAnswerListResponse> {
    return this.http
      .get<RankedAnswerListResponse>(
        `${this.roomsUrl}/${encodeURIComponent(roomCode)}/ranking-game/rounds/${encodeURIComponent(roundId)}/answer/position/all`,
        {params: {playerId}},
      );
  }
}
