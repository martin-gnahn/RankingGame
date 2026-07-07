import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {map, Observable} from 'rxjs';

import {environment} from '../../../environments/environment';
import {AnswerResponseDto, RoomCode} from './room.models';
import {
  AddRankingPositionRequest,
  GameSessionPlayerResponse,
  RankedAnswerDto,
  RankedAnswerResponse,
  ValueObjectResponse,
} from './game.models';

@Injectable({
  providedIn: 'root',
})
export class GameApiService {
  private readonly roomsUrl = `${environment.apiBaseUrl}/rooms`;

  constructor(private readonly http: HttpClient) {}

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
  ): Observable<unknown> {
    return this.http.post(
      `${this.roomsUrl}/${encodeURIComponent(roomCode)}/ranking-game/rounds/${encodeURIComponent(roundId)}/answer/position/new`,
      request,
    );
  }

  getRankingPositions(
    roomCode: RoomCode,
    roundId: string,
    playerId: string,
  ): Observable<RankedAnswerDto[]> {
    return this.http
      .get<RankedAnswerResponse[]>(
        `${this.roomsUrl}/${encodeURIComponent(roomCode)}/ranking-game/rounds/${encodeURIComponent(roundId)}/answer/position/all`,
        {params: {playerId}},
      )
      .pipe(map((answers) => answers.map((answer) => this.toRankedAnswer(answer))));
  }

  private toRankedAnswer(response: RankedAnswerResponse): RankedAnswerDto {
    return {
      rankingId: this.valueOf(response.id),
      answerId: this.valueOf(response.answer?.answerId),
      playerId: this.valueOf(response.answer?.playerId),
      answerText: this.valueOf(response.answer?.answerText),
      oneBasedPosition: response.oneBasedPosition,
    };
  }

  private valueOf(value: ValueObjectResponse): string {
    if (typeof value === 'string') {
      return value;
    }

    return value?.value ?? '';
  }
}
