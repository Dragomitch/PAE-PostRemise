#!/usr/bin/env node
// CI guard for the generated API client (`npm run api:check`): generates the client from the
// committed openapi.json into a temporary folder (openapi-ts.config.mjs reads OPENAPI_TS_OUTPUT)
// and compares it, file by file, with the committed src/app/core/api/. Fails when openapi.json
// changed without `npm run api:generate`, or when a generated file was edited by hand.
import { spawnSync } from 'node:child_process';
import { existsSync, mkdtempSync, readdirSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const committed = join(root, 'src/app/core/api');

/** Every file under `dir`, as paths relative to it, sorted. */
function files(dir) {
  if (!existsSync(dir)) {
    return [];
  }
  return readdirSync(dir, { recursive: true, withFileTypes: true })
    .filter((entry) => entry.isFile())
    .map((entry) => relative(dir, join(entry.parentPath, entry.name)))
    .sort();
}

const temp = mkdtempSync(join(tmpdir(), 'openapi-client-'));
const generated = join(temp, 'api');
try {
  const run = spawnSync(
    process.execPath,
    [join(root, 'node_modules/@hey-api/openapi-ts/bin/run.js')],
    {
      cwd: root,
      env: { ...process.env, OPENAPI_TS_OUTPUT: generated },
      encoding: 'utf8',
    },
  );
  if (run.status !== 0) {
    console.error(run.stdout, run.stderr);
    console.error('api:check failed: the generator did not run');
    process.exit(1);
  }

  const expected = files(generated);
  const actual = files(committed);
  const problems = [
    ...expected.filter((file) => !actual.includes(file)).map((file) => `missing: ${file}`),
    ...actual.filter((file) => !expected.includes(file)).map((file) => `not generated: ${file}`),
    ...expected
      .filter((file) => actual.includes(file))
      .filter(
        (file) =>
          readFileSync(join(generated, file), 'utf8') !==
          readFileSync(join(committed, file), 'utf8'),
      )
      .map((file) => `differs: ${file}`),
  ];
  if (problems.length > 0) {
    console.error(
      'api:check failed: src/app/core/api/ is not the client generated from openapi.json',
    );
    problems.forEach((problem) => console.error(`  ${problem}`));
    console.error(
      'Run `npm run api:generate` (after `npm run api:sync` if the API changed) and commit the result.',
    );
    process.exit(1);
  }
  console.log(`api:check: the ${expected.length} generated files match openapi.json`);
} finally {
  rmSync(temp, { recursive: true, force: true });
}
