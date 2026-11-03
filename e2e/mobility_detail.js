// Opens the completed mobility's detail in the legacy UI as the professor and checks that both
// payment statuses read "Effectué le d/m/yyyy" with the dates the API returns. Expects the data
// created by api_e2e.py. Prints one PASS/FAIL line per check, exits 1 on a failure.
//
// Environment:
//   APP_URL         legacy UI and API served by the jar (default http://localhost:8080)
//   SCREENSHOT_DIR  where the screenshot goes (default e2e/screenshots)
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const APP_URL = (process.env.APP_URL || 'http://localhost:8080').replace(/\/$/, '');
const SHOTS = process.env.SCREENSHOT_DIR || path.join(__dirname, 'screenshots');
fs.mkdirSync(SHOTS, { recursive: true });

const results = [];
const ok = (cond, msg) => results.push([!!cond, msg]);
const dmy = d => { const [y, m, day] = String(d).substring(0, 10).split('-'); return `${Number(day)}/${Number(m)}/${y}`; };

(async () => {
  const browser = await chromium.launch();
  const page = await (await browser.newContext()).newPage();
  const errors = [];
  page.on('pageerror', e => errors.push(e.message));
  await page.goto(`${APP_URL}/connexion`);
  await page.waitForSelector('#fsi-field-username', { state: 'visible' });
  await page.fill('#fsi-field-username', 'prof');
  await page.fill('#fsi-field-password', 'secret-prof');
  await page.click('#signin-view button[type="submit"]');
  await page.waitForTimeout(800);
  await page.locator('a[href="/mobilites"]:visible').first().click();
  await page.waitForSelector('#mobilities-view table tbody tr', { state: 'visible' });
  await page.waitForTimeout(500);
  await page.locator('#mobilities-view table tbody tr', { hasText: 'Terminée' }).first().click();
  await page.waitForSelector('#first-payment-status .content', { state: 'visible', timeout: 8000 });
  const first = (await page.locator('#first-payment-status .content').textContent()).trim();
  const second = (await page.locator('#second-payment-status .content').textContent()).trim();
  await page.screenshot({ path: path.join(SHOTS, '30-mobility-detail.png'), fullPage: true });

  const done = await page.evaluate(async () =>
    (await (await fetch('/api/1.0/mobilities')).json()).data.find(m => m.state === 'Terminée'));
  ok(done, 'the API lists a completed (Terminée) mobility');
  if (done) {
    const want1 = `Effectué le ${dmy(done.firstPaymentRequestDate)}`;
    const want2 = `Effectué le ${dmy(done.secondPaymentRequestDate)}`;
    ok(first === want1, `first payment status "${first}" is "${want1}"`);
    ok(second === want2, `second payment status "${second}" is "${want2}"`);
  }
  ok(errors.length === 0, `no page error${errors.length ? ': ' + errors.join(' | ') : ''}`);

  await browser.close();
  for (const [pass, msg] of results) console.log((pass ? 'PASS ' : 'FAIL ') + msg);
  const failed = results.filter(r => !r[0]).length;
  console.log(`${results.length - failed} passed, ${failed} failed`);
  process.exit(failed ? 1 : 0);
})().catch(e => { console.error(e); process.exit(2); });
