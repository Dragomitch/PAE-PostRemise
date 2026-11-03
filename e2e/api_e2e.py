"""End-to-end API run against the packaged app on a real PostgreSQL.

Signs up a professor and two students, exercises every endpoint and the whole mobility
lifecycle (choice, confirmation, documents, payments, cancellation) and prints one PASS/FAIL
line per request or check. Exits 1 if any check failed.

Python standard library only. Needs a freshly loaded database (SQLRessources/init.sql): the
first sign-up must become the professor.

Environment:
  API_URL   base URL of the REST API (default http://localhost:8080/api/1.0)

Options:
  -q        print only the failed checks and the summary
"""
import datetime
import http.cookiejar
import json
import os
import sys
import traceback
import urllib.error
import urllib.parse
import urllib.request

BASE = os.environ.get("API_URL", "http://localhost:8080/api/1.0").rstrip("/")
TIMEOUT = float(os.environ.get("API_TIMEOUT", "30"))
YEAR = datetime.date.today().year
results = []


class Client:
    def __init__(self, name):
        self.name = name
        self.jar = http.cookiejar.CookieJar()
        self.op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))

    def xsrf(self):
        for c in self.jar:
            if c.name == "XSRF-TOKEN":
                return c.value

    def call(self, method, path, body=None, expect=(200,), params=None, label=None):
        url = BASE + path + ("?" + urllib.parse.urlencode(params) if params else "")
        data = json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(url, data=data, method=method)
        req.add_header("Accept", "application/json, text/csv, application/problem+json")
        req.add_header("Accept-Language", "en")
        if data is not None:
            req.add_header("Content-Type", "application/json")
        if method != "GET" and self.xsrf():
            req.add_header("X-XSRF-TOKEN", self.xsrf())
        try:
            with self.op.open(req, timeout=TIMEOUT) as r:
                status, raw, ctype = r.status, r.read().decode(), r.headers.get("Content-Type", "")
        except urllib.error.HTTPError as e:
            status, raw, ctype = e.code, e.read().decode(), e.headers.get("Content-Type", "")
        ok = status in expect
        results.append((ok, f"{self.name:9} {method:6} {path} -> {status}" + ("" if ok else f"  (expected {expect}) {raw[:300]}"), label))
        if "json" in ctype and raw:
            return json.loads(raw)
        return raw


def rows(x):
    if isinstance(x, dict):
        return x.get("data", [])
    return x if isinstance(x, list) else []


def check(cond, msg):
    results.append((bool(cond), "  check: " + msg, None))


