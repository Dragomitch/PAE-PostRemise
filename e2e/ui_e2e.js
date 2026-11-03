// UI walk on the real app: Angular sign-up and sign-in, then every legacy UI view as a professor
// and as a student. Expects the data created by api_e2e.py (users prof and alice, a completed
// mobility with its two payments). Prints one PASS/FAIL line per check, exits 1 on a failure.
//
// Environment:
//   APP_URL         legacy UI and API served by the jar (default http://localhost:8080)
//   NG_URL          Angular app, ng serve with /api proxied to APP_URL (default http://localhost:4200)
//   SCREENSHOT_DIR  where the screenshots go (default e2e/screenshots)
//   DATECHECK=1     also compare the payment dates of /paiements with the API
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const APP_URL = (process.env.APP_URL || 'http://localhost:8080').replace(/\/$/, '');
const NG_URL = (process.env.NG_URL || 'http://localhost:4200').replace(/\/$/, '');
const SHOTS = process.env.SCREENSHOT_DIR || path.join(__dirname, 'screenshots');
fs.mkdirSync(SHOTS, { recursive: true });

const results = [];
const ok = (cond, msg) => results.push([!!cond, msg]);

function watch(page, name, problems) {
  page.on('console', m => { if (m.type() === 'error' && !/status of 401/.test(m.text())) problems.push(`${name} console: ${m.text()}`); });
  page.on('pageerror', e => problems.push(`${name} pageerror: ${e.message}`));
  page.on('response', r => { if (r.status() >= 500) problems.push(`${name} HTTP ${r.status()} ${r.url()}`);
    else if (r.status() >= 400) results.push([true, `  info ${name}: ${r.request().method()} ${r.url()} -> ${r.status()}`]); });
}

