import {CdkDragDrop, DragDropModule} from '@angular/cdk/drag-drop';
import {Component, input, output} from '@angular/core';
import {TranslatePipe} from '@ngx-translate/core';

import {AnswerDto} from '../../core/api/room.models';
import {RankedAnswerView} from '../game-view.models';

@Component({
  selector: 'app-ranking-answer',
  imports: [DragDropModule, TranslatePipe],
  templateUrl: './ranking-answer.html',
  styleUrl: './ranking-answer.scss',
})
export class RankingAnswer {
  readonly availableAnswers = input.required<AnswerDto[]>();
  readonly rankedAnswers = input.required<RankedAnswerView[]>();
  readonly rankingIsComplete = input(false);
  readonly rankingDropDisabled = input(false);
  readonly rankingSubmittingAnswerId = input<string | null>(null);
  readonly answerRanked = output<AnswerDto>();

  protected rankDroppedAnswer(event: CdkDragDrop<RankedAnswerView[], AnswerDto[], AnswerDto>): void {
    const answer = event.item.data as AnswerDto | undefined;
    const droppedIntoSameContainer =
      (event.previousContainer as unknown) === (event.container as unknown);
    if (!answer || droppedIntoSameContainer) {
      return;
    }

    this.answerRanked.emit(answer);
  }
}
