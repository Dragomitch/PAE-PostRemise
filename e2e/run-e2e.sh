#!/usr/bin/env bash
# End-to-end run against a backend that is already up (the jar on a freshly loaded database):
#   1. waits for the backend (GET /api/1.0/options)
#   2. api_e2e.py: sign-ups, every endpoint, the whole mobility lifecycle
#   3. starts the Angular dev server (ng serve) with /api proxied to the backend, waits for it
#   4. ui_e2e.js with DATECHECK=1: Angular sign-up/sign-in and every legacy UI view
#   5. mobility_detail.js: payment statuses of the completed mobility
# then stops the dev server. Exits 1 if a step failed. See e2e/README.md.
#
# Environment (defaults in brackets):
#   APP_URL          backend origin, serves the legacy UI and the API [http://localhost:8080]
#   API_URL          REST API base [$APP_URL/api/1.0]
#   NG_PORT          port of the Angular dev server [4200]
#   NG_URL           Angular app [http://localhost:$NG_PORT]
#   SCREENSHOT_DIR   screenshots [e2e/screenshots]
#   NG_LOG           dev server log [e2e/ng-serve.log]
#   BACKEND_TIMEOUT  seconds to wait for the backend [120]
#   NG_TIMEOUT       seconds to wait for the dev server [300]
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(dirname "$HERE")"

export APP_URL="${APP_URL:-http://localhost:8080}"
APP_URL="${APP_URL%/}"
export API_URL="${API_URL:-$APP_URL/api/1.0}"
NG_PORT="${NG_PORT:-4200}"
export NG_URL="${NG_URL:-http://localhost:$NG_PORT}"
export SCREENSHOT_DIR="${SCREENSHOT_DIR:-$HERE/screenshots}"
NG_LOG="${NG_LOG:-$HERE/ng-serve.log}"
BACKEND_TIMEOUT="${BACKEND_TIMEOUT:-120}"
NG_TIMEOUT="${NG_TIMEOUT:-300}"
export NG_CLI_ANALYTICS=false

mkdir -p "$SCREENSHOT_DIR"
TMP="$(mktemp -d)"
NG_PID=""
# shellcheck disable=SC2329  # called by the EXIT trap
cleanup() {
  if [ -n "$NG_PID" ] && kill -0 "$NG_PID" 2>/dev/null; then
    echo "--- stopping ng serve (pid $NG_PID)"
    kill "$NG_PID" 2>/dev/null
    wait "$NG_PID" 2>/dev/null
  fi
  rm -rf "$TMP"
}
trap cleanup EXIT
trap 'exit 130' INT TERM

# wait_for URL SECONDS [PID]: polls URL until it answers 2xx; gives up early if PID dies
wait_for() {
  local url="$1" limit="$2" pid="${3:-}" start=$SECONDS
  until curl -fsS -o /dev/null --max-time 5 "$url" 2>/dev/null; do
    if [ -n "$pid" ] && ! kill -0 "$pid" 2>/dev/null; then
      echo "process $pid exited before $url answered"; return 1
    fi
    if (( SECONDS - start >= limit )); then
      echo "$url did not answer within ${limit}s"; return 1
    fi
    sleep 2
  done
  echo "$url is up after $(( SECONDS - start ))s"
}

declare -a SUMMARY=()
FAILED=0
step() {  # step NAME COMMAND...: runs a step, records PASS/FAIL
  local name="$1"; shift
  echo; echo "=== $name"
  if "$@"; then SUMMARY+=("PASS $name"); else SUMMARY+=("FAIL $name"); FAILED=1; fi
}
finish() {
  echo; echo "=== summary"
  printf '%s\n' "${SUMMARY[@]}"
  exit "$FAILED"
}

echo "backend $APP_URL, API $API_URL, Angular $NG_URL, screenshots $SCREENSHOT_DIR"

step "backend is up" wait_for "$API_URL/options" "$BACKEND_TIMEOUT"
[ "$FAILED" = 0 ] || finish

step "API checks (api_e2e.py)" python3 "$HERE/api_e2e.py"

# The dev server proxies the same paths as frontend/proxy.conf.json (/api, the flag images...),
# every target replaced by the backend under test, so a path added there is covered here too.
python3 - "$ROOT/frontend/proxy.conf.json" "$TMP/proxy.conf.json" "$APP_URL" <<'PY'
import json, sys
source, target, app_url = sys.argv[1:]
proxy = json.load(open(source))
for rule in proxy.values():
    rule["target"] = app_url
json.dump(proxy, open(target, "w"), indent=2)
PY
echo; echo "=== starting ng serve on port $NG_PORT (log: $NG_LOG)"
( cd "$ROOT/frontend" && exec ./node_modules/.bin/ng serve --port "$NG_PORT" \
    --proxy-config "$TMP/proxy.conf.json" --no-open --no-live-reload ) > "$NG_LOG" 2>&1 &
NG_PID=$!
if ! wait_for "$NG_URL/" "$NG_TIMEOUT" "$NG_PID"; then
  tail -n 50 "$NG_LOG"
  SUMMARY+=("FAIL Angular dev server is up"); FAILED=1
  finish
fi
SUMMARY+=("PASS Angular dev server is up")

step "UI walk (ui_e2e.js, DATECHECK=1)" env DATECHECK=1 node "$HERE/ui_e2e.js"
step "mobility detail (mobility_detail.js)" node "$HERE/mobility_detail.js"

finish