(async () => {
  const browser = await chromium.launch();
  const problems = [];

  // --- Angular: sign up a new student, then sign in -----------------------------------------
  const ng = await (await browser.newContext()).newPage();
  watch(ng, 'angular', problems);
  await ng.goto(`${NG_URL}/signup`);
  await ng.fill('#signup-username', 'dora');
  await ng.fill('#signup-firstName', 'Dora');
  await ng.fill('#signup-lastName', 'Tester');
  await ng.fill('#signup-email', 'dora@example.test');
  await ng.selectOption('#signup-option', { index: 1 }).catch(async () => {
    await ng.selectOption('#signup-option', 'BIN');
  });
  await ng.fill('#signup-password', 'secret-dora');
  await ng.fill('#signup-reenterPassword', 'secret-dora');
  const [signup] = await Promise.all([
    ng.waitForResponse(r => r.url().endsWith('/api/1.0/users') && r.request().method() === 'POST'),
    ng.click('button[type="submit"]'),
  ]);
  ok(signup.status() === 200 || signup.status() === 201, `angular sign-up -> ${signup.status()}`);
  await ng.screenshot({ path: path.join(SHOTS, '01-angular-signup.png') });
  await ng.goto(`${NG_URL}/signin`);
  await ng.fill('#signin-username', 'dora');
  await ng.fill('#signin-password', 'secret-dora');
  const [signin] = await Promise.all([
    ng.waitForResponse(r => r.url().endsWith('/api/1.0/session') && r.request().method() === 'POST'),
    ng.click('button[type="submit"]'),
  ]);
  ok(signin.status() === 200, `angular sign-in -> ${signin.status()}`);
  await ng.waitForTimeout(500);
  await ng.screenshot({ path: path.join(SHOTS, '02-angular-signin.png') });

  // --- legacy UI: sign in, then open every view of the menu ---------------------------------
  async function legacy(user, password, views, prefix) {
    const page = await (await browser.newContext()).newPage();
    watch(page, `legacy:${user}`, problems);
    await page.goto(`${APP_URL}/connexion`);
    await page.waitForSelector('#fsi-field-username', { state: 'visible' });
    await page.fill('#fsi-field-username', user);
    await page.fill('#fsi-field-password', password);
    const [r] = await Promise.all([
      page.waitForResponse(x => x.url().endsWith('/api/1.0/session') && x.request().method() === 'POST'),
      page.click('#signin-view button[type="submit"]'),
    ]);
    ok(r.status() === 200, `legacy ${user} sign-in -> ${r.status()}`);
    await page.waitForTimeout(800);
    let i = 0;
    for (const [href, view] of views) {
      const link = page.locator(`a[href="${href}"]:visible`).first();
      if (await link.count() === 0) { ok(false, `${user}: no visible link ${href}`); continue; }
      await link.click();
      await page.waitForSelector(`#${view}`, { state: 'visible', timeout: 5000 })
        .then(() => ok(true, `${user}: ${href} shows #${view}`))
        .catch(() => ok(false, `${user}: ${href} did not show #${view}`));
      await page.waitForTimeout(700);
      const rows = await page.locator(`#${view} table tbody tr`).count();
      const empty = await page.locator(`#${view} td.dataTables_empty`).count();
      results.push([true, `  ${user} ${href}: ${rows - empty} row(s)`]);
      await page.screenshot({ path: path.join(SHOTS, `${prefix}-${String(++i).padStart(2, '0')}${href.replace(/\//g, '-')}.png`), fullPage: true });
      if (href === '/paiements' && process.env.DATECHECK) {
        // the table shows d/m/yyyy: compare it with the dates the API returns (#91 showed month - 1)
        const api = await page.evaluate(async () => (await (await fetch('/api/1.0/payments')).json()).data.map(p => p.paymentDate));
        const want = api.map(d => { const [y, m, day] = d.substring(0, 10).split('-'); return `${Number(day)}/${Number(m)}/${y}`; });
        const shown = await page.locator('#payments-view table tbody tr td:last-child').allTextContents();
        ok(api.length > 0 && shown.sort().join() === want.sort().join(), `payment dates shown ${JSON.stringify(shown)} match the API ${JSON.stringify(want)}`);
      }
      if (href === '/mes-donnees-personnelles' && process.env.DATECHECK) {
        // the personal-data form shows d/m/yyyy: compare it with the birthdate the API returns (#146 showed month - 1)
        await page.waitForFunction(() => /\//.test(document.querySelector('#student-personal-details-view [name=birthdate]')?.value || ''), null, { timeout: 5000 }).catch(() => {});
        const api = await page.evaluate(async () => {
          const me = await (await fetch('/api/1.0/session')).json();
          return (await (await fetch(`/api/1.0/nominatedStudents/${me.id}`)).json()).birthdate;
        });
        const [y, m, day] = String(api).substring(0, 10).split('-');
        const want = `${Number(day)}/${Number(m)}/${y}`;
        const shown = await page.inputValue('#student-personal-details-view [name=birthdate]');
        ok(!!api && shown === want, `birthdate field shows ${JSON.stringify(shown)}, the API birthdate ${api} is ${JSON.stringify(want)}`);
      }
    }
    return page;
  }
  await legacy('prof', 'secret-prof', [
    ['/demandes-de-mobilite', 'mobility-choices-view'],
    ['/mobilites', 'mobilities-view'],
    ['/partenaires', 'partners-view'],
    ['/etudiants', 'students-view'],
    ['/paiements', 'payments-view'],
  ], '10-prof');
  await legacy('alice', 'secret-alice', [
    ['/demandes-de-mobilite', 'mobility-choices-view'],
    ['/mobilites', 'mobilities-view'],
    ['/partenaires', 'partners-view'],
    ['/mes-donnees-personnelles', 'student-personal-details-view'],
  ], '20-alice');

  await browser.close();
  for (const p of problems) results.push([false, p]);
  for (const [pass, msg] of results) console.log((pass ? 'PASS ' : 'FAIL ') + msg);
  const failed = results.filter(r => !r[0]).length;
  console.log(`${results.length - failed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch(e => { console.error(e); process.exit(2); });
