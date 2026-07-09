// @ts-ignore
import de from '../public/i18n/de.json';

type TranslationKey = keyof typeof de;
type TranslationParams = Record<string, string | number>;

const placeholderPattern = /\{\{\s*(\w+)\s*}}/g;

export function t(key: TranslationKey, params: TranslationParams = {}): string {
  return renderTranslation(key, params, (value) => String(value));
}

function renderTranslation(
  key: TranslationKey,
  params: TranslationParams,
  renderParam: (value: string | number) => string,
): string {
  return translationTemplate(key).replace(placeholderPattern, (_, paramName: string) => {
    const paramValue = params[paramName];

    if (paramValue === undefined) {
      throw new Error(`Missing translation param "${paramName}" for key "${String(key)}"`);
    }

    return renderParam(paramValue);
  });
}

function translationTemplate(key: TranslationKey): string {
  return de[key];
}
