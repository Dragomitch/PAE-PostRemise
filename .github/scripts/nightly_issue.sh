#!/usr/bin/env bash
# Opens, updates or closes the single "Nightly build failed" issue after a nightly run.
# Inputs (environment): SUITE_RESULT and DEPENDENCIES_RESULT (the results of the two jobs),
# RUN_URL; gh authenticates through GH_TOKEN and targets GH_REPO.
set -euo pipefail

title='Nightly build failed'
label='needs-triage'

failed=()
[ "${SUITE_RESULT:?}" = success ] || failed+=("suite (${SUITE_RESULT})")
[ "${DEPENDENCIES_RESULT:?}" = success ] || failed+=("dependencies (${DEPENDENCIES_RESULT})")

number="$(gh issue list --state open --label "$label" --search "\"$title\" in:title" \
  --json number,title --jq ".[] | select(.title == \"$title\") | .number" | head -n 1)"

if [ "${#failed[@]}" -gt 0 ]; then
  names="$(printf '%s, ' "${failed[@]}")"
  names="${names%, }"
  body="The nightly run of $(date -u +%F) failed.

Run: ${RUN_URL:?}
Failing jobs: ${names}

The reports (dependency updates, npm audit) are in the artifact \`nightly-reports\` of the run."
  if [ -n "$number" ]; then
    gh issue edit "$number" --body "$body"
    echo "Updated issue #$number"
  else
    gh issue create --title "$title" --label "$label" --body "$body"
  fi
elif [ -n "$number" ]; then
  gh issue close "$number" --comment "The nightly run of $(date -u +%F) is green again: ${RUN_URL:?}"
  echo "Closed issue #$number"
else
  echo "Green run, no open issue."
fi
