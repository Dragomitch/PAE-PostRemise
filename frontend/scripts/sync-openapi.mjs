#!/usr/bin/env node
// Copies the OpenAPI document served by the backend into frontend/openapi.json (`npm run api:sync`),
// the input of the generated client (`npm run api:generate`, see openapi-ts.config.mjs).
//
//   API_DOCS_URL  where the document is served [http://localhost:8080/v3/api-docs]
//   --out <file>  where to write it [openapi.json next to package.json]
//
// The document is normalized so that the committed copy only changes when the API does:
// - object keys are sorted (recursively), arrays keep their order (it can be meaningful);
// - `servers` is dropped: springdoc fills it from the request (`http://localhost:<port>`), so it
//   differs between machines, and the client calls the API with relative URLs anyway;
// - two-space indentation and a final newline.
// The CI e2e job writes the document served by the built jar with --out and diffs it against the
// committed copy, with the same normalization.
import { writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');

/** Returns a copy of `value` with the keys of every object in alphabetical order. */
export function sortKeys(value) {
  if (Array.isArray(value)) {
    return value.map(sortKeys);
  }
  if (value !== null && typeof value === 'object') {
    return Object.fromEntries(
      Object.keys(value)
        .sort()
        .map((key) => [key, sortKeys(value[key])]),
    );
  }
  return value;
}

/** The document as it is committed: without `servers`, keys sorted, as indented JSON. */
export function normalize(document) {
  const { servers: _servers, ...rest } = document;
  return `${JSON.stringify(sortKeys(rest), null, 2)}\n`;
}

async function main() {
  const url = process.env.API_DOCS_URL || 'http://localhost:8080/v3/api-docs';
  const outIndex = process.argv.indexOf('--out');
  const out =
    outIndex >= 0 && process.argv[outIndex + 1]
      ? resolve(process.argv[outIndex + 1])
      : join(root, 'openapi.json');

  let response;
  try {
    response = await fetch(url, { headers: { Accept: 'application/json' } });
  } catch (error) {
    throw new Error(`cannot reach ${url} (is the backend running?): ${error.cause ?? error}`);
  }
  if (!response.ok) {
    throw new Error(`${url} answered ${response.status} ${response.statusText}`);
  }
  const document = await response.json();
  if (typeof document.openapi !== 'string' || typeof document.paths !== 'object') {
    throw new Error(`${url} did not answer an OpenAPI document`);
  }
  writeFileSync(out, normalize(document));
  console.log(
    `api:sync: ${Object.keys(document.paths).length} paths from ${url} written to ${out}`,
  );
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch((error) => {
    console.error(`api:sync failed: ${error.message}`);
    process.exit(1);
  });
}
