import {ComponentFixture, TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';

import {provideTestingTranslations} from '../core/i18n/translate-testing.providers';
import {ErrorDataWriterService} from '../error-data-writer-service';
import {ErrorComponent} from './error';

describe('ErrorComponent', () => {
  let component: ErrorComponent;
  let fixture: ComponentFixture<ErrorComponent>;
  let errorDataWriter: ErrorDataWriterService;

  function configureComponent(): Promise<void> {
    return TestBed.configureTestingModule({
      imports: [ErrorComponent],
      providers: [
        provideTestingTranslations(),
        provideRouter([]),
      ],
    }).compileComponents();
  }

  beforeEach(async () => {
    await configureComponent();

    errorDataWriter = TestBed.inject(ErrorDataWriterService);
    fixture = TestBed.createComponent(ErrorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows a token expired message for expired 401 responses', async () => {
    TestBed.resetTestingModule();
    await configureComponent();

    errorDataWriter = TestBed.inject(ErrorDataWriterService);
    errorDataWriter.errorData.set({
      status: 401,
      errorKey: 'TOKEN_EXPIRED',
      message: 'User session is expired.',
    });

    fixture = TestBed.createComponent(ErrorComponent);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Sitzung abgelaufen');
    expect(text).toContain('401');
    expect(text).toContain('User session is expired.');
  });

  it('shows an invalid token message for non-expired 401 responses', async () => {
    TestBed.resetTestingModule();
    await configureComponent();

    errorDataWriter = TestBed.inject(ErrorDataWriterService);
    errorDataWriter.errorData.set({
      status: 401,
      errorKey: 'TOKEN_NOT_AUTHORIZED',
      message: 'User is not authorized to access backend.',
    });

    fixture = TestBed.createComponent(ErrorComponent);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Ungültige Sitzung');
    expect(text).toContain('User is not authorized to access backend.');
  });
});
