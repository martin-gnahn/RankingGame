import {Component, input, output} from '@angular/core';
import {ReactiveFormsModule} from '@angular/forms';
import {TranslatePipe} from '@ngx-translate/core';

import {ActiveRoundResponse} from '../../core/api/room.models';
import {AnswerSubmissionState} from '../game-view.models';
import {ScoreCard as ScoreCardComponent} from '../score-card/score-card';

@Component({
  selector: 'app-answer-form',
  imports: [ReactiveFormsModule, TranslatePipe, ScoreCardComponent],
  templateUrl: './answer-form.html',
  styleUrl: './answer-form.scss',
})
export class AnswerForm {
  readonly round = input.required<ActiveRoundResponse>();
  readonly state = input.required<AnswerSubmissionState>();
  readonly answerSubmitted = output<void>();
}
