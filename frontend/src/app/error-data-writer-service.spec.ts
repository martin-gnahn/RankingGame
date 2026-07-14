import {TestBed} from '@angular/core/testing';

import {ErrorDataWriterService} from './error-data-writer-service';

describe('WritingErrorDataService', () => {
  let service: ErrorDataWriterService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ErrorDataWriterService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
