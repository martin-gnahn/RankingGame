import {FormControl, FormGroup} from '@angular/forms';

import {RankedAnswerDto} from '../core/api/game.models';
import {AnswerDto} from '../core/api/room.models';

export interface ScoreCard {
  value: number;
  tone: 'low' | 'middle' | 'high';
}

export interface RankedAnswerView extends RankedAnswerDto {
  nickname: string;
  cardValue?: AnswerDto['cardValue'];
}

export type AnswerFormGroup = FormGroup<{
  answerText: FormControl<string>;
}>;
