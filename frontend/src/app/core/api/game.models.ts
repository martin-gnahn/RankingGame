export interface GameSessionPlayerResponse {
  gameSessionId: string;
  playerId: string;
}

export interface AddRankingPositionRequest {
  hostId: string;
  answerId: string;
}

export interface RankedAnswerDto {
  rankingId: string;
  answerId: string;
  playerId: string;
  answerText: string;
  oneBasedPosition: number;
}

export type ValueObjectResponse = string | { value?: string } | null | undefined;

export interface RankedAnswerResponse {
  id?: ValueObjectResponse;
  answer?: {
    playerId?: ValueObjectResponse;
    answerId?: ValueObjectResponse;
    answerText?: ValueObjectResponse;
  } | null;
  oneBasedPosition: number;
}
