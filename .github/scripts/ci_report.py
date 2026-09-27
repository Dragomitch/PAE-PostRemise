#!/usr/bin/env python3
"""Builds the Markdown CI report (tests + coverage) for the job summary and the PR comment.

Usage: ci_report.py <artifacts-dir> <output.md>

Reads, when present:
  backend-reports/surefire-reports/TEST-*.xml, backend-reports/failsafe-reports/TEST-*.xml
  backend-reports/jacoco/jacoco.xml
  frontend-reports/junit/TEST-*.xml
  frontend-reports/coverage/cobertura-coverage.xml
Missing inputs are reported as such instead of failing, so a broken build still gets a report.
"""
import glob
import os
import sys
import xml.etree.ElementTree as ET


def junit_totals(pattern):
    files = glob.glob(pattern, recursive=True)
    if not files:
        return None
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0, "time": 0.0}
    failed = []
    for path in files:
        root = ET.parse(path).getroot()
        suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
        for suite in suites:
            for key in ("tests", "failures", "errors", "skipped"):
                totals[key] += int(float(suite.get(key, 0) or 0))
            totals["time"] += float(suite.get("time", 0) or 0)
            for case in suite.findall("testcase"):
                if case.find("failure") is not None or case.find("error") is not None:
                    failed.append(f"{case.get('classname', '')}.{case.get('name', '')}")
    return totals, failed


def pct(covered, total):
    return 100.0 * covered / total if total else 100.0


def jacoco(path):
    if not os.path.exists(path):
        return None
    root = ET.parse(path).getroot()
    result = {}
    for counter in root.findall("counter"):  # report-level counters only
        missed, covered = int(counter.get("missed")), int(counter.get("covered"))
        result[counter.get("type")] = pct(covered, missed + covered)
    return {"lines": result.get("LINE"), "branches": result.get("BRANCH")}


def cobertura(path):
    if not os.path.exists(path):
        return None
    root = ET.parse(path).getroot()
    return {"lines": 100 * float(root.get("line-rate", 0)),
            "branches": 100 * float(root.get("branch-rate", 0))}


def badge(value):
    if value is None:
        return "n/a"
    icon = "🟢" if value >= 80 else "🟡" if value >= 60 else "🔴"
    return f"{icon} {value:.1f}%"


def main(base, out):
    rows = []
    all_failed = []
    suites = [
        ("Backend unit", f"{base}/backend-reports/surefire-reports/**/TEST-*.xml"),
        ("Backend integration", f"{base}/backend-reports/failsafe-reports/**/TEST-*.xml"),
        ("Frontend", f"{base}/frontend-reports/junit/**/TEST-*.xml"),
    ]
    for name, pattern in suites:
        res = junit_totals(pattern)
        if res is None:
            if name != "Backend integration":
                rows.append(f"| {name} | ⚠️ no report | | | | |")
            continue
        t, failed = res
        all_failed += failed
        bad = t["failures"] + t["errors"]
        status = "✅" if bad == 0 else "❌"
        passed = t["tests"] - bad - t["skipped"]
        rows.append(f"| {name} | {status} | {passed} | {bad} | {t['skipped']} | {t['time']:.1f}s |")

    cov = [("Backend (JaCoCo)", jacoco(f"{base}/backend-reports/jacoco/jacoco.xml")),
           ("Frontend (Istanbul)", cobertura(f"{base}/frontend-reports/coverage/cobertura-coverage.xml"))]

    lines = ["<!-- ci-report -->", "## 🧪 CI report", "",
             "| Suite | Status | Passed | Failed | Skipped | Duration |",
             "|---|:-:|--:|--:|--:|--:|", *rows, "",
             "| Coverage | Lines | Branches |", "|---|--:|--:|"]
    for name, c in cov:
        lines.append(f"| {name} | {badge(c and c['lines'])} | {badge(c and c['branches'])} |"
                     if c else f"| {name} | ⚠️ no report | |")
    if all_failed:
        lines += ["", "<details><summary>❌ Failing tests</summary>", ""]
        lines += [f"- `{name}`" for name in all_failed[:50]]
        if len(all_failed) > 50:
            lines.append(f"- … and {len(all_failed) - 50} more")
        lines += ["", "</details>"]
    run_url = os.environ.get("RUN_URL")
    if run_url:
        lines += ["", f"Full reports (HTML coverage, raw XML) are attached to the [workflow run]({run_url})."]
    with open(out, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
