import {Component, input} from '@angular/core';
import {TranslatePipe} from '@ngx-translate/core';

import {ActiveRoundResponse} from '../../core/api/room.models';

@Component({
  selector: 'app-question',
  imports: [TranslatePipe],
  templateUrl: './question.html',
  styleUrl: './question.scss',
})
export class Question {
  readonly round = input.required<ActiveRoundResponse>();
}
