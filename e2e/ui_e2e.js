// UI walk on the real app: Angular sign-up and sign-in, then every legacy UI view as a professor
// and as a student, then the Angular mobility choices screen as the student alice (her list, a
// creation through the form) and as prof (active list and count, rejection, filter "all"), then
// alice's personal data (a phone number changed through the form), prof's users table and the
// mobilities (prof's list and the completed mobility's detail, alice's list).
// Expects the data created by api_e2e.py (users prof and alice, a completed mobility with its two
// payments, alice's personal data). Prints one PASS/FAIL line per check, exits 1 on a failure.
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

  // --- Angular: the mobility choices screen as alice (student), then as prof (professor) ------
  // Runs after the legacy walk so that the views above see the data of api_e2e.py unchanged. Alice
  // creates a choice for September of this year through the form, prof finds it among the active
  // choices and rejects it with a denial reason; nothing else (mobilities, payments) changes.
  const YEAR = new Date().getFullYear();
  const choiceRows = page => page.locator('app-mobility-choices tbody tr');
  const listed = page => page.waitForSelector('app-mobility-choices table, app-mobility-choices p.state:not([role])');
  const apiCount = (page, filter) => page.evaluate(async f =>
    (await (await fetch(`/api/1.0/mobilityChoices/count${f ? `?filter=${f}` : ''}`)).json()).count, filter);
  const shownCount = async page => Number((await page.textContent('app-mobility-choices .count')).replace(/\D+/g, ''));
  async function mobilityChoices(user) {
    const page = await (await browser.newContext()).newPage();
    watch(page, `angular:${user}`, problems);
    await page.goto(`${NG_URL}/signin`);
    await page.fill('#signin-username', user);
    await page.fill('#signin-password', `secret-${user}`);
    await Promise.all([page.waitForURL(/\/partners$/), page.click('button[type="submit"]')]);
    await page.click('nav a[href="/mobility-choices"]');
    await listed(page);
    return page;
  }

  const alice = await mobilityChoices('alice');
  const before = await choiceRows(alice).count();
  const own = await apiCount(alice);
  ok(before === own && before > 0 && /Mes demandes de mobilité/.test(await alice.textContent('h1')),
    `angular alice: /mobility-choices lists her ${own} choice(s) (${before} row(s))`);
  await alice.screenshot({ path: path.join(SHOTS, '30-angular-alice-mobility-choices.png'), fullPage: true });
  await alice.click('button.create');
  await alice.waitForSelector('#mobility-choice-programme option[value="1"]', { state: 'attached' });
  await alice.selectOption('#mobility-choice-programme', '1');
  await alice.selectOption('#mobility-choice-country', 'FR');
  await alice.selectOption('#mobility-choice-partner', { label: 'Universite de Lyon' });
  await alice.selectOption('#mobility-choice-order', '2');
  await alice.selectOption('#mobility-choice-departure', `1 ${YEAR}`);
  await alice.screenshot({ path: path.join(SHOTS, '31-angular-alice-mobility-choice-form.png'), fullPage: true });
  const [created] = await Promise.all([
    alice.waitForResponse(r => r.url().endsWith('/api/1.0/mobilityChoice') && r.request().method() === 'POST'),
    alice.click('app-mobility-choice-form button[type="submit"]'),
  ]);
  ok(created.status() === 200 || created.status() === 201, `angular alice: choice created through the form -> ${created.status()}`);
  await alice.waitForFunction(n => document.querySelectorAll('app-mobility-choices tbody tr').length === n, before + 1, { timeout: 5000 }).catch(() => {});
  const after = await choiceRows(alice).count();
  ok(after === before + 1, `angular alice: the new choice is listed (${before} -> ${after} row(s))`);
  await alice.screenshot({ path: path.join(SHOTS, '32-angular-alice-mobility-choice-created.png'), fullPage: true });

  const prof = await mobilityChoices('prof');
  const active = await choiceRows(prof).count();
  const activeApi = await apiCount(prof, 'active');
  ok(active === activeApi && active === await shownCount(prof) && active > 0,
    `angular prof: /mobility-choices lists the ${activeApi} active choice(s), count shown ${await shownCount(prof)}`);
  await prof.screenshot({ path: path.join(SHOTS, '40-angular-prof-mobility-choices.png'), fullPage: true });
  // alice's new choice is the last one (the API orders by id)
  await choiceRows(prof).last().locator('button.reject').click();
  await prof.waitForSelector('#mobility-choices-reject-reason option:not([value=""])', { state: 'attached' });
  await prof.selectOption('#mobility-choices-reject-reason', { index: 1 });
  await prof.screenshot({ path: path.join(SHOTS, '41-angular-prof-mobility-choice-reject.png'), fullPage: true });
  const [rejected] = await Promise.all([
    prof.waitForResponse(r => /\/api\/1\.0\/mobilityChoices\/\d+\/reject\?reason=\d+$/.test(r.url()) && r.request().method() === 'PUT'),
    prof.click('.action-panel button[type="submit"]'),
  ]);
  ok(rejected.status() === 200, `angular prof: choice rejected with a denial reason -> ${rejected.status()}`);
  await prof.waitForFunction(n => document.querySelectorAll('app-mobility-choices tbody tr').length === n, active - 1, { timeout: 5000 }).catch(() => {});
  await listed(prof);
  const left = await choiceRows(prof).count();
  ok(left === active - 1 && left === await apiCount(prof, 'active'), `angular prof: the rejected choice left the active list (${active} -> ${left} row(s))`);
  await Promise.all([
    prof.waitForResponse(r => r.url().includes('/api/1.0/mobilityChoices/count?filter=all')),
    prof.selectOption('#mobility-choices-filter', 'all'),
  ]);
  await listed(prof);
  await prof.waitForTimeout(300);
  const all = await choiceRows(prof).count();
  ok(all > 0 && all === await shownCount(prof) && all === await apiCount(prof, 'all'), `angular prof: filter "all" lists ${all} choice(s), count shown ${await shownCount(prof)}`);
  await prof.screenshot({ path: path.join(SHOTS, '42-angular-prof-mobility-choices-all.png'), fullPage: true });

  // --- Angular: alice's personal data, then prof's users table -----------------------------
  // alice changes her phone number through the form and the API must return it; prof's table must
  // list the accounts of GET /users with their role. The promotion is opened and cancelled only:
  // the roles stay as api_e2e.py left them.
  const apiGet = (page, url) => page.evaluate(async u => (await fetch(u)).json(), url);
  const myRecord = async page => apiGet(page, `/api/1.0/nominatedStudents/${(await apiGet(page, '/api/1.0/session')).id}`);
  await alice.click('nav a[href="/personal-data"]');
  await alice.waitForFunction(() => document.querySelector('#personal-data-phoneNumber')?.value, null, { timeout: 5000 }).catch(() => {});
  const record = await myRecord(alice);
  const shownRecord = await alice.evaluate(() => Object.fromEntries(
    ['lastName', 'birthdate', 'nationality', 'phoneNumber', 'iban', 'nbrPassedYears'].map(f => [f, document.querySelector(`#personal-data-${f}`)?.value])));
  ok(shownRecord.lastName === record.lastName && shownRecord.birthdate === record.birthdate && shownRecord.nationality === record.nationality.countryCode
    && shownRecord.phoneNumber === record.phoneNumber && shownRecord.iban === record.iban && Number(shownRecord.nbrPassedYears) === record.nbrPassedYears,
    `angular alice: /personal-data shows her record ${JSON.stringify(shownRecord)}`);
  const phone = '+32470000099';
  await alice.fill('#personal-data-phoneNumber', phone);
  const [saved] = await Promise.all([
    alice.waitForResponse(r => /\/api\/1\.0\/nominatedStudents\/\d+$/.test(r.url()) && r.request().method() === 'PUT'),
    alice.click('app-personal-data button[type="submit"]'),
  ]);
  ok(saved.status() === 200, `angular alice: personal data saved through the form -> PUT ${saved.status()}`);
  await alice.waitForSelector('app-personal-data .notification-success', { timeout: 5000 }).catch(() => {});
  const updated = await myRecord(alice);
  ok(updated.phoneNumber === phone && updated.version === record.version + 1,
    `angular alice: GET /nominatedStudents/{id} returns the new phone number ${updated.phoneNumber} (version ${record.version} -> ${updated.version})`);
  await alice.screenshot({ path: path.join(SHOTS, '50-angular-alice-personal-data.png'), fullPage: true });

  await prof.click('nav a[href="/users"]');
  await prof.waitForSelector('app-users table');
  const roleNames = { Professor: 'Professeur', Student: 'Étudiant' };
  const apiUsers = (await apiGet(prof, '/api/1.0/users')).data.map(u => `${u.username}:${roleNames[u.role]}`).sort();
  const shownUsers = (await prof.$$eval('app-users tbody tr', trs => trs.map(tr => `${tr.dataset.username}:${tr.querySelector('.role').textContent.trim()}`))).sort();
  ok(apiUsers.length > 0 && shownUsers.join() === apiUsers.join(), `angular prof: /users lists the ${apiUsers.length} user(s) of GET /users with their role ${JSON.stringify(shownUsers)}`);
  const students = apiUsers.filter(u => u.endsWith(':Étudiant')).map(u => u.split(':')[0]);
  const promotable = (await prof.$$eval('app-users tbody tr', trs => trs.filter(tr => tr.querySelector('button.promote')).map(tr => tr.dataset.username))).sort();
  ok(students.length > 0 && promotable.join() === students.join(), `angular prof: "Promouvoir" is offered to the students only ${JSON.stringify(promotable)}`);
  await prof.screenshot({ path: path.join(SHOTS, '60-angular-prof-users.png'), fullPage: true });
  const promotions = [];
  prof.on('request', r => { if (/\/promote$/.test(r.url())) promotions.push(r.url()); });
  await prof.click('app-users tbody tr button.promote');
  await prof.waitForSelector('app-users .promote-panel');
  await prof.screenshot({ path: path.join(SHOTS, '61-angular-prof-users-promote.png'), fullPage: true });
  await prof.click('app-users .promote-panel button.cancel');
  await prof.waitForTimeout(300);
  ok(promotions.length === 0 && await prof.locator('app-users .promote-panel').count() === 0,
    'angular prof: "Promouvoir" asks for a confirmation, cancelled without any request');

  // --- Angular: the mobilities, prof's list and the completed mobility's detail, alice's list ---
  // Read only: the API script already drives the whole lifecycle. The list must hold the mobilities
  // of GET /mobilities with their state, the detail of the completed one (the mobility that
  // mobility_detail.js checks on the legacy UI) its state and the days of its two payments.
  const mobilityStates = page => page.$$eval('app-mobilities tbody tr', trs => trs.map(tr => `${tr.dataset.id}:${tr.dataset.state}`).sort());
  await prof.click('nav a[href="/mobilities"]');
  await prof.waitForSelector('app-mobilities table');
  const profMobilities = (await apiGet(prof, '/api/1.0/mobilities')).data;
  const profStates = await mobilityStates(prof);
  ok(profMobilities.length > 0 && profStates.join() === profMobilities.map(m => `${m.id}:${m.state}`).sort().join(),
    `angular prof: /mobilities lists the ${profMobilities.length} mobilities of GET /mobilities with their state ${JSON.stringify(profStates)}`);
  await prof.screenshot({ path: path.join(SHOTS, '70-angular-prof-mobilities.png'), fullPage: true });
  const done = profMobilities.find(m => m.state === 'Terminée');
  await prof.locator('app-mobilities tbody tr[data-state="Terminée"] th a').first().click();
  await prof.waitForSelector('app-mobility-detail .second-payment time', { timeout: 5000 }).catch(() => {});
  const shownDetail = await prof.evaluate(() => ({
    url: location.pathname,
    state: document.querySelector('app-mobility-detail .mobility-state')?.dataset.state,
    first: document.querySelector('app-mobility-detail .first-payment time')?.getAttribute('datetime'),
    second: document.querySelector('app-mobility-detail .second-payment time')?.getAttribute('datetime'),
    buttons: document.querySelectorAll('app-mobility-detail button').length,
  }));
  ok(done && shownDetail.url === `/mobilities/${done.id}` && shownDetail.state === done.state && shownDetail.buttons === 0,
    `angular prof: the detail of the completed mobility ${done && done.id} shows its state ${JSON.stringify(shownDetail.state)}, no button`);
  ok(done && shownDetail.first === String(done.firstPaymentRequestDate).substring(0, 10) && shownDetail.second === String(done.secondPaymentRequestDate).substring(0, 10),
    `angular prof: payment days shown ${JSON.stringify([shownDetail.first, shownDetail.second])} match the API ${JSON.stringify(done && [done.firstPaymentRequestDate, done.secondPaymentRequestDate])}`);
  await prof.screenshot({ path: path.join(SHOTS, '71-angular-prof-mobility-detail.png'), fullPage: true });

  await alice.click('nav a[href="/mobilities"]');
  await alice.waitForSelector('app-mobilities table');
  const aliceMobilities = (await apiGet(alice, '/api/1.0/mobilities')).data;
  const aliceStates = await mobilityStates(alice);
  ok(aliceMobilities.length > 0 && aliceStates.join() === aliceMobilities.map(m => `${m.id}:${m.state}`).sort().join()
    && /Mes mobilités/.test(await alice.textContent('app-mobilities h1')),
    `angular alice: /mobilities lists her ${aliceMobilities.length} mobilities ${JSON.stringify(aliceStates)}`);
  await alice.screenshot({ path: path.join(SHOTS, '72-angular-alice-mobilities.png'), fullPage: true });

  await browser.close();
  for (const p of problems) results.push([false, p]);
  for (const [pass, msg] of results) console.log((pass ? 'PASS ' : 'FAIL ') + msg);
  const failed = results.filter(r => !r[0]).length;
  console.log(`${results.length - failed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch(e => { console.error(e); process.exit(2); });
