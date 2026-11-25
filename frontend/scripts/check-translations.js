import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { supportedLanguages } from '../src/localeConfig.js';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const frontendDirectory = path.resolve(scriptDirectory, '..');
const sourcePath = path.resolve(frontendDirectory, 'src/locales/en/translation.json');
const source = JSON.parse(fs.readFileSync(sourcePath, 'utf8'));
const sourceKeys = new Set(Object.keys(source));
const placeholders = (value) => [...value.matchAll(/\{(\w+)\}/g)]
  .map((match) => match[1])
  .sort()
  .join(',');

// Only plain t('literal key') calls are translation keys. Dynamic keys,
// member calls such as foo.t(...), and other non-translation t() helpers are
// deliberately ignored.
const literalTranslationCall = /(?<![\w$.])t\(\s*(['"`])((?:\\.|(?!\1)[^\\])*)\1/g;

function sourceFiles(directory) {
  return fs.readdirSync(directory, { withFileTypes: true })
    .sort((left, right) => left.name.localeCompare(right.name))
    .flatMap((entry) => {
      const filePath = path.join(directory, entry.name);
      if (entry.isDirectory()) return sourceFiles(filePath);
      if (!/\.(?:js|jsx)$/.test(entry.name)) return [];
      if (/\.(?:test|spec)\.[^.]+$/.test(entry.name)) return [];
      return [filePath];
    });
}

function usedTranslationKeys() {
  const used = new Map();
  for (const file of sourceFiles(path.resolve(frontendDirectory, 'src'))) {
    const contents = fs.readFileSync(file, 'utf8');
    for (const match of contents.matchAll(literalTranslationCall)) {
      const key = match[2].replace(/\\(['"`\\])/g, '$1');
      if (key.includes('${')) continue;
      if (!used.has(key)) used.set(key, []);
      used.get(key).push(path.relative(frontendDirectory, file));
    }
  }
  return used;
}

let failed = false;
for (const { code } of supportedLanguages) {
  const file = path.resolve(frontendDirectory, `src/locales/${code}/translation.json`);
  const translations = JSON.parse(fs.readFileSync(file, 'utf8'));
  const keys = Object.keys(translations);
  const unknownKeys = keys.filter((key) => !sourceKeys.has(key));
  const invalidPlaceholders = keys.filter((key) => (
    sourceKeys.has(key) && placeholders(source[key]) !== placeholders(translations[key])
  ));
  const missingKeys = [...sourceKeys].filter((key) => !(key in translations));

  if (unknownKeys.length || invalidPlaceholders.length || missingKeys.length) {
    failed = true;
    console.error(`${code}: invalid translation catalog`);
    if (missingKeys.length) console.error(`  Missing keys: ${missingKeys.join(', ')}`);
    if (unknownKeys.length) console.error(`  Unknown keys: ${unknownKeys.join(', ')}`);
    if (invalidPlaceholders.length) {
      console.error(`  Placeholder mismatch: ${invalidPlaceholders.join(', ')}`);
    }
    continue;
  }
  console.log(`${code}: ${keys.length}/${sourceKeys.size} translated`);
}

const usedKeys = usedTranslationKeys();
const missingSourceKeys = [...usedKeys].filter(([key]) => !sourceKeys.has(key));
if (missingSourceKeys.length) {
  failed = true;
  console.error('Source uses t() keys missing from en:');
  for (const [key, files] of missingSourceKeys) {
    console.error(`  ${key} (${files.join(', ')})`);
  }
} else {
  console.log(`source: all ${usedKeys.size} literal t() keys present in en`);
}

if (failed) process.exitCode = 1;
