# End-to-end tests

These scripts drive the packaged application (the Spring Boot jar on a real PostgreSQL) the way a
user would. They run in CI as the `e2e` job of `.github/workflows/ci.yml` and can be run locally
with the same runner.

| File | What it does |
|---|---|
| `run-e2e.sh` | Runner: waits for the backend, runs the three scripts below in order, starts and stops the Angular dev server, prints a summary and exits 1 if a step failed |
| `api_e2e.py` | API scenario (Python standard library only): 159 checks |
| `ui_e2e.js` | Playwright (Chromium) walk of the Angular sign-up/sign-in, of every legacy UI view and of the Angular mobility choices, personal data, users and mobilities screens: 43 checks |
| `mobility_detail.js` | Playwright check of the completed mobility's detail page: 4 checks |
| `package.json` | Pins Playwright (exact version) |

## What is checked

`api_e2e.py` signs up a professor (the first sign-up) and two students, signs them in, then goes
through every endpoint: reference data (options, countries, programmes), denial reasons,
partners (creation, options, filters, optimistic locking), mobility choices (creation, filters,
count, cancel, reject, CSV export, confirm), the nominated student, and a full mobility lifecycle
(ProEco and second software encodings, the 9 Erasmus+ documents, both payments, the state moving
from `A payer` to `Terminée`, document exports, a stale-version update refused with 409, a
cancellation). It also checks user promotion, profile edition and sign-out, and the expected
error statuses (403, 404, 409, 400). Each request and each assertion prints one `PASS` or `FAIL`
line (`-q` prints only the failures). A response the scenario cannot use stops it with a
`FAIL aborted` line; the script exits 1 if any check failed.

`ui_e2e.js` signs up and signs in a new student in the Angular app, then signs in to the legacy UI
as `prof` and as `alice` (created by the API script) and opens every view of their menu. A view
must appear, and the page must log no JavaScript error and get no HTTP 5xx. With `DATECHECK=1`
(the runner sets it) the payment dates of `/paiements` must match the dates returned by the API,
and the birthdate field of `/mes-donnees-personnelles` must show the API birthdate as `d/m/yyyy`.
Last, after the legacy walk so that it sees the data of the API script unchanged, it opens the
Angular mobility choices screen (`/mobility-choices`): as `alice`, the rows must match the API
count, and a choice created through the form (Erasmus+, France, `Universite de Lyon`, September of
the current year) must be stored and listed; as `prof`, the active list must match the count shown
and the API count, alice's new choice is rejected with a denial reason and leaves the active list,
and the `all` filter must list as many rows as the count shown. Only that new choice is changed,
so `mobility_detail.js` still finds its completed mobility. Then, as `alice`, the Angular personal
data screen (`/personal-data`) must show her record as `GET /nominatedStudents/{id}` returns it, and
a phone number changed through the form must be returned by the API with the next version; as
`prof`, the users screen (`/users`) must list the accounts of `GET /users` with their role, offer
"Promouvoir" to the students only, and ask for a confirmation, which the walk cancels: nobody is
promoted, so the roles stay as the API script left them. Last, read only, the Angular mobilities
screen (`/mobilities`): as `prof`, the rows must be the mobilities of `GET /mobilities` with their
state, and the detail of the completed mobility (the one `mobility_detail.js` checks on the legacy
UI) must show its state, no button, and the days of its two payments as the API returns them; as
`alice`, the list must hold her mobilities of `GET /mobilities`.

`mobility_detail.js` opens the completed mobility as `prof` and checks that both payment statuses
read `Effectué le d/m/yyyy` with the dates returned by the API.

The UI scripts depend on the data created by `api_e2e.py`, so the database must be freshly loaded
from `SQLRessources/init.sql` before a run (`init.sql` drops and recreates the schema, so loading
it again resets the data).

## Ports and environment

| Variable | Default | Used by |
|---|---|---|
| `APP_URL` | `http://localhost:8080` | backend origin: legacy UI and API (runner, UI scripts) |
| `API_URL` | `$APP_URL/api/1.0` | `api_e2e.py` |
| `NG_PORT` | `4200` | port of `ng serve` (runner) |
| `NG_URL` | `http://localhost:$NG_PORT` | Angular app (`ui_e2e.js`) |
| `SCREENSHOT_DIR` | `e2e/screenshots` | screenshots of every view |
| `NG_LOG` | `e2e/ng-serve.log` | dev server log |
| `BACKEND_TIMEOUT`, `NG_TIMEOUT` | `120`, `300` | seconds to wait for the backend and the dev server |

The runner starts `ng serve` from `frontend/` with a generated proxy file that sends `/api` to
`APP_URL` (the committed `frontend/proxy.conf.json` targets port 8080). Screenshots and logs are
git-ignored; in CI they are uploaded as the `e2e-results` artifact, with the app log.

## Running locally

Prerequisites: Java 21 and Maven, Node 20+, Python 3, `psql`, a PostgreSQL database, and
`npm ci` done in `frontend/`.

```bash
# 1. Playwright and its browser (once)
cd e2e && npm ci && npx playwright install chromium && cd ..

# 2. A fresh schema (repeat before every run)
psql -h localhost -U pguser01 -d testdb -v ON_ERROR_STOP=1 -f SQLRessources/init.sql

# 3. The jar, configured like docker-compose.yml
mvn -B -ntp -q package -DskipTests -Djacoco.skip=true
DB_HOST=localhost DB_PORT=5432 DB_NAME=testdb DB_USERNAME=pguser01 \
DB_PASSWORD=yoursecurepassword JWT_SECRET="$(openssl rand -hex 32)" \
  java -jar target/PAE-Gestionnaire-Erasmus-1.0-SNAPSHOT.jar &

# 4. The suite
e2e/run-e2e.sh
```

To run on other ports (for instance next to a running Docker Compose stack), start the jar with
`SERVER_PORT=8081` (Spring Boot binds it to `server.port`) and run
`APP_URL=http://localhost:8081 NG_PORT=4201 e2e/run-e2e.sh`.

The scripts can also be run one by one against a running stack, for instance
`API_URL=http://localhost:8081/api/1.0 python3 e2e/api_e2e.py`, `APP_URL=... NG_URL=...
DATECHECK=1 node e2e/ui_e2e.js` and `APP_URL=... node e2e/mobility_detail.js`.
