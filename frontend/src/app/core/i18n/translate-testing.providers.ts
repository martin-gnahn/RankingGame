import {Provider} from '@angular/core';
import {provideTranslateLoader, provideTranslateService, TranslateLoader} from '@ngx-translate/core';
import {Observable, of} from 'rxjs';

const testingTranslationUrl = '/i18n/de.json';

class TestingTranslateLoader extends TranslateLoader {
  override getTranslation(): Observable<Record<string, string>> {
    return of(loadTestingTranslations());
  }
}

function loadTestingTranslations(): Record<string, string> {
  const request = new XMLHttpRequest();
  request.open('GET', testingTranslationUrl, false);
  request.send();

  if (request.status < 200 || request.status >= 300) {
    throw new Error(`Could not load test translations from ${testingTranslationUrl}`);
  }

  return JSON.parse(request.responseText) as Record<string, string>;
}

export function provideTestingTranslations(): Provider[] {
  return provideTranslateService({
    loader: provideTranslateLoader(() => new TestingTranslateLoader()),
    fallbackLang: 'de',
    lang: 'de',
  });
}
