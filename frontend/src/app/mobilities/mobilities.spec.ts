import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { Mobility, User } from '../core/models';
import { SessionService } from '../core/session.service';
import { Mobilities } from './mobilities';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };
const PROFESSOR: User = { id: 1, username: 'prof', firstName: 'Paula', role: 'Professor' };
const STUDENT: User = { id: 2, username: 'alice', firstName: 'Alice', role: 'Student' };

function mobility(
  id: number,
  firstName: string,
  lastName: string,
  academicYear: number,
  term: number,
  state: string,
): Mobility {
  return {
    id,
    version: 3,
    mobilityType: id % 2 ? 'SMS' : 'SMP',
    academicYear,
    term,
    state,
    programme: { id: 1, programmeName: 'Erasmus+' },
    country: { countryCode: 'FR', name: 'France' },
    partner: { id: 1, fullName: 'Universite de Lyon' },
    nominatedStudent: { id: 10 + id, firstName, lastName, option: { code: 'BIN' } },
    encodedInProEco: false,
    encodedInSecondSoftware: false,
  };
}

const CLOSED = mobility(1, 'Alice', 'Tester', 2026, 1, 'Terminée');
const CANCELLED = mobility(2, 'Alice', 'Tester', 2026, 2, 'Annulée');
const RUNNING = mobility(3, 'Bruno', 'Abel', 2026, 1, 'En cours');
const OLD = mobility(5, 'Carla', 'Zed', 2025, 2, 'Terminée');

