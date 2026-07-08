import {Component, input} from '@angular/core';
import {TranslatePipe} from '@ngx-translate/core';

import {ScoreCard as ScoreCardView} from '../game-view.models';

@Component({
  selector: 'app-score-card',
  imports: [TranslatePipe],
  templateUrl: './score-card.html',
  styleUrl: './score-card.scss',
})
export class ScoreCard {
  readonly assignedCardValue = input.required<number>();
  readonly scoreCards = input.required<ScoreCardView[]>();

  protected isAssignedCard(cardValue: number): boolean {
    return Number(this.assignedCardValue()) === cardValue;
  }
}
