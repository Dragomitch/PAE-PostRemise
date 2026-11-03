#!/usr/bin/env python3
"""Unit tests of ci_report.py, run by the "Workflow lint" job of ci.yml.

    python3 -m unittest discover -s .github/scripts -p 'test_*.py'

The fixtures are written to a temporary directory in the layout the "Build CI report" step
builds (backend-reports/surefire-reports, backend-reports/failsafe-reports, frontend-reports/junit),
so the script is exercised through main() like in CI.
"""
import os
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import ci_report

OUTCOMES = {
    "pass": "",
    "failure": '<failure message="expected 1 but was 2" type="AssertionError">trace</failure>',
    "error": '<error message="boom" type="IllegalStateException">trace</error>',
    "skipped": "<skipped/>",
}


def testcase(classname, name, outcome="pass"):
    attributes = f'name="{name}" classname="{classname}" time="0.1"'
    child = OUTCOMES[outcome]
    return f"<testcase {attributes}>{child}</testcase>" if child else f"<testcase {attributes}/>"


def testsuite(name, cases, tests=None, failures=0, errors=0, skipped=0, time="1.0"):
    """A Surefire-like suite. The attributes are given separately from the cases on purpose."""
    tests = len(cases) if tests is None else tests
    body = "\n  ".join(testcase(*case) for case in cases)
    return (f'<testsuite name="{name}" time="{time}" tests="{tests}" errors="{errors}" '
            f'skipped="{skipped}" failures="{failures}">\n  {body}\n</testsuite>\n')


def document(*suites, wrapper=None):
    """An XML file holding the suites, optionally inside a wrapper element such as testsuites."""
    body = "".join(suites)
    if wrapper:
        body = f"<{wrapper}>\n{body}</{wrapper}>\n"
    return f'<?xml version="1.0" encoding="UTF-8"?>\n{body}'


class CiReportTest(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.base = Path(tmp.name) / "norm"
        self.base.mkdir()
        self.out = Path(tmp.name) / "ci-report.md"
        env = mock.patch.dict(os.environ)
        env.start()
        self.addCleanup(env.stop)
        os.environ.pop("RUN_URL", None)

    def write(self, relative_path, content):
        path = self.base / relative_path
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")

    def render(self):
        ci_report.main(str(self.base), str(self.out))
        return self.out.read_text(encoding="utf-8")

    @staticmethod
    def row(markdown, suite):
        """The cells of the table row of a suite: [name, status, passed, failed, skipped, duration]."""
        for line in markdown.splitlines():
            cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
            if line.startswith("|") and cells[0] == suite:
                return cells
        raise AssertionError(f"no row for {suite!r} in:\n{markdown}")

    def test_plain_suite(self):
        self.write("backend-reports/surefire-reports/TEST-com.acme.PlainTest.xml",
                   document(testsuite("com.acme.PlainTest",
                                      [("com.acme.PlainTest", "a"), ("com.acme.PlainTest", "b"),
                                       ("com.acme.PlainTest", "c")], time="0.5")))

        markdown = self.render()

        self.assertEqual(["Backend unit", "✅", "3", "0", "0", "0.5s"], self.row(markdown, "Backend unit"))
        self.assertNotIn("Failing tests", markdown)

    def test_nested_cases_are_counted_although_the_tests_attribute_leaves_them_out(self):
        outer = "com.acme.FooServiceTest"
        cases = [(outer, "outerOne"), (outer, "outerTwo"),
                 (outer + "$Create", "createsIt"), (outer + "$Create", "createsAnother"),
                 (outer + "$Edit", "rejectsAStaleVersion", "failure"),
                 (outer + "$Edit", "notYetImplemented", "skipped")]
        # Surefire 3.5: the attributes only cover the 2 cases of the outer class
        suite = testsuite(outer, cases, tests=2)
        self.assertEqual(6, suite.count("<testcase "))
        self.assertIn('tests="2"', suite)
        self.write(f"backend-reports/surefire-reports/TEST-{outer}.xml", document(suite))

        markdown = self.render()

        self.assertEqual(["Backend unit", "❌", "4", "1", "1", "1.0s"], self.row(markdown, "Backend unit"))
        self.assertIn("<details><summary>❌ Failing tests</summary>", markdown)
        self.assertIn(f"- `{outer}$Edit.rejectsAStaleVersion`", markdown)
        self.assertNotIn("notYetImplemented", markdown)

    def test_an_error_counts_as_a_failure_and_is_listed(self):
        self.write("backend-reports/surefire-reports/TEST-com.acme.ErrorTest.xml",
                   document(testsuite("com.acme.ErrorTest",
                                      [("com.acme.ErrorTest", "fine"),
                                       ("com.acme.ErrorTest", "blowsUp", "error")],
                                      errors=1)))

        markdown = self.render()

        self.assertEqual(["Backend unit", "❌", "1", "1", "0", "1.0s"], self.row(markdown, "Backend unit"))
        self.assertIn("- `com.acme.ErrorTest.blowsUp`", markdown)

    def test_totals_add_up_over_files_suites_and_reports(self):
        self.write("backend-reports/surefire-reports/TEST-com.acme.OneTest.xml",
                   document(testsuite("com.acme.OneTest", [("com.acme.OneTest", "a")] * 2, time="1.5")))
        self.write("backend-reports/surefire-reports/TEST-com.acme.TwoTest.xml",
                   document(testsuite("com.acme.TwoTest", [("com.acme.TwoTest", "b")] * 3, time="2.0")))
        self.write("backend-reports/failsafe-reports/TEST-com.acme.ThingIT.xml",
                   document(testsuite("com.acme.ThingIT", [("com.acme.ThingIT", "c")], time="3.0")))
        # the frontend reporter wraps its suites in a testsuites element
        self.write("frontend-reports/junit/TEST-chrome.xml",
                   document(testsuite("chrome", [("app", "x"), ("app", "y", "skipped")], skipped=1, time="4.0"),
                            testsuite("firefox", [("app", "z")], time="1.0"),
                            wrapper="testsuites"))

        markdown = self.render()

        self.assertEqual(["Backend unit", "✅", "5", "0", "0", "3.5s"], self.row(markdown, "Backend unit"))
        self.assertEqual(["Backend integration", "✅", "1", "0", "0", "3.0s"],
                         self.row(markdown, "Backend integration"))
        self.assertEqual(["Frontend", "✅", "2", "0", "1", "5.0s"], self.row(markdown, "Frontend"))

    def test_missing_reports_keep_the_layout(self):
        markdown = self.render()

        self.assertEqual(["Backend unit", "⚠️ no report", "", "", "", ""], self.row(markdown, "Backend unit"))
        self.assertEqual(["Frontend", "⚠️ no report", "", "", "", ""], self.row(markdown, "Frontend"))
        self.assertNotIn("Backend integration", markdown)
        self.assertTrue(markdown.startswith("<!-- ci-report -->\n## 🧪 CI report\n"))
        self.assertIn("| Suite | Status | Passed | Failed | Skipped | Duration |", markdown)
        self.assertIn("| Coverage | Lines | Branches |", markdown)


if __name__ == "__main__":
    unittest.main()
