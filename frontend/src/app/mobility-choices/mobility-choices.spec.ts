import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { MobilityChoice, User } from '../core/models';
import { SessionService } from '../core/session.service';
import { choiceState, departureLabel, mobilityTypeLabel } from './mobility-choice-labels';
import { MobilityChoices } from './mobility-choices';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };

const PROFESSOR: User = {
  id: 1,
  username: 'prof',
  firstName: 'Paula',
  lastName: 'Prof',
  role: 'Professor',
};
const STUDENT: User = {
  id: 2,
  username: 'alice',
  firstName: 'Alice',
  lastName: 'Tester',
  role: 'Student',
};

function choice(id: number, extra: Partial<MobilityChoice> = {}): MobilityChoice {
  return {
    id,
    user: { id: 2, firstName: 'Alice', lastName: 'Tester' },
    preferenceOrder: 1,
    mobilityType: 'SMS',
    academicYear: 2026,
    term: 1,
    programme: { id: 1, programmeName: 'Erasmus+' },
    country: { countryCode: 'FR', name: 'France' },
    partner: { id: 3, fullName: 'Université de Lyon' },
    ...extra,
  };
}

const LYON = choice(10);
const BERLIN = choice(11, {
  preferenceOrder: 2,
  term: 2,
  academicYear: 2027,
  country: { countryCode: 'DE', name: 'Allemagne' },
  partner: { id: 4, fullName: 'Hochschule Berlin' },
});
const NO_PARTNER = choice(12, {
  preferenceOrder: 3,
  mobilityType: 'SMP',
  country: null,
  partner: null,
});
const CANCELED = choice(13, { cancellationReason: "J'ai changé d'avis" });
const REJECTED = choice(14, {
  user: { id: 5, firstName: 'Bruno', lastName: 'Tester' },
  denialReason: { id: 1 },
});

