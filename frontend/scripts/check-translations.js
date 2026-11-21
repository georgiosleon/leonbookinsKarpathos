import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { supportedLanguages } from '../src/localeConfig.js';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const sourcePath = path.resolve(scriptDirectory, '../src/locales/en/translation.json');
const source = JSON.parse(fs.readFileSync(sourcePath, 'utf8'));
const sourceKeys = new Set(Object.keys(source));
const placeholders = (value) => [...value.matchAll(/\{(\w+)\}/g)]
  .map((match) => match[1])
  .sort()
  .join(',');

let failed = false;
for (const { code } of supportedLanguages) {
  const file = path.resolve(scriptDirectory, `../src/locales/${code}/translation.json`);
  const translations = JSON.parse(fs.readFileSync(file, 'utf8'));
  const keys = Object.keys(translations);
  const unknownKeys = keys.filter((key) => !sourceKeys.has(key));
  const invalidPlaceholders = keys.filter((key) => (
    sourceKeys.has(key) && placeholders(source[key]) !== placeholders(translations[key])
  ));
  const missing = sourceKeys.size - keys.filter((key) => sourceKeys.has(key)).length;

  if (unknownKeys.length || invalidPlaceholders.length) {
    failed = true;
    console.error(`${code}: invalid translation catalog`);
    if (unknownKeys.length) console.error(`  Unknown keys: ${unknownKeys.join(', ')}`);
    if (invalidPlaceholders.length) {
      console.error(`  Placeholder mismatch: ${invalidPlaceholders.join(', ')}`);
    }
    continue;
  }
  console.log(`${code}: ${sourceKeys.size - missing}/${sourceKeys.size} translated; missing keys use English fallback`);
}

if (failed) process.exitCode = 1;
