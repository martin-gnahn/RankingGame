import de from '../public/i18n/de.json';

type TranslationDictionary = Record<string, unknown>;
type TranslationKey = string;
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
  const value = findTranslation(key);

  if (typeof value !== 'string') {
    throw new Error(`Missing translation for key "${key}"`);
  }

  return value;
}

function findTranslation(key: TranslationKey): unknown {
  const translations = de as TranslationDictionary;

  if (Object.prototype.hasOwnProperty.call(translations, key)) {
    return translations[key];
  }

  let current: unknown = translations;
  for (const segment of key.split('.')) {
    if (!isTranslationDictionary(current)) {
      return undefined;
    }

    current = current[segment];
  }

  return current;
}

function isTranslationDictionary(value: unknown): value is TranslationDictionary {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}
