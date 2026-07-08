import {Provider} from '@angular/core';
import {provideTranslateService} from '@ngx-translate/core';
import {provideTranslateHttpLoader} from '@ngx-translate/http-loader';

export function provideAppTranslations(): Provider[] {
  return provideTranslateService({
    loader: provideTranslateHttpLoader({
      prefix: './i18n/',
      suffix: '.json',
      failOnError: true,
    }),
    fallbackLang: 'de',
    lang: 'de',
  });
}
