import {Component, input} from '@angular/core';
import {TranslatePipe} from '@ngx-translate/core';

import {RankedAnswerView} from '../game-view.models';

@Component({
  selector: 'app-ranking-overview',
  imports: [TranslatePipe],
  templateUrl: './ranking-overview.html',
  styleUrl: './ranking-overview.scss',
})
export class RankingOverview {
  readonly rankedAnswers = input.required<RankedAnswerView[]>();
  readonly rankingLoading = input(false);
  readonly isCurrentPlayerCaptain = input(false);
}