describe('MobilityChoices', () => {
  let fixture: ComponentFixture<MobilityChoices>;
  let http: HttpTestingController;
  let router: Router;
  let el: HTMLElement;

  function open(user: User): void {
    TestBed.configureTestingModule({
      imports: [MobilityChoices],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    TestBed.inject(SessionService)
      .signIn({ username: user.username, password: 'secret' })
      .subscribe();
    http.expectOne('/api/1.0/session').flush(user);
    fixture = TestBed.createComponent(MobilityChoices);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  }

  const listRequest = (filter?: string) =>
    http.expectOne(
      (r) => r.url === '/api/1.0/mobilityChoices' && r.params.get('filter') === (filter ?? null),
    );
  const countRequest = (filter: string) =>
    http.expectOne(
      (r) => r.url === '/api/1.0/mobilityChoices/count' && r.params.get('filter') === filter,
    );

  /** Answers the student's list request. */
  function studentList(choices: MobilityChoice[]): void {
    listRequest().flush({ data: choices });
    fixture.detectChanges();
  }

  /** Answers the professor's list and count requests (and the mobilities when asked). */
  function professorList(filter: string, choices: MobilityChoice[], mobilityIds?: number[]): void {
    listRequest(filter).flush({ data: choices });
    countRequest(filter).flush({ count: choices.length });
    if (mobilityIds) {
      http.expectOne('/api/1.0/mobilities').flush({ data: mobilityIds.map((id) => ({ id })) });
    }
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  const rows = () => Array.from(el.querySelectorAll<HTMLTableRowElement>('tbody tr'));
  const row = (id: number) => el.querySelector<HTMLTableRowElement>(`tbody tr[data-id="${id}"]`)!;
  /** The text pieces of a cell joined with a space (the template leaves no space between elements). */
  const cellText = (cell: Element) => {
    const walker = document.createTreeWalker(cell, NodeFilter.SHOW_TEXT);
    const parts: string[] = [];
    while (walker.nextNode()) {
      const text = walker.currentNode.textContent!.replace(/\s+/g, ' ').trim();
      if (text) {
        parts.push(text);
      }
    }
    return parts.join(' ');
  };
  const cells = (id: number) => Array.from(row(id).querySelectorAll('td, th')).map(cellText);
  const headers = () =>
    Array.from(el.querySelectorAll('thead th')).map((h) => h.textContent?.trim());
  const alert = () => el.querySelector('[role=alert]');
  const notice = () => el.querySelector('.notice')?.textContent?.trim();
  const click = (selector: string, root: ParentNode = el) => {
    root.querySelector<HTMLElement>(selector)!.click();
    fixture.detectChanges();
  };

  describe('loading and failure', () => {
    it('shows a loading state until the choices arrive', () => {
      open(STUDENT);
      expect(el.querySelector('[role=status].state')?.textContent).toContain(
        'Chargement des demandes de mobilité',
      );
      studentList([LYON]);
      expect(el.textContent).not.toContain('Chargement des demandes');
      expect(rows().length).toBe(1);
    });

    it('shows the problem with a retry button, and loads again on retry', () => {
      open(STUDENT);
      listRequest().flush(null, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();
      expect(alert()?.textContent).toContain('Une erreur est survenue sur le serveur');
      expect(el.querySelector('table')).toBeNull();

      click('button.retry');
      expect(alert()).toBeNull();
      studentList([LYON]);
      expect(rows().length).toBe(1);
    });

    it("shows the backend's message for a professor whose count fails, without any list", () => {
      open(PROFESSOR);
      listRequest('active').flush({ data: [LYON] });
      countRequest('active').flush(
        { code: 'ACCESS_DENIED', status: 403, detail: 'Accès refusé par le serveur.' },
        { status: 403, statusText: 'Forbidden', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(alert()?.textContent).toContain('Accès refusé par le serveur.');
      expect(el.querySelector('table')).toBeNull();
      expect(el.querySelector('.count')).toBeNull();
    });

    it('goes back to the sign-in page when the session has expired', () => {
      open(STUDENT);
      const navigate = spyOn(router, 'navigate').and.resolveTo(true);
      listRequest().flush(
        { code: 'UNAUTHENTICATED', status: 401 },
        { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(navigate).toHaveBeenCalledOnceWith(['/signin']);
      expect(TestBed.inject(SessionService).user()).toBeNull();
      expect(alert()).toBeNull();
    });

    it('does not update a screen that was closed while the request was in flight', () => {
      open(STUDENT);
      const req = listRequest();
      fixture.destroy();
      expect(req.cancelled).toBeTrue();
    });
  });

  describe('as a student', () => {
    beforeEach(() => {
      open(STUDENT);
      studentList([LYON, BERLIN, NO_PARTNER, CANCELED]);
    });

    it('lists their own choices with the columns of the legacy view, without filter nor count', () => {
      expect(el.querySelector('h1')?.textContent?.trim()).toBe('Mes demandes de mobilité');
      expect(headers()).toEqual([
        'Préférence',
        'Pays',
        'Partenaire',
        'Type',
        'Programme',
        'Départ',
        'État',
        'Actions',
      ]);
      expect(cells(10)).toEqual([
        '1',
        'France',
        'Université de Lyon',
        'Académique',
        'Erasmus+',
        'Septembre 2026',
        'En attente',
        'Annuler',
      ]);
      expect(cells(11)).toEqual([
        '2',
        'Allemagne',
        'Hochschule Berlin',
        'Académique',
        'Erasmus+',
        'Février 2027',
        'En attente',
        'Annuler',
      ]);
      expect(cells(12)).toEqual([
        '3',
        'À définir',
        'Pas de partenaire spécifié',
        'Stage',
        'Erasmus+',
        'Septembre 2026',
        'En attente',
        'Annuler',
      ]);
      expect(cells(13)).toEqual([
        '1',
        'France',
        'Université de Lyon',
        'Académique',
        'Erasmus+',
        'Septembre 2026',
        "Annulée J'ai changé d'avis",
        '',
      ]);
      expect(el.querySelector('#mobility-choices-filter')).toBeNull();
      expect(el.querySelector('.count')).toBeNull();
      expect(el.querySelector('a.export')).toBeNull();
      expect(el.querySelector('button.confirm, button.reject')).toBeNull();
    });

    it('shows the flag of the country, as decoration, and drops a flag that does not load', () => {
      const flag = row(10).querySelector('img.flag')!;
      expect(flag.getAttribute('src')).toBe('/images/flags/FR.png');
      expect(flag.getAttribute('alt')).toBe('');
      flag.dispatchEvent(new Event('error'));
      fixture.detectChanges();
      expect(row(10).querySelector('img')).toBeNull();
      expect(row(13).querySelector('img')).toBeNull(); // same country
      expect(row(11).querySelector('img')).not.toBeNull();
      expect(cells(10)[1]).toBe('France');
    });

    it('opens and closes the creation form', () => {
      expect(el.querySelector('app-mobility-choice-form')).toBeNull();
      click('button.create');
      expect(el.querySelector('app-mobility-choice-form')).not.toBeNull();
      expect(el.querySelector('button.create')).toBeNull();
      http.expectOne('/api/1.0/programmes').flush([]);
      http.expectOne('/api/1.0/countries').flush([]);
      http.expectOne('/api/1.0/partners').flush({ data: [] });
      fixture.detectChanges();

      click('app-mobility-choice-form button.button:not([type=submit])');
      expect(el.querySelector('app-mobility-choice-form')).toBeNull();
      expect(el.querySelector('button.create')).not.toBeNull();
    });

    it('says the choice was saved and loads the list again once the form created it', () => {
      click('button.create');
      http.expectOne('/api/1.0/programmes').flush([{ id: 1, programmeName: 'Erasmus+' }]);
      http
        .expectOne('/api/1.0/countries')
        .flush([{ countryCode: 'FR', name: 'France', programme: { id: 1 } }]);
      http.expectOne('/api/1.0/partners').flush({ data: [] });
      fixture.detectChanges();
      const form = el.querySelector('app-mobility-choice-form form')!;
      for (const [id, value] of [
        ['programme', '1'],
        ['order', '2'],
        [
          'departure',
          form.querySelector<HTMLSelectElement>('#mobility-choice-departure')!.options[1].value,
        ],
      ]) {
        const select = form.querySelector<HTMLSelectElement>(`#mobility-choice-${id}`)!;
        select.value = value;
        select.dispatchEvent(new Event('change'));
      }
      click('button[type=submit]', form);
      http.expectOne('/api/1.0/mobilityChoice').flush(choice(20, { preferenceOrder: 2 }));
      fixture.detectChanges();

      expect(el.querySelector('app-mobility-choice-form')).toBeNull();
      expect(notice()).toBe('La demande de mobilité a été enregistrée.');
      studentList([LYON, choice(20, { preferenceOrder: 2 })]);
      expect(rows().length).toBe(2);
      expect(notice()).toBe('La demande de mobilité a été enregistrée.');
    });

    describe('cancel', () => {
      const panel = () => el.querySelector<HTMLElement>('.action-panel');
      const reason = () =>
        el.querySelector<HTMLTextAreaElement>('#mobility-choices-cancel-reason')!;
      function type(text: string): void {
        reason().value = text;
        reason().dispatchEvent(new Event('input'));
        fixture.detectChanges();
      }

      beforeEach(() => click('button.cancel', row(11)));

      it('asks for a reason in a panel that tells which choice it is about, and focuses it', async () => {
        expect(panel()?.querySelector('h2')?.textContent).toContain(
          'Annuler une demande de mobilité',
        );
        expect(panel()?.querySelector('.summary')?.textContent).toBe(
          'Préférence 2 · Hochschule Berlin · Février 2027',
        );
        expect(
          el.querySelector('label[for=mobility-choices-cancel-reason]')?.textContent,
        ).toContain("Raison de l'annulation :");
        expect(reason().getAttribute('maxlength')).toBe('500');
        await fixture.whenStable();
        expect(document.activeElement).toBe(reason());
      });

      it('requires a reason before sending anything', () => {
        click('button[type=submit]', panel()!);
        expect(el.querySelector('#mobility-choices-reason-error')?.textContent).toContain(
          'Ce champ est obligatoire.',
        );
        expect(reason().getAttribute('aria-invalid')).toBe('true');
        expect(reason().getAttribute('aria-describedby')).toBe('mobility-choices-reason-error');
      });

      it('refuses a reason longer than 500 characters', () => {
        type('x'.repeat(501));
        expect(el.querySelector('#mobility-choices-reason-error')?.textContent).toContain(
          'La raison ne peut pas dépasser 500 caractères.',
        );
        click('button[type=submit]', panel()!);
      });

      it('sends the trimmed reason, then closes the panel, says so and loads the list again', () => {
        type('  Je pars ailleurs  ');
        click('button[type=submit]', panel()!);
        const req = http.expectOne((r) => r.url === '/api/1.0/mobilityChoices/11/cancel');
        expect(req.request.method).toBe('PUT');
        expect(req.request.params.get('reason')).toBe('Je pars ailleurs');
        expect(
          panel()!.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled,
        ).toBeTrue();
        expect(row(10).querySelector<HTMLButtonElement>('button.cancel')!.disabled).toBeTrue();

        // a second submit while the request is in flight is ignored
        (fixture.componentInstance as unknown as { submitPanel(): void }).submitPanel();
        http.expectNone((r) => r.url === '/api/1.0/mobilityChoices/11/cancel');

        req.flush(null);
        fixture.detectChanges();
        expect(panel()).toBeNull();
        expect(notice()).toBe('La demande de mobilité a été annulée.');
        studentList([LYON, { ...BERLIN, cancellationReason: 'Je pars ailleurs' }]);
        expect(cells(11)[6]).toBe('Annulée Je pars ailleurs');
      });

      it("shows the server's field error next to the reason and the other errors in the alert", () => {
        type('Raison');
        click('button[type=submit]', panel()!);
        http
          .expectOne((r) => r.url === '/api/1.0/mobilityChoices/11/cancel')
          .flush(
            {
              code: 'VALIDATION_FAILED',
              status: 400,
              detail: 'Certaines informations sont invalides.',
              errors: [
                { field: 'reason', code: 'NotBlank', message: 'La raison est obligatoire.' },
                { field: 'id', code: 'Positive', message: "L'identifiant doit être positif." },
              ],
            },
            { status: 400, statusText: 'Bad Request', headers: PROBLEM_HEADERS },
          );
        fixture.detectChanges();
        expect(panel()?.querySelector('[role=alert]')?.textContent).toContain(
          'Certaines informations sont invalides.',
        );
        expect(panel()?.querySelector('[role=alert] li')?.textContent).toBe(
          "L'identifiant doit être positif.",
        );
        expect(el.querySelector('#mobility-choices-reason-error')?.textContent).toBe(
          'La raison est obligatoire.',
        );
        expect(notice()).toBe('');
      });

      it('shows a business refusal in the panel and keeps it open', () => {
        type('Raison');
        click('button[type=submit]', panel()!);
        http
          .expectOne((r) => r.url === '/api/1.0/mobilityChoices/11/cancel')
          .flush(
            {
              code: 'MOBILITY_CHOICE_ALREADY_CONFIRMED',
              status: 409,
              detail: 'Cette demande est déjà confirmée.',
            },
            { status: 409, statusText: 'Conflict', headers: PROBLEM_HEADERS },
          );
        fixture.detectChanges();
        expect(panel()?.querySelector('[role=alert]')?.textContent).toContain(
          'Cette demande est déjà confirmée.',
        );
        expect(panel()?.querySelector('[role=alert] ul')).toBeNull();
        expect(
          panel()!.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled,
        ).toBeFalse();
      });

      it('goes back to the sign-in page when the session expired meanwhile', () => {
        const navigate = spyOn(router, 'navigate').and.resolveTo(true);
        type('Raison');
        click('button[type=submit]', panel()!);
        http
          .expectOne((r) => r.url === '/api/1.0/mobilityChoices/11/cancel')
          .flush(
            { code: 'UNAUTHENTICATED', status: 401 },
            { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS },
          );
        expect(navigate).toHaveBeenCalledOnceWith(['/signin']);
      });

      it('closes without sending anything, and starts empty when opened again', () => {
        type('Brouillon');
        click('button.button:not([type=submit])', panel()!);
        expect(panel()).toBeNull();
        click('button.cancel', row(10));
        expect(reason().value).toBe('');
        expect(panel()?.querySelector('.summary')?.textContent).toBe(
          'Préférence 1 · Université de Lyon · Septembre 2026',
        );
      });

      it('closes the panel when the creation form opens', () => {
        click('button.create');
        expect(panel()).toBeNull();
        http.match(() => true);
      });
    });
  });

  describe('as a student without any choice', () => {
    it('says there is no choice yet', () => {
      open(STUDENT);
      studentList([]);
      expect(el.querySelector('table')).toBeNull();
      expect(el.textContent).toContain("Vous n'avez encore introduit aucune demande de mobilité.");
    });
  });

  describe('as a professor', () => {
    beforeEach(() => {
      open(PROFESSOR);
      professorList('active', [LYON, NO_PARTNER]);
    });

    const filter = () => el.querySelector<HTMLSelectElement>('#mobility-choices-filter')!;
    function pick(value: string): void {
      filter().value = value;
      filter().dispatchEvent(new Event('change'));
      fixture.detectChanges();
    }

    it('lists the active choices of every student with the count, the filter and the export', () => {
      expect(el.querySelector('h1')?.textContent?.trim()).toBe('Gestion des demandes de mobilité');
      expect(headers()).toEqual([
        'Préférence',
        'Étudiant',
        'Pays',
        'Partenaire',
        'Type',
        'Programme',
        'Départ',
        'État',
        'Actions',
      ]);
      expect(cells(10)).toEqual([
        '1',
        'Tester Alice',
        'France',
        'Université de Lyon',
        'Académique',
        'Erasmus+',
        'Septembre 2026',
        'En attente',
        'Confirmer Refuser',
      ]);
      expect(el.querySelector('.count')?.textContent?.trim()).toBe('Nombre de demandes : 2');
      expect(el.querySelector('label[for=mobility-choices-filter]')?.textContent).toContain(
        'Afficher les demandes',
      );
      expect(Array.from(filter().options).map((o) => [o.value, o.textContent])).toEqual([
        ['active', 'Actives'],
        ['all', 'Toutes'],
        ['canceled', 'Annulées'],
        ['rejected', 'Rejetées'],
        ['passed', 'Années précédentes'],
      ]);
      expect(filter().value).toBe('active');
      const exportLink = el.querySelector<HTMLAnchorElement>('a.export')!;
      expect(exportLink.textContent).toContain('Exporter en csv');
      expect(exportLink.getAttribute('href')).toBe('/api/1.0/mobilityChoices/export?filter=active');
      expect(exportLink.getAttribute('target')).toBe('_blank');
      expect(exportLink.getAttribute('rel')).toBe('noopener');
      expect(el.querySelector('button.create')).toBeNull();
      expect(el.querySelector('button.cancel')).toBeNull();
    });

    it('offers no confirmation for a choice without partner, only the rejection', () => {
      expect(cells(12)).toEqual([
        '3',
        'Tester Alice',
        'À définir',
        'Pas de partenaire spécifié',
        'Stage',
        'Erasmus+',
        'Septembre 2026',
        'En attente',
        'Refuser',
      ]);
      expect(row(12).querySelector('button.confirm')).toBeNull();
    });

    it('loads the list and the count of the chosen filter, and points the export at it', () => {
      pick('rejected');
      expect(el.querySelector('[role=status].state')).not.toBeNull();
      professorList('rejected', [REJECTED]);
      expect(cells(14)).toEqual([
        '1',
        'Tester Bruno',
        'France',
        'Université de Lyon',
        'Académique',
        'Erasmus+',
        'Septembre 2026',
        'Refusée',
        '',
      ]);
      expect(el.querySelector('.count')?.textContent?.trim()).toBe('Nombre de demandes : 1');
      expect(el.querySelector('a.export')?.getAttribute('href')).toBe(
        '/api/1.0/mobilityChoices/export?filter=rejected',
      );
    });

    it('marks the choices that became mobilities as confirmed in the lists that may hold them', () => {
      pick('all');
      professorList('all', [LYON, BERLIN, CANCELED], [11]);
      expect(cells(11)[7]).toBe('Confirmée');
      expect(cells(11)[8]).toBe('');
      expect(cells(10)[7]).toBe('En attente');
      expect(cells(13)[7]).toContain('Annulée');

      pick('passed');
      professorList('passed', [BERLIN], []);
      expect(cells(11)[7]).toBe('En attente');
    });

    it('keeps only the answer of the last filter when the filter changes quickly', () => {
      pick('all');
      const stale = listRequest('all');
      pick('canceled');
      expect(stale.cancelled).toBeTrue();
      http.match((r) => r.params.get('filter') === 'all' || r.url === '/api/1.0/mobilities');
      professorList('canceled', [CANCELED]);
      expect(rows().length).toBe(1);
    });

    it('says when no choice matches the filter', () => {
      pick('passed');
      professorList('passed', [], []);
      expect(el.querySelector('table')).toBeNull();
      expect(el.textContent).toContain('Aucune demande de mobilité pour ce filtre.');
      expect(el.querySelector('.count')?.textContent?.trim()).toBe('Nombre de demandes : 0');
    });

    describe('confirm', () => {
      it('confirms the choice, says so and loads the list again', () => {
        click('button.confirm', row(10));
        const req = http.expectOne('/api/1.0/mobilityChoices/10/confirm');
        expect(req.request.method).toBe('PUT');
        expect(row(12).querySelector<HTMLButtonElement>('button.reject')!.disabled).toBeTrue();

        // a second confirmation while the first is in flight is ignored
        (fixture.componentInstance as unknown as { confirm(c: MobilityChoice): void }).confirm(
          LYON,
        );
        http.expectNone('/api/1.0/mobilityChoices/10/confirm');

        req.flush(null);
        fixture.detectChanges();
        expect(notice()).toBe('La demande de mobilité a été confirmée.');
        professorList('active', [NO_PARTNER]);
        expect(rows().length).toBe(1);
      });

      it('shows the refusal of the backend above the table', () => {
        click('button.confirm', row(10));
        http.expectOne('/api/1.0/mobilityChoices/10/confirm').flush(
          {
            code: 'MOBILITY_CHOICE_CLOSED',
            status: 409,
            detail: 'Cette demande est déjà clôturée.',
          },
          { status: 409, statusText: 'Conflict', headers: PROBLEM_HEADERS },
        );
        fixture.detectChanges();
        expect(el.querySelector('.action-problem')?.textContent).toContain(
          'Cette demande est déjà clôturée.',
        );
        expect(rows().length).toBe(2);

        pick('all');
        expect(el.querySelector('.action-problem')).toBeNull();
        professorList('all', [], []);
      });
    });

    describe('reject', () => {
      const panel = () => el.querySelector<HTMLElement>('.action-panel');
      const reason = () => el.querySelector<HTMLSelectElement>('#mobility-choices-reject-reason')!;
      const reasons = [
        { id: 1, reason: "L'étudiant part en mobilité pour ce semestre.", version: 0 },
        { id: 4, reason: 'Dossier incomplet', version: 0 },
      ];
      function choose(value: string): void {
        reason().value = value;
        reason().dispatchEvent(new Event('change'));
        fixture.detectChanges();
      }

      it('asks for one of the denial reasons, loaded once', () => {
        click('button.reject', row(10));
        expect(panel()?.querySelector('h2')?.textContent).toContain(
          'Refuser une demande de mobilité',
        );
        expect(panel()?.querySelector('.summary')?.textContent).toBe(
          'Préférence 1 · Tester Alice · Université de Lyon · Septembre 2026',
        );
        expect(reason().textContent).toContain('Chargement des raisons');
        const req = http.expectOne('/api/1.0/denialReasons');
        expect(req.request.method).toBe('GET');
        req.flush(reasons);
        fixture.detectChanges();
        expect(Array.from(reason().options).map((o) => o.text)).toEqual([
          'Choisissez une raison',
          "L'étudiant part en mobilité pour ce semestre.",
          'Dossier incomplet',
        ]);

        click('button.button:not([type=submit])', panel()!);
        click('button.reject', row(12));
        http.expectNone('/api/1.0/denialReasons');
        expect(panel()?.querySelector('.summary')?.textContent).toBe(
          'Préférence 3 · Tester Alice · Septembre 2026',
        );
      });

      it('requires a reason, then rejects with its id, says so and loads the list again', () => {
        click('button.reject', row(10));
        http.expectOne('/api/1.0/denialReasons').flush(reasons);
        fixture.detectChanges();
        click('button[type=submit]', panel()!);
        expect(el.querySelector('#mobility-choices-reason-error')?.textContent).toContain(
          'Ce champ est obligatoire.',
        );

        choose('4');
        expect(el.querySelector('#mobility-choices-reason-error')).toBeNull();
        click('button[type=submit]', panel()!);
        const req = http.expectOne('/api/1.0/mobilityChoices/10/reject?reason=4');
        expect(req.request.method).toBe('PUT');
        req.flush(null);
        fixture.detectChanges();
        expect(panel()).toBeNull();
        expect(notice()).toBe('La demande de mobilité a été refusée.');
        professorList('active', [NO_PARTNER]);
      });

      it('shows a failure to load the reasons in the panel', () => {
        click('button.reject', row(10));
        http
          .expectOne('/api/1.0/denialReasons')
          .flush(null, { status: 500, statusText: 'Server Error' });
        fixture.detectChanges();
        expect(panel()?.querySelector('[role=alert]')?.textContent).toContain(
          'Une erreur est survenue sur le serveur',
        );
      });

      it("shows the backend's refusal next to the reason", () => {
        click('button.reject', row(10));
        http.expectOne('/api/1.0/denialReasons').flush(reasons);
        fixture.detectChanges();
        choose('1');
        click('button[type=submit]', panel()!);
        http.expectOne('/api/1.0/mobilityChoices/10/reject?reason=1').flush(
          {
            code: 'VALIDATION_FAILED',
            status: 400,
            detail: 'Invalide.',
            errors: [{ field: 'reason', code: 'Positive', message: 'Raison inconnue.' }],
          },
          { status: 400, statusText: 'Bad Request', headers: PROBLEM_HEADERS },
        );
        fixture.detectChanges();
        expect(el.querySelector('#mobility-choices-reason-error')?.textContent).toBe(
          'Raison inconnue.',
        );
        expect(reason().getAttribute('aria-invalid')).toBe('true');
      });

      it('closes the panel and the messages when the filter changes', () => {
        click('button.reject', row(10));
        http.expectOne('/api/1.0/denialReasons').flush(reasons);
        pick('all');
        expect(panel()).toBeNull();
        professorList('all', [], []);
      });
    });
  });
});

describe('mobility choice labels', () => {
  it('writes the departure like the legacy UI', () => {
    expect(departureLabel(1, 2026)).toBe('Septembre 2026');
    expect(departureLabel(2, 2027)).toBe('Février 2027');
  });

  it('names the mobility types', () => {
    expect(mobilityTypeLabel('SMS')).toBe('Académique');
    expect(mobilityTypeLabel('SMP')).toBe('Stage');
  });

  it('tells the state of a choice, a rejection first', () => {
    expect(choiceState(LYON)).toBe('pending');
    expect(choiceState(LYON, new Set([10]))).toBe('confirmed');
    expect(choiceState(CANCELED, new Set([13]))).toBe('canceled');
    expect(choiceState({ ...CANCELED, denialReason: { id: 1 } })).toBe('rejected');
  });
});
