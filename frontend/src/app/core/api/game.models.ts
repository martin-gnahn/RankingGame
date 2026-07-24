export interface GameSessionPlayerResponse {
  gameSessionId: string;
  playerId: string;
}

export interface AddRankingPositionRequest {
  answerId: string;
}

export interface RankAnswerResultResponse {
  roundId: string;
  playerId: string;
  answerId: string;
  rankingId: string;
  oneBasedPosition: number;
}

export interface RankedAnswerDto {
  rankingId: string;
  answerId: string;
  playerId: string;
  answerText: string;
  oneBasedPosition: number;
}

export interface RankedAnswerListResponse {
  rankings: RankedAnswerDto[];
}
