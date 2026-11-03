// Configuration of @hey-api/openapi-ts: `npm run api:generate` writes the typed client of the REST
// API into src/app/core/api/ from the committed openapi.json (`npm run api:sync`). Never edit the
// generated files: change the backend, sync the document and generate again. `npm run api:check`
// generates into a temporary folder (OPENAPI_TS_OUTPUT) and fails when the committed files differ.
import { defineConfig } from '@hey-api/openapi-ts';

const API_PREFIX = '/api/1.0';

const pascal = (word) => word.charAt(0).toUpperCase() + word.slice(1);

/**
 * The name of an operation from its method and path, e.g. `PUT /api/1.0/mobilities/{id}/cancel`
 * gives `putMobilitiesByIdCancel`. springdoc's operation ids are the Java method names with a
 * numeric suffix on duplicates (`showAll_7`), which depends on the order it scans the controllers
 * in: names built from the path only change when the API does.
 */
export function operationName(method, path) {
  const words = path
    .replace(API_PREFIX, '')
    .split('/')
    .filter(Boolean)
    .flatMap((segment) => {
      const parameter = /^\{(.+)\}$/.exec(segment);
      return parameter ? ['by', parameter[1]] : segment.split(/[^A-Za-z0-9]+/);
    })
    .filter(Boolean);
  return method.toLowerCase() + words.map(pascal).join('');
}

export default defineConfig({
  input: './openapi.json',
  output: {
    path: process.env.OPENAPI_TS_OUTPUT || './src/app/core/api',
  },
  parser: {
    patch: {
      operations: (method, path, operation) => {
        operation.operationId = operationName(method, path);
      },
    },
  },
  plugins: [
    // the HttpClient based runtime, used by ApiService only to build HttpRequest objects
    '@hey-api/client-angular',
    // the schema types (UserDto, MobilityDto...) and the Data/Response types of every operation
    '@hey-api/typescript',
    // one `<operation>Request(options)` function per operation, returning an HttpRequest that
    // ApiService sends with the application's HttpClient (and therefore its interceptors)
    { name: '@angular/common', httpRequests: true, httpResources: false },
  ],
});
