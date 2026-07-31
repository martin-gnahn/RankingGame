import {TestBed} from '@angular/core/testing';

import {ErrorDataReaderService} from './error-data-reader-service';

describe('ErrorDataReaderService', () => {
  let service: ErrorDataReaderService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ErrorDataReaderService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