describe('Mobilities', () => {
  let fixture: ComponentFixture<Mobilities>;
  let http: HttpTestingController;
  let router: Router;
  let el: HTMLElement;

  /** Signs the user in, opens the screen and returns the pending `GET /mobilities`. */
  function open(user: User = PROFESSOR): TestRequest {
    TestBed.configureTestingModule({
      imports: [Mobilities],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: LOCALE_ID, useValue: 'fr' },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    TestBed.inject(SessionService).signIn({ username: user.username, password: 's' }).subscribe();
    http.expectOne('/api/1.0/session').flush(user);
    fixture = TestBed.createComponent(Mobilities);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    return http.expectOne('/api/1.0/mobilities');
  }

  function show(mobilities: Mobility[], user: User = PROFESSOR): void {
    open(user).flush({ data: mobilities });
    fixture.detectChanges();
  }

  const rows = () => Array.from(el.querySelectorAll<HTMLTableRowElement>('tbody tr'));
  const cells = (row: HTMLTableRowElement) =>
    Array.from(row.querySelectorAll('th, td')).map((c) => c.textContent?.trim());

  afterEach(() => http.verify());

  it('shows the loading state until the API answers', () => {
    const req = open();
    expect(el.querySelector('p.state')?.textContent).toContain('Chargement des mobilités');
    req.flush({ data: [] });
    fixture.detectChanges();
    expect(el.textContent).not.toContain('Chargement des mobilités');
  });

  it('lists every mobility for a professor with the legacy columns, latest departure first', () => {
    show([OLD, CLOSED, RUNNING, CANCELLED]);
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Gestion des mobilités');
    expect(el.querySelector('.count')?.textContent).toContain('4');
    expect(Array.from(el.querySelectorAll('thead th')).map((th) => th.textContent?.trim())).toEqual(
      ['Option', 'Etudiant', 'Partenaire', 'Type', 'Programme', 'Départ', 'Etat'],
    );
    expect(rows().map((r) => r.dataset['id'])).toEqual(['2', '3', '1', '5']);
    expect(cells(rows()[0])).toEqual([
      'BIN',
      'Alice Tester',
      'Universite de Lyon',
      'Stage',
      'Erasmus+',
      'Février 2026',
      'Annulée',
    ]);
    expect(cells(rows()[2])[3]).toBe('Académique');
    expect(rows()[0].querySelector('.badge')?.className).toBe('badge badge-cancelled');
    expect(rows()[1].querySelector('.badge')?.className).toBe('badge badge-running');
    expect(rows()[2].querySelector('.badge')?.className).toBe('badge badge-closed');
    expect(rows()[2].dataset['state']).toBe('Terminée');
  });

  it('opens the detail from the student name', () => {
    show([CLOSED]);
    const link = rows()[0].querySelector<HTMLAnchorElement>('th a')!;
    expect(link.getAttribute('href')).toBe('/mobilities/1');
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    link.click();
    expect(router.navigateByUrl).toHaveBeenCalled();
  });

  it('sorts the same departure by last name, then first name, missing names first', () => {
    const anonymous = mobility(7, '', '', 2026, 1, 'Créée');
    anonymous.nominatedStudent = { id: 17 };
    const twin = mobility(9, 'Aaron', 'Tester', 2026, 1, 'Créée');
    show([CLOSED, RUNNING, twin, anonymous]);
    expect(rows().map((r) => r.dataset['id'])).toEqual(['7', '3', '9', '1']);
  });

  it('sorts the students without a first name before the others of the same last name', () => {
    const anonymous = mobility(7, '', '', 2026, 1, 'Créée');
    anonymous.nominatedStudent = { id: 17 };
    const lastOnly = mobility(9, '', 'Tester', 2026, 1, 'Créée');
    lastOnly.nominatedStudent = { id: 19, lastName: 'Tester' };
    const zoe = mobility(11, 'Zoe', 'Tester', 2026, 1, 'Créée');
    show([anonymous, CLOSED, lastOnly, zoe]);
    expect(rows().map((r) => r.dataset['id'])).toEqual(['7', '9', '1', '11']);
  });

  it('shows the country when the mobility has no partner, and drops a missing flag', () => {
    const noPartner = { ...CLOSED, partner: null };
    const noCountry = { ...RUNNING, country: null };
    show([noPartner, noCountry]);
    const row = (id: number) => el.querySelector(`tbody tr[data-id="${id}"]`)!;
    const flag = row(1).querySelector<HTMLImageElement>('img.flag')!;
    expect(flag.getAttribute('src')).toBe('/images/flags/FR.png');
    expect(row(1).textContent).toContain('France');
    flag.dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(row(1).querySelector('img.flag')).toBeNull();
    expect(row(3).querySelector('img.flag')).toBeNull();
    expect(row(3).textContent).toContain('Universite de Lyon');
  });

  it("lists a student's own mobilities without the option column", () => {
    show([CLOSED], STUDENT);
    expect(el.querySelector('h1')?.textContent?.trim()).toBe('Mes mobilités');
    expect(el.querySelectorAll('thead th').length).toBe(6);
    expect(cells(rows()[0])[0]).toBe('Alice Tester');
  });

  it('says when there is no mobility', () => {
    show([]);
    expect(el.querySelector('table')).toBeNull();
    expect(el.querySelector('p.state')?.textContent).toContain('Aucune mobilité.');
  });

  it('shows a failure with a retry button that reloads the list', () => {
    open().flush(
      { code: 'INTERNAL_ERROR', status: 500, detail: 'Le serveur a échoué.' },
      { status: 500, statusText: 'Server Error', headers: PROBLEM_HEADERS },
    );
    fixture.detectChanges();
    expect(el.querySelector('[role="alert"]')?.textContent).toContain('Le serveur a échoué.');
    el.querySelector<HTMLButtonElement>('button.retry')!.click();
    fixture.detectChanges();
    http.expectOne('/api/1.0/mobilities').flush({ data: [CLOSED] });
    fixture.detectChanges();
    expect(rows().length).toBe(1);
    expect(el.querySelector('[role="alert"]')).toBeNull();
  });

  it('goes back to the sign-in page when the session expired', () => {
    const req = open();
    const navigate = spyOn(router, 'navigate').and.resolveTo(true);
    req.flush(
      { code: 'UNAUTHENTICATED', status: 401, detail: 'Connectez-vous.' },
      { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS },
    );
    expect(navigate).toHaveBeenCalledWith(['/signin']);
    expect(TestBed.inject(SessionService).user()).toBeNull();
  });
});
