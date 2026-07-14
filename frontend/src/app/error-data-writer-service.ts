import {Injectable, signal} from '@angular/core';

export interface ErrorData {
  status: number;
  errorKey: string;
  message: string;
}

@Injectable({
  providedIn: 'root',
})
export class ErrorDataWriterService {
  errorData = signal<ErrorData | null>(null);
}
