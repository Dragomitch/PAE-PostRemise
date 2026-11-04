import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { DenialReason, User } from '../core/models';
import { SessionService } from '../core/session.service';
import { DenialReasons } from './denial-reasons';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };
const PROFESSOR: User = {
  id: 1,
  username: 'prof',
  firstName: 'Paula',
  lastName: 'Prof',
  role: 'Professor',
};
const SEMESTER: DenialReason = {
  id: 1,
  reason: "L'étudiant part en mobilité pour ce semestre.",
  version: 0,
};
const INCOMPLETE: DenialReason = { id: 4, reason: 'Dossier incomplet', version: 0 };
const ALL = [SEMESTER, INCOMPLETE];

describe('DenialReasons', () => {
  let fixture: ComponentFixture<DenialReasons>;
  let http: HttpTestingController;
  let router: Router;
  let el: HTMLElement;

  /** Signs the professor in, opens the screen and returns the pending `GET /denialReasons`. */
  function open(): TestRequest {
    TestBed.configureTestingModule({
      imports: [DenialReasons],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    TestBed.inject(SessionService).signIn({ username: 'prof', password: 'secret' }).subscribe();
    http.expectOne('/api/1.0/session').flush(PROFESSOR);
    fixture = TestBed.createComponent(DenialReasons);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    return http.expectOne('/api/1.0/denialReasons');
  }

  function show(reasons: DenialReason[] = ALL): void {
    open().flush(reasons);
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  const rows = () => Array.from(el.querySelectorAll<HTMLTableRowElement>('tbody tr'));
  const row = (id: number) => el.querySelector<HTMLTableRowElement>(`tbody tr[data-id="${id}"]`)!;
  const cells = (tr: HTMLTableRowElement) =>
    Array.from(tr.querySelectorAll(':scope > th, :scope > td')).map((c) => c.textContent?.trim());
  const alert = () => el.querySelector('[role=alert]');
  const notice = () => el.querySelector('.notice')?.textContent?.trim();
  const click = (selector: string, root: ParentNode = el) => {
    root.querySelector<HTMLElement>(selector)!.click();
    fixture.detectChanges();
  };
  const editInput = () => el.querySelector<HTMLInputElement>('#denial-reasons-edit-reason')!;
  const editError = () => el.querySelector('#denial-reasons-edit-reason-error');
  function type(target: HTMLInputElement, value: string): void {
    target.value = value;
    target.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  describe('list', () => {
    it('shows a loading state, then every reason in the order of the API', () => {
      const req = open();
      expect(el.querySelector('h1')?.textContent).toContain('Raisons de refus');
      expect(el.querySelector('.intro')?.textContent).toContain(
        'Les raisons proposées pour refuser une demande de mobilité ou annuler une mobilité.',
      );
      expect(el.querySelector('[role=status].state')?.textContent).toContain(
        'Chargement des raisons de refus',
      );
      req.flush(ALL);
      fixture.detectChanges();
      expect(el.querySelector('.state')).toBeNull();
      expect(
        Array.from(el.querySelectorAll('thead th')).map((th) => th.textContent?.trim()),
      ).toEqual(['Raison', 'Action']);
      expect(rows().map((tr) => tr.dataset['id'])).toEqual(['1', '4']);
      expect(cells(row(4))).toEqual(['Dossier incomplet', 'Modifier']);
      expect(row(4).querySelector('th')?.getAttribute('scope')).toBe('row');
      expect(el.querySelector('.count')?.textContent).toContain('Nombre de raisons : 2');
    });

    it('says when there is no reason', () => {
      show([]);
      expect(el.querySelector('table')).toBeNull();
      expect(el.querySelector('.state')?.textContent).toContain('Aucune raison de refus.');
    });

    it('shows a failure with a retry button', () => {
      open().flush(null, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();
      expect(alert()?.textContent).toContain('Une erreur est survenue sur le serveur');
      expect(el.querySelector('table')).toBeNull();

      click('button.retry');
      expect(alert()).toBeNull();
      http.expectOne('/api/1.0/denialReasons').flush(ALL);
      fixture.detectChanges();
      expect(rows().length).toBe(2);
    });

    it('goes back to the sign-in page when the session expired', () => {
      const req = open();
      const navigate = spyOn(router, 'navigate').and.resolveTo(true);
      req.flush(
        { code: 'UNAUTHENTICATED', status: 401 },
        { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(navigate).toHaveBeenCalledOnceWith(['/signin']);
      expect(TestBed.inject(SessionService).user()).toBeNull();
    });
  });

  describe('creation', () => {
    const panel = () => el.querySelector<HTMLElement>('.creation-panel');

    it('opens the creation form in place of the button, then closes it', async () => {
      show();
      expect(panel()).toBeNull();
      click('button.new-reason');
      expect(panel()?.querySelector('h2')?.textContent).toContain('Créer une raison de refus');
      expect(el.querySelector('button.new-reason')).toBeNull();
      expect(panel()?.querySelector('#denial-reasons-create-reason')).not.toBeNull();

      click('button.close-form', panel()!);
      expect(panel()).toBeNull();
      await fixture.whenStable();
      expect(document.activeElement).toBe(el.querySelector('button.new-reason'));
      http.expectNone('/api/1.0/denialReasons');
    });

    it('adds the created reason to the table and says so', () => {
      show();
      click('button.new-reason');
      type(panel()!.querySelector('input')!, 'Désistement');
      click('button.create', panel()!);
      const req = http.expectOne('/api/1.0/denialReasons');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ reason: 'Désistement' });
      req.flush({ id: 9, reason: 'Désistement', version: 0 });
      fixture.detectChanges();
      expect(panel()).toBeNull();
      expect(rows().map((tr) => tr.dataset['id'])).toEqual(['1', '4', '9']);
      expect(cells(row(9))).toEqual(['Désistement', 'Modifier']);
      expect(notice()).toBe('La raison « Désistement » a été créée.');
      expect(el.querySelector('.count')?.textContent).toContain('Nombre de raisons : 3');
    });

    it('creates the first reason of an empty list', () => {
      show([]);
      click('button.new-reason');
      type(panel()!.querySelector('input')!, 'Désistement');
      click('button.create', panel()!);
      http.expectOne('/api/1.0/denialReasons').flush({ id: 9, reason: 'Désistement', version: 0 });
      fixture.detectChanges();
      expect(rows().length).toBe(1);
    });

    it('closes the edition of a row', () => {
      show();
      click('button.edit', row(4));
      expect(editInput()).not.toBeNull();
      click('button.new-reason');
      expect(el.querySelector('#denial-reasons-edit-reason')).toBeNull();
      expect(panel()).not.toBeNull();
    });
  });

  describe('edition', () => {
    it('edits the text in its row, the focus in the input', async () => {
      show();
      click('button.edit', row(4));
      expect(row(4).querySelector('td.edition')?.getAttribute('colspan')).toBe('2');
      expect(row(4).querySelector('label')?.textContent).toContain('Texte de la raison');
      expect(editInput().value).toBe('Dossier incomplet');
      expect(editInput().getAttribute('maxlength')).toBe('300');
      expect(cells(row(1))).toEqual(["L'étudiant part en mobilité pour ce semestre.", 'Modifier']);
      await fixture.whenStable();
      expect(document.activeElement).toBe(editInput());

      click('button.cancel-edit', row(4));
      expect(el.querySelector('#denial-reasons-edit-reason')).toBeNull();
      expect(cells(row(4))).toEqual(['Dossier incomplet', 'Modifier']);
      http.expectNone(() => true);
    });

    it('closes the creation form', () => {
      show();
      click('button.new-reason');
      click('button.edit', row(4));
      expect(el.querySelector('.creation-panel')).toBeNull();
      expect(el.querySelector('button.new-reason')).not.toBeNull();
    });

    it('requires a text that is not only spaces, at most 300 characters', () => {
      show();
      click('button.edit', row(4));
      type(editInput(), '   ');
      click('button.save', row(4));
      expect(editInput().value).toBe('');
      expect(editError()?.textContent).toContain('Ce champ est obligatoire.');
      expect(editInput().getAttribute('aria-invalid')).toBe('true');
      expect(editInput().getAttribute('aria-describedby')).toBe('denial-reasons-edit-reason-error');

      type(editInput(), 'x'.repeat(301));
      expect(editError()?.textContent).toContain('La raison ne peut pas dépasser 300 caractères.');
      click('button.save', row(4));
      http.expectNone('/api/1.0/denialReasons/4');
    });

    it('sends the trimmed text with the version read, then shows the answer', () => {
      show();
      click('button.edit', row(4));
      type(editInput(), '  Dossier incomplet ou tardif ');
      click('button.save', row(4));
      const req = http.expectOne('/api/1.0/denialReasons/4');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({
        id: 4,
        reason: 'Dossier incomplet ou tardif',
        version: 0,
      });
      const save = row(4).querySelector<HTMLButtonElement>('button.save')!;
      expect(save.disabled).toBeTrue();
      expect(save.getAttribute('aria-busy')).toBe('true');
      expect(row(1).querySelector<HTMLButtonElement>('button.edit')!.disabled).toBeTrue();

      // a second submit while the first one is in flight sends nothing
      fixture.componentInstance['save']();
      http.expectNone('/api/1.0/denialReasons/4');

      req.flush({ id: 4, reason: 'Dossier incomplet ou tardif', version: 0 });
      fixture.detectChanges();
      expect(el.querySelector('#denial-reasons-edit-reason')).toBeNull();
      expect(cells(row(4))).toEqual(['Dossier incomplet ou tardif', 'Modifier']);
      expect(cells(row(1))[0]).toBe("L'étudiant part en mobilité pour ce semestre.");
      expect(notice()).toBe('La raison « Dossier incomplet ou tardif » a été modifiée.');
    });

    it('ignores a save without an edited row', () => {
      show();
      fixture.componentInstance['save']();
      fixture.detectChanges();
      http.expectNone(() => true);
      expect(editError()).toBeNull();
    });

    it("shows the API's field errors in the row", () => {
      show();
      click('button.edit', row(4));
      click('button.save', row(4));
      http.expectOne('/api/1.0/denialReasons/4').flush(
        {
          code: 'VALIDATION_FAILED',
          status: 400,
          detail: 'Certains champs sont invalides.',
          errors: [
            { field: 'reason', code: 'Size', message: 'Trop long.' },
            { field: 'id', code: 'Positive', message: 'Identifiant invalide.' },
          ],
        },
        { status: 400, statusText: 'Bad Request', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(editError()?.textContent?.trim()).toBe('Trop long.');
      const problem = row(4).querySelector('[role=alert]')!;
      expect(problem.textContent).toContain('Certains champs sont invalides.');
      expect(Array.from(problem.querySelectorAll('li')).map((li) => li.textContent)).toEqual([
        'Identifiant invalide.',
      ]);
      expect(problem.querySelector('button.reload')).toBeNull();
    });

    it('offers to reload the list when the reason changed meanwhile (409)', () => {
      show();
      click('button.edit', row(4));
      click('button.save', row(4));
      http.expectOne('/api/1.0/denialReasons/4').flush(
        {
          code: 'CONCURRENT_MODIFICATION',
          status: 409,
          detail: 'Ces données ont été modifiées entre-temps.',
        },
        { status: 409, statusText: 'Conflict', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      const problem = row(4).querySelector('[role=alert]')!;
      expect(problem.textContent).toContain('Ces données ont été modifiées entre-temps.');
      expect(problem.querySelector('ul')).toBeNull();

      click('button.reload', problem);
      http.expectOne('/api/1.0/denialReasons').flush([SEMESTER]);
      fixture.detectChanges();
      expect(el.querySelector('#denial-reasons-edit-reason')).toBeNull();
      expect(rows().map((tr) => tr.dataset['id'])).toEqual(['1']);
      expect(alert()).toBeNull();
    });

    it('goes back to the sign-in page when the session expired', () => {
      show();
      const navigate = spyOn(router, 'navigate').and.resolveTo(true);
      click('button.edit', row(4));
      click('button.save', row(4));
      http
        .expectOne('/api/1.0/denialReasons/4')
        .flush(
          { code: 'UNAUTHENTICATED', status: 401 },
          { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS },
        );
      fixture.detectChanges();
      expect(navigate).toHaveBeenCalledOnceWith(['/signin']);
    });
  });
});