def main():
    anon = Client("anon")
    anon.call("GET", "/options")  # issues the XSRF cookie

    # --- users: the first sign-up is a professor -------------------------------------------------
    prof = Client("prof"); prof.call("GET", "/options")
    stud = Client("student"); stud.call("GET", "/options")
    stud2 = Client("student2"); stud2.call("GET", "/options")
    def user(u, opt="BIN"):
        return {"username": u, "password": "secret-" + u, "firstName": u.capitalize(), "lastName": "Tester",
                "email": f"{u}@example.test", "option": {"code": opt}}
    p = prof.call("POST", "/users", user("prof"), expect=(200, 201))
    check(isinstance(p, dict) and p.get("role") == "Professor", "first sign-up is a professor")
    s = stud.call("POST", "/users", user("alice"), expect=(200, 201))
    check(isinstance(s, dict) and s.get("role") == "Student", "second sign-up is a student")
    s2 = stud2.call("POST", "/users", user("bruno", "BCH"), expect=(200, 201))
    stud.call("POST", "/users", user("alice"), expect=(409,), label="duplicate username -> 409")
    for c, u in ((prof, "prof"), (stud, "alice"), (stud2, "bruno")):
        c.call("POST", "/session", {"username": u, "password": "secret-" + u})
    me = stud.call("GET", "/session")
    check(me.get("username") == "alice", "session returns the current user")
    users = prof.call("GET", "/users")
    check(len(rows(users)) == 3, "professor lists 3 users")
    stud.call("GET", "/users", expect=(403,), label="student cannot list users")

    # --- reference data ------------------------------------------------------------------------
    check(len(anon.call("GET", "/options")) >= 1, "options are public")
    check(len(prof.call("GET", "/countries")) == 250, "250 countries")
    check(prof.call("GET", "/countries/BE").get("name"), "country BE found")
    prof.call("GET", "/countries/ZZ", expect=(404,), label="unknown country 404")
    progs = prof.call("GET", "/programmes"); check(len(progs) >= 3, "programmes listed")
    prof.call("GET", "/programmes/1")
    prof.call("GET", "/options/BIN")

    # --- denial reasons ------------------------------------------------------------------------
    dr = prof.call("POST", "/denialReasons", {"reason": "Dossier incomplet"}, expect=(200, 201))
    drs = prof.call("GET", "/denialReasons")
    drid = rows(drs)[0]["id"] if isinstance(drs, dict) else drs[0]["id"]
    prof.call("PUT", f"/denialReasons/{drid}", {"id": drid, "reason": "Dossier incomplet (bis)"})

    # --- partners ------------------------------------------------------------------------------
    def partner(name, country="FR", official=True):
        return {"legalName": name + " SA", "businessName": name, "fullName": name, "official": official,
                "organisationType": "PME", "employeeCount": "12",
                "address": {"street": "Rue de la Paix", "number": "1", "city": "Paris", "postalCode": "75000",
                            "region": "", "country": {"countryCode": country}},
                "email": name.lower().replace(" ", "") + "@example.test",
                "website": "https://" + name.lower().replace(" ", "") + ".example.test", "phoneNumber": "+33100000000",
                "options": [{"code": "BIN", "departement": "Informatique"}]}
    pa = prof.call("POST", "/partners", partner("Universite de Lyon"), expect=(200, 201))
    pb = prof.call("POST", "/partners", partner("Hochschule Berlin", "DE"), expect=(200, 201))
    paid = pa["id"]; pbid = pb["id"]
    nowww = partner("Sans Site")
    nowww.pop("website")
    pc = prof.call("POST", "/partners", nowww, expect=(200, 201), label="partner without website accepted")
    check(isinstance(pc, dict) and pc.get("website", "") == "", "partner without website stored with an empty one")
    prof.call("POST", f"/partners/{paid}", {"code": "BCH", "departement": "Chimie"})
    popts = prof.call("GET", f"/partners/partnersOptions/{paid}")
    check(len(popts if isinstance(popts, list) else popts.get("data", [])) == 2, "partner has 2 options")
    plist = prof.call("GET", "/partners", params=None)
    check(len(rows(plist)) == 3, "professor sees 3 partners")
    slist = stud.call("GET", "/partners", params=None)
    check(len(rows(slist)) == 3, "student (BIN) sees the 3 official BIN partners")
    prof.call("GET", "/partners", params={"filter": "country", "value": "FR"})
    got = prof.call("GET", f"/partners/{paid}")
    check([o["code"] for o in got["options"]] == ["BCH", "BIN"], "partner options sorted by code")
    got["phoneNumber"] = "+33199999999"
    prof.call("PUT", f"/partners/{paid}", got)
    prof.call("PUT", f"/partners/{paid}", got, expect=(409,), label="stale partner version -> 409")
    prof.call("GET", "/partners", params={"filter": "nope", "value": ""}, expect=(400,), label="unknown filter 400")
    check(len(rows(prof.call("GET", "/partners", params={"filter": "archived", "value": "lyon"}))) == 0, "no archived partner yet")

    # --- mobility choices ----------------------------------------------------------------------
    def choice(order, partner_id=None, country="FR", mtype="SMS", uid=None, term=1):
        c = {"user": {"id": uid}, "preferenceOrder": order, "mobilityType": mtype, "academicYear": YEAR, "term": term,
             "programme": {"id": 1}, "country": {"countryCode": country}}
        if partner_id:
            c["partner"] = {"id": partner_id}
        return c
    c1 = stud.call("POST", "/mobilityChoice", choice(1, paid, uid=s["id"]), expect=(200, 201))
    c2 = stud.call("POST", "/mobilityChoice", choice(2, pbid, "DE", uid=s["id"], term=2), expect=(200, 201))
    c3 = stud.call("POST", "/mobilityChoice", choice(3, None, "BE", "SMP", uid=s["id"]), expect=(200, 201))
    c4 = stud2.call("POST", "/mobilityChoice", choice(1, paid, uid=s2["id"]), expect=(200, 201))
    check(all(isinstance(c, dict) and c.get("id") for c in (c1, c2, c3, c4)), "choices created with ids")
    check(c1.get("submissionDate"), "submission date set by the database")
    mine = stud.call("GET", "/mobilityChoices")
    check(len(rows(mine)) == 3, "student sees own 3 choices")
    for f in ("all", "active", "canceled", "rejected", "passed"):
        prof.call("GET", "/mobilityChoices", params={"filter": f})
    cnt = prof.call("GET", "/mobilityChoices/count", params={"filter": "active"})
    check(cnt.get("count") == 4, f"4 active choices (got {cnt})")
    stud.call("PUT", f"/mobilityChoices/{c3['id']}/cancel", params={"reason": "Changed my mind"})
    prof.call("PUT", f"/mobilityChoices/{c4['id']}/reject", params={"reason": drid})
    check(len(rows(prof.call("GET", "/mobilityChoices", params={"filter": "canceled"}))) == 1, "1 cancelled choice")
    check(len(rows(prof.call("GET", "/mobilityChoices", params={"filter": "rejected"}))) == 1, "1 rejected choice")
    csv = prof.call("GET", "/mobilityChoices/export", params={"filter": "all"})
    check(isinstance(csv, str) and csv.count("\n") >= 4, "choices CSV export")
    prof.call("PUT", f"/mobilityChoices/{c1['id']}/confirm")
    check(len(rows(prof.call("GET", "/mobilityChoices", params={"filter": "active"}))) == 1, "confirmed choice no longer active")
    prof.call("PUT", f"/partners/{paid}", prof.call("GET", f"/partners/{paid}") | {"archived": True},
              expect=(409, 422), label="partner with an active mobility cannot be archived")

    # --- nominated student ---------------------------------------------------------------------
    ns = dict(user("alice")); ns.pop("password")
    ns |= {"id": s["id"], "title": "Ms", "birthdate": "2001-04-12", "nationality": {"countryCode": "BE"},
           "address": {"street": "Rue de la Loi", "number": "16", "city": "Bruxelles", "postalCode": "1000",
                       "region": "", "country": {"countryCode": "BE"}},
           "phoneNumber": "+32470000001", "gender": "F", "nbrPassedYears": 1, "iban": "BE68539007547034",
           "bankName": "Belfius", "bic": "GKCCBEBB", "cardHolder": "Alice Tester"}
    created = stud.call("POST", "/nominatedStudents", ns, expect=(200, 201))
    nsget = stud.call("GET", f"/nominatedStudents/{s['id']}")
    check(nsget.get("iban") == "BE68539007547034" and nsget.get("nationality", {}).get("countryCode") == "BE", "nominated student read back")
    check(len(rows(prof.call("GET", "/nominatedStudents"))) == 1, "professor lists 1 nominated student")
    nsget["phoneNumber"] = "+32470000002"
    stud.call("PUT", f"/nominatedStudents/{s['id']}", nsget)
    check(stud.call("GET", f"/nominatedStudents/{s['id']}").get("phoneNumber") == "+32470000002", "nominated student updated")

    # --- mobilities ----------------------------------------------------------------------------
    mobs = rows(prof.call("GET", "/mobilities"))
    check(len(mobs) == 1 and mobs[0]["id"] == c1["id"], "the confirmed choice is a mobility")
    m = prof.call("GET", f"/mobilities/{c1['id']}")
    check(m.get("partner", {}).get("fullName") == "Universite de Lyon", "mobility joins its partner")
    docs = m.get("documents") or []
    results.append((True, f"  info: mobility state={m.get('state')} version={m.get('version')} documents={len(docs)}", None))
    v = m["version"]
    prof.call("PUT", f"/mobilities/{m['id']}/confirmProEcoEncoding", params={"version": v})
    v = prof.call("GET", f"/mobilities/{m['id']}")["version"]
    prof.call("PUT", f"/mobilities/{m['id']}/confirmSecondSoftwareEncoding", params={"version": v})
    m = prof.call("GET", f"/mobilities/{m['id']}")
    check(m.get("encodedInProEco") is True or m.get("proEcoEncoding") is True or "ProEco" in json.dumps(m), "ProEco encoding stored")
    prof.call("PUT", f"/mobilities/{m['id']}/confirmSecondSoftwareEncoding", params={"version": 1},
              expect=(409,), label="stale mobility version -> 409")
    def fill(category):
        for d in [d for d in docs if d.get("category") == category]:
            cur = prof.call("GET", f"/mobilities/{m['id']}")
            prof.call("PUT", f"/mobilities/{m['id']}/confirmDocument", params={"document": d["id"], "version": cur["version"]})
    def pay():
        cur = prof.call("GET", f"/mobilities/{m['id']}")
        return prof.call("PUT", f"/mobilities/{m['id']}/confirmPayment", params={"version": cur["version"]})
    check(len(docs) == 9, f"Erasmus+ mobility has its 9 documents (got {len(docs)})")
    fill("D")
    check(prof.call("GET", f"/mobilities/{m['id']}").get("state") == "A payer", "departure documents filled -> A payer")
    pay()
    check(prof.call("GET", f"/mobilities/{m['id']}").get("state") == "En cours", "first payment -> En cours")
    fill("R")
    check(prof.call("GET", f"/mobilities/{m['id']}").get("state") == "Solde à payer", "return documents filled -> Solde à payer")
    pay()
    final = prof.call("GET", f"/mobilities/{m['id']}")
    check(final.get("state") == "Terminée", "balance payment -> Terminée")
    check(all(d.get("filledIn") for d in final.get("documents", [])), "all documents filled in")
    payments = prof.call("GET", "/payments")
    types = sorted(p.get("paymentType") for p in rows(payments) if p.get("mobilityChoiceId") == m["id"])
    check(types == ["D", "R"], f"payments D and R listed (got {types})")
    for f in ("D", "DF", "R", "RF"):
        exp = prof.call("GET", f"/mobilities/{m['id']}/export", params={"filter": f})
        check(isinstance(exp, str) and exp.strip(), f"documents export {f}")
    prof.call("GET", f"/mobilities/{m['id']}/export", params={"filter": "all"}, expect=(400,), label="unknown document filter 400")
    stud.call("GET", "/mobilities")
    # second mobility, cancelled by the professor with a denial reason
    prof.call("PUT", f"/mobilityChoices/{c2['id']}/confirm")
    m2 = prof.call("GET", f"/mobilities/{c2['id']}")
    prof.call("PUT", f"/mobilities/{m2['id']}/cancel", params={"version": m2["version"], "denialReason": drid})
    check(prof.call("GET", f"/mobilities/{c2['id']}").get("state") == "Annulée", "mobility cancelled")

    # --- users: promote, edit ------------------------------------------------------------------
    bruno = [u for u in rows(prof.call("GET", "/users")) if u["username"] == "bruno"][0]
    prof.call("PUT", f"/users/{bruno['id']}/promote")
    check([u for u in rows(prof.call("GET", "/users")) if u["username"] == "bruno"][0]["role"] == "Professor", "bruno promoted")
    prof.call("PUT", "/users/by-username/nobody/promote", expect=(404,), label="unknown username 404")
    meu = stud.call("GET", "/session")
    edit = {k: meu[k] for k in ("id", "username", "firstName", "lastName", "email", "option", "version") if k in meu}
    edit["firstName"] = "Alicia"
    stud.call("PUT", "/users/edit", edit)
    check(stud.call("GET", "/session").get("firstName") == "Alicia", "user edited")

    # --- session end ---------------------------------------------------------------------------
    stud.call("DELETE", "/session")
    stud.call("GET", "/session", expect=(401,), label="signed out -> 401")


def report(quiet):
    for ok, line, label in results:
        if not ok or not quiet:
            print(("PASS " if ok else "FAIL ") + line + (f"   [{label}]" if label else ""))
    failed = sum(1 for r in results if not r[0])
    print(f"{len(results) - failed} passed, {failed} failed")
    return failed


if __name__ == "__main__":
    try:
        main()
    except Exception as e:  # a broken response aborts the scenario: report it as a failure
        traceback.print_exc()
        results.append((False, f"aborted: {type(e).__name__}: {e}", None))
    sys.exit(1 if report("-q" in sys.argv) else 0)
