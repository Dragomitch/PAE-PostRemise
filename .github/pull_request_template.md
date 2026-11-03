## Summary

<!-- What changes and why, in two or three sentences. -->

## Issue

<!-- Every change comes through a PR that links its issue. Closes the issue when the PR is merged. -->

Closes #

## Changes

<!-- One bullet per notable change; name the files or classes that matter to the reviewer. -->

-

## Test plan

<!--
State what was run and the numbers it produced, never "should work". Tick a box only for a
command you ran on this branch, and write its result after the colon. Keep the lines that apply,
delete the others, add the end-to-end or manual checks that cover the change.
-->

- [ ] `mvn -B -ntp clean verify`: N unit tests, N integration tests, 0 failures, coverage gates met
- [ ] `mvn -B -ntp clean verify -Dsurefire.runOrder=random -Dfailsafe.runOrder=random`: N unit tests, N integration tests, 0 failures
- [ ] `npm run test:ci` (in `frontend/`): N tests, 0 failures, coverage thresholds met
- [ ] `docker compose config --quiet`: valid

## Screenshots

<!--
For a change to a screen; write "none" otherwise. Commit the images under
docs/screenshots/<topic>/ and embed each one as an HTML image wrapped in a link to its page, so
that it previews inline and opens in full on click (HTML, not the Markdown [![..](..)](..) form,
which the agents' GitHub tooling stores wrapped in backticks):

<a href="https://github.com/Dragomitch/PAE-PostRemise/blob/<commit-sha>/docs/screenshots/<topic>/<file>.png"><img src="https://raw.githubusercontent.com/Dragomitch/PAE-PostRemise/<commit-sha>/docs/screenshots/<topic>/<file>.png" alt="alt text" width="700"></a>

Full size: https://github.com/Dragomitch/PAE-PostRemise/blob/<commit-sha>/docs/screenshots/<topic>/<file>.png

(the tooling sometimes wraps an image URL in backticks; the "Full size" line keeps a working link,
and one image per message, further ones in one comment each, is the safest)

<commit-sha> is the full sha of a commit of the branch that contains the image. On a Forgejo mirror
the image URL is https://<host>/Dragomitch/PAE-PostRemise/raw/commit/<commit-sha>/docs/screenshots/<topic>/<file>.png
and the page is the same path under /src/commit/<commit-sha>/.
-->

none
