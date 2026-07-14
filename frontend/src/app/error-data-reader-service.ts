import {computed, inject, Injectable} from '@angular/core';
import {ErrorDataWriterService} from './error-data-writer-service';

@Injectable({
  providedIn: 'root',
})
export class ErrorDataReaderService {
  writingErrorDataService = inject(ErrorDataWriterService);

  errorData = this.writingErrorDataService.errorData.asReadonly();
  errorKey = computed(() => this.errorData()?.errorKey);
  errorStatus = computed(() => this.errorData()?.status);
  errorMessage = computed(() => this.errorData()?.message);
}
