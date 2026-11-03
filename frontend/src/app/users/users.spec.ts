import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { User } from '../core/models';
import { SessionService } from '../core/session.service';
import { Users, fullName } from './users';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };
const PROFESSOR: User = { id: 1, username: 'prof', firstName: 'Paula', lastName: 'Prof', role: 'Professor' };

function user(id: number, username: string, firstName: string, lastName: string, role: string, registrationDate?: string): User {
  return {
    id,
    username,
    firstName,
    lastName,
    email: `${username}@example.test`,
    role,
    option: { code: 'BIN', name: 'Bachelier en informatique de gestion' },
    registrationDate,
    version: 1,
  };
}

const PROF = user(1, 'prof', 'Paula', 'Prof', 'Professor', '2026-09-01T08:30:00.123456');
const ALICE = user(2, 'alice', 'Alice', 'Tester', 'Student', '2026-11-03T01:50:50.872502');
const BRUNO = user(3, 'bruno', 'Bruno', 'Abel', 'Student', '2026-10-15T23:59:59');
const ALL = [PROF, ALICE, BRUNO];

describe('Users', () => {
  let fixture: ComponentFixture<Users>;
  let http: HttpTestingController;
  let router: Router;
  let el: HTMLElement;

  /** Signs the professor in, opens the screen and returns the pending `GET /users`. */
  function open(): TestRequest {
    TestBed.configureTestingModule({
      imports: [Users],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(), { provide: LOCALE_ID, useValue: 'fr' }],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    TestBed.inject(SessionService).signIn({ username: 'prof', password: 'secret' }).subscribe();
    http.expectOne('/api/1.0/session').flush(PROFESSOR);
    fixture = TestBed.createComponent(Users);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    return http.expectOne('/api/1.0/users');
  }

  function show(users: User[] = ALL): void {
    open().flush({ data: users });
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  const rows = () => Array.from(el.querySelectorAll<HTMLTableRowElement>('tbody tr'));
  const row = (username: string) => el.querySelector<HTMLTableRowElement>(`tbody tr[data-username="${username}"]`)!;
  const cells = (tr: HTMLTableRowElement) => Array.from(tr.querySelectorAll('th, td')).map((c) => c.textContent?.trim());
  const alert = () => el.querySelector('[role=alert]');
  const panel = () => el.querySelector('.promote-panel');
  const notice = () => el.querySelector('.notice')?.textContent?.trim();

  function askPromotion(username: string): void {
    row(username).querySelector<HTMLButtonElement>('button.promote')!.click();
    fixture.detectChanges();
  }

  function confirm(): void {
    panel()!.querySelector<HTMLButtonElement>('button.confirm')!.click();
    fixture.detectChanges();
  }

  describe('list', () => {
    it('shows a loading state, then every user sorted by name', () => {
      const req = open();
      expect(el.querySelector('h1')?.textContent).toContain('Gestion des utilisateurs');
      expect(el.querySelector('[role=status].state')?.textContent).toContain('Chargement des utilisateurs');
      req.flush({ data: ALL });
      fixture.detectChanges();
      expect(el.querySelector('.state')).toBeNull();
      expect(rows().map((tr) => tr.dataset['username'])).toEqual(['bruno', 'prof', 'alice']);
      expect(el.querySelector('.count')?.textContent).toContain("Nombre d'utilisateurs : 3");
    });

    it('has the columns of the legacy list and the registration date', () => {
      show();
      expect(Array.from(el.querySelectorAll('thead th')).map((th) => th.textContent?.trim())).toEqual([
        'Nom',
        "Nom d'utilisateur",
        'E-mail',
        'Option',
        'Rôle',
        "Date d'inscription",
        'Action',
      ]);
      // dates in the UI language (French here)
      expect(cells(row('alice'))).toEqual(['Alice Tester', 'alice', 'alice@example.test', 'BIN', 'Étudiant', '3 nov. 2026', 'Promouvoir']);
      expect(cells(row('prof'))).toEqual(['Paula Prof', 'prof', 'prof@example.test', 'BIN', 'Professeur', '1 sept. 2026', '']);
      expect(row('alice').querySelector('th')?.getAttribute('scope')).toBe('row');
      expect(row('alice').querySelector('a')?.getAttribute('href')).toBe('mailto:alice@example.test');
      expect(row('alice').dataset['id']).toBe('2');
      // the date is the day the server wrote, whatever the browser's time zone
      expect(cells(row('bruno'))[5]).toBe('15 oct. 2026');
    });

    it('offers the promotion to students only', () => {
      show();
      expect(row('alice').querySelector('button.promote')).not.toBeNull();
      expect(row('bruno').querySelector('button.promote')).not.toBeNull();
      expect(row('prof').querySelector('button.promote')).toBeNull();
      expect(row('prof').querySelector('.badge-professor')).not.toBeNull();
      expect(row('alice').querySelector('.badge-professor')).toBeNull();
    });

    it('shows an account without name, e-mail, option nor registration date by its username', () => {
      show([{ id: 9, username: 'zed', role: 'Student' }, { id: 8, username: 'adam', role: 'Student' }, ALICE]);
      expect(rows().map((tr) => tr.dataset['username'])).toEqual(['adam', 'zed', 'alice']);
      expect(cells(row('zed'))).toEqual(['zed', 'zed', '', '', 'Étudiant', '', 'Promouvoir']);
      expect(row('zed').querySelector('a')).toBeNull();
    });

    it('says when there is no user', () => {
      show([]);
      expect(el.querySelector('table')).toBeNull();
      expect(el.querySelector('.state')?.textContent).toContain('Aucun utilisateur.');
    });

    it('shows a failure with a retry button', () => {
      open().flush(null, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();
      expect(alert()?.textContent).toContain('Une erreur est survenue sur le serveur');
      expect(el.querySelector('table')).toBeNull();

      el.querySelector<HTMLButtonElement>('button.retry')!.click();
      fixture.detectChanges();
      expect(alert()).toBeNull();
      http.expectOne('/api/1.0/users').flush({ data: ALL });
      fixture.detectChanges();
      expect(rows().length).toBe(3);
    });

    it('goes back to the sign-in page when the session expired', () => {
      const req = open();
      const navigate = spyOn(router, 'navigate').and.resolveTo(true);
      req.flush({ code: 'UNAUTHENTICATED', status: 401 }, { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS });
      fixture.detectChanges();
      expect(navigate).toHaveBeenCalledOnceWith(['/signin']);
      expect(TestBed.inject(SessionService).user()).toBeNull();
      expect(alert()).toBeNull();
    });
  });

  describe('promotion', () => {
    beforeEach(() => show());

    it('asks for a confirmation, focused on the confirm button, before sending anything', async () => {
      askPromotion('alice');
      await fixture.whenStable();
      expect(panel()?.querySelector('h2')?.textContent).toContain('Promouvoir un utilisateur');
      expect(panel()?.textContent).toContain('Promouvoir Alice Tester (alice) au rôle de professeur ? Cette action est définitive.');
      expect(document.activeElement).toBe(panel()!.querySelector('button.confirm'));
      http.expectNone('/api/1.0/users/2/promote');
    });

    it('promotes the student with PUT /users/{id}/promote and refreshes the row', () => {
      askPromotion('alice');
      confirm();
      const req = http.expectOne('/api/1.0/users/2/promote');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toBeNull();
      expect(panel()!.querySelector<HTMLButtonElement>('button.confirm')!.disabled).toBeTrue();
      expect(row('bruno').querySelector<HTMLButtonElement>('button.promote')!.disabled).toBeTrue();

      // a second confirmation while the request is in flight is ignored
      (fixture.componentInstance as unknown as { confirmPromotion(): void }).confirmPromotion();
      http.expectNone('/api/1.0/users/2/promote');

      req.flush(null);
      fixture.detectChanges();
      expect(panel()).toBeNull();
      expect(notice()).toBe('Alice Tester est maintenant professeur.');
      expect(cells(row('alice'))[4]).toBe('Professeur');
      expect(row('alice').querySelector('button.promote')).toBeNull();
      expect(row('bruno').querySelector<HTMLButtonElement>('button.promote')!.disabled).toBeFalse();
      // the other rows are unchanged and nothing else is requested
      expect(cells(row('bruno'))[4]).toBe('Étudiant');
    });

    it('closes the confirmation without sending anything', () => {
      askPromotion('bruno');
      panel()!.querySelector<HTMLButtonElement>('button.cancel')!.click();
      fixture.detectChanges();
      expect(panel()).toBeNull();
      http.expectNone('/api/1.0/users/3/promote');
      (fixture.componentInstance as unknown as { confirmPromotion(): void }).confirmPromotion();
      http.expectNone('/api/1.0/users/3/promote');
    });

    it("shows the API's refusal in the confirmation, which stays open", () => {
      askPromotion('bruno');
      confirm();
      http.expectOne('/api/1.0/users/3/promote').flush(
        { code: 'RESOURCE_NOT_FOUND', status: 404, detail: 'La ressource demandée n’existe pas.' },
        { status: 404, statusText: 'Not Found', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(panel()?.querySelector('[role=alert]')?.textContent).toContain('La ressource demandée n’existe pas.');
      expect(cells(row('bruno'))[4]).toBe('Étudiant');
      expect(notice()).toBe('');

      // another student: the old refusal is gone
      askPromotion('alice');
      expect(panel()?.querySelector('[role=alert]')).toBeNull();
      expect(panel()?.textContent).toContain('(alice)');
      confirm();
      http.expectOne('/api/1.0/users/2/promote').flush(
        { code: 'ACCESS_DENIED', status: 403, detail: 'Accès refusé.' },
        { status: 403, statusText: 'Forbidden', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(panel()?.querySelector('[role=alert]')?.textContent).toContain('Accès refusé.');
      panel()!.querySelector<HTMLButtonElement>('button.cancel')!.click();
      fixture.detectChanges();
      expect(alert()).toBeNull();
    });

    it('clears the success message when another promotion is asked for', () => {
      askPromotion('alice');
      confirm();
      http.expectOne('/api/1.0/users/2/promote').flush(null);
      fixture.detectChanges();
      expect(notice()).toContain('Alice Tester');
      askPromotion('bruno');
      expect(notice()).toBe('');
    });

    it('goes back to the sign-in page when the session expired before the promotion', () => {
      const navigate = spyOn(router, 'navigate').and.resolveTo(true);
      askPromotion('alice');
      confirm();
      http.expectOne('/api/1.0/users/2/promote').flush({ code: 'UNAUTHENTICATED', status: 401 }, { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS });
      expect(navigate).toHaveBeenCalledOnceWith(['/signin']);
    });
  });
});

describe('fullName', () => {
  it('writes "First Last", or the username of an account without name', () => {
    expect(fullName({ username: 'alice', firstName: 'Alice', lastName: 'Tester' })).toBe('Alice Tester');
    expect(fullName({ username: 'alice', lastName: 'Tester' })).toBe('Tester');
    expect(fullName({ username: 'alice' })).toBe('alice');
  });
});
