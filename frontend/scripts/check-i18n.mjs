#!/usr/bin/env node
// CI guard for the @angular/localize catalogs (run with `npm run i18n:check`).
//
// 1. Re-extracts the messages from the sources into a temporary folder and compares them with
//    the committed src/locale/messages.xlf (ids + source text; line numbers are ignored).
//    Fails when a message was added, removed or changed without running `npm run i18n:extract`.
// 2. For every translation file declared in angular.json (`i18n.locales`), checks that each
//    message has a non-empty <target>, that the <source> copy matches the current source text
//    (so edited French text flags the translation as outdated), that placeholders match, and
//    that no obsolete ids remain.
// The build itself (`i18nMissingTranslation: "error"`) already fails on missing translations;
// this script adds the "catalog is stale / translation outdated" checks.
import { spawnSync } from 'node:child_process';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { createRequire } from 'node:module';
import { tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const require = createRequire(import.meta.url);
const sourceCatalog = join(root, 'src/locale/messages.xlf');

function parseUnits(xml) {
  const units = new Map();
  for (const match of xml.matchAll(/<trans-unit\s+id="([^"]+)"[^>]*>([\s\S]*?)<\/trans-unit>/g)) {
    const [, id, body] = match;
    const source = /<source>([\s\S]*?)<\/source>/.exec(body)?.[1];
    const target = /<target[^>]*>([\s\S]*?)<\/target>/.exec(body)?.[1];
    units.set(id, { source, target });
  }
  return units;
}

function placeholders(text = '') {
  return [...text.matchAll(/<x\s+id="([^"]+)"/g)].map((m) => m[1]).sort().join(',');
}

const problems = [];

// 1. Source catalog freshness.
const tmp = mkdtempSync(join(tmpdir(), 'ema-i18n-'));
try {
  const ng = require.resolve('@angular/cli/bin/ng.js');
  const result = spawnSync(process.execPath, [ng, 'extract-i18n', '--output-path', tmp], {
    cwd: root,
    stdio: ['ignore', 'ignore', 'inherit'],
  });
  if (result.status !== 0) {
    console.error('i18n:check: `ng extract-i18n` failed.');
    process.exit(result.status ?? 1);
  }
  const fresh = parseUnits(readFileSync(join(tmp, 'messages.xlf'), 'utf8'));
  const committed = parseUnits(readFileSync(sourceCatalog, 'utf8'));
  for (const [id, unit] of fresh) {
    if (!committed.has(id)) {
      problems.push(`messages.xlf: new message "${id}" is not extracted`);
    } else if (committed.get(id).source !== unit.source) {
      problems.push(`messages.xlf: message "${id}" changed in the sources`);
    }
  }
  for (const id of committed.keys()) {
    if (!fresh.has(id)) {
      problems.push(`messages.xlf: message "${id}" no longer exists in the sources`);
    }
  }
  if (problems.length) {
    problems.push('-> run `npm run i18n:extract` and commit src/locale/messages.xlf');
  }

  // 2. Translations.
  const angular = JSON.parse(readFileSync(join(root, 'angular.json'), 'utf8'));
  for (const project of Object.values(angular.projects)) {
    for (const [locale, config] of Object.entries(project.i18n?.locales ?? {})) {
      const files = [].concat(typeof config === 'string' ? config : config.translation ?? []);
      for (const file of files) {
        const translated = parseUnits(readFileSync(join(root, file), 'utf8'));
        for (const [id, unit] of fresh) {
          const t = translated.get(id);
          if (!t || !t.target || !t.target.trim()) {
            problems.push(`${file}: missing ${locale} translation for "${id}"`);
          } else if (t.source !== unit.source) {
            problems.push(`${file}: translation of "${id}" is outdated (French source changed)`);
          } else if (placeholders(t.target) !== placeholders(unit.source)) {
            problems.push(`${file}: placeholders of "${id}" differ from the source`);
          }
        }
        for (const id of translated.keys()) {
          if (!fresh.has(id)) {
            problems.push(`${file}: obsolete translation "${id}"`);
          }
        }
      }
    }
  }
} finally {
  rmSync(tmp, { recursive: true, force: true });
}

if (problems.length) {
  console.error('i18n:check failed:\n  ' + problems.join('\n  '));
  process.exit(1);
}
console.log('i18n:check: message catalog and translations are up to date.');
