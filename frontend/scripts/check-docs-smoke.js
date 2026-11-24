#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(process.cwd(), '..');
const checks = [
  { file: 'README.md', patterns: ['## Licensing and activation', '1qaz2wsx3edc', '/api/v1/license/status', '/api/v1/license/activate'] },
  { file: 'docs/backend.md', patterns: ['## Licensing', 'Flyway', 'ddl-auto=validate', '1qaz2wsx3edc'] },
  { file: 'docs/frontend.md', patterns: ['License activation', 'license gate'] },
  { file: 'docs/architecture.md', patterns: ['Licensing', 'LicenseEnforcementFilter'] },
  { file: 'frontend/src/components/HelpButton.jsx', patterns: ['License activation', 'Activate/manage license & customer'] },
];
let failed = false;
for (const { file, patterns } of checks) {
  const text = fs.readFileSync(path.join(root, file), 'utf8');
  for (const pattern of patterns) {
    if (!text.includes(pattern)) {
      console.error(`MISSING ${pattern} in ${file}`);
      failed = true;
    }
  }
}
if (failed) process.exit(1);
console.log('docs/help smoke checks passed');
