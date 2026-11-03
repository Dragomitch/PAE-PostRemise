import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { Mobility, MobilityDocument, User } from '../core/models';
import { SessionService } from '../core/session.service';
import { MobilityDetail } from './mobility-detail';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };
const PROFESSOR: User = { id: 1, username: 'prof', firstName: 'Paula', role: 'Professor' };
const STUDENT: User = { id: 2, username: 'alice', firstName: 'Alice', role: 'Student' };
const URL = '/api/1.0/mobilities/1';
const REASONS = [
  { id: 1, reason: 'Dossier incomplet' },
  { id: 2, reason: 'Désistement' },
];

function doc(id: number, category: 'D' | 'R', filledIn: boolean): MobilityDocument {
  return { id, name: `Document ${id}`, category, filledIn };
}

/** A mobility in "A payer": departure documents filled in, return ones not, nothing encoded. */
function mobility(patch: Partial<Mobility> = {}): Mobility {
  return {
    id: 1,
    version: 5,
    mobilityType: 'SMS',
    academicYear: 2026,
    term: 1,
    state: 'A payer',
    programme: { id: 1, programmeName: 'Erasmus+', externalSoftName: 'Mobility Tool' },
    country: { countryCode: 'FR', name: 'France' },
    partner: { id: 1, fullName: 'Universite de Lyon' },
    nominatedStudent: {
      id: 2,
      firstName: 'Alice',
      lastName: 'Tester',
      email: 'alice@example.test',
      phoneNumber: '+32470000002',
      option: { code: 'BIN', name: 'Bachelier en informatique de gestion' },
      iban: 'BE68539007547034',
      bic: 'GKCCBEBB',
      bankName: 'Belfius',
      cardHolder: 'Alice Tester',
    },
    professorInCharge: { id: 1, firstName: 'Paula', lastName: 'Prof' },
    documents: [doc(1, 'D', true), doc(2, 'D', true), doc(14, 'R', false), doc(15, 'R', false)],
    encodedInProEco: false,
    encodedInSecondSoftware: false,
    firstPaymentRequestDate: null,
    secondPaymentRequestDate: null,
    ...patch,
  };
}

const COMPLETED = mobility({
  state: 'Terminée',
  version: 14,
  documents: [doc(1, 'D', true), doc(14, 'R', true)],
  encodedInProEco: true,
  encodedInSecondSoftware: true,
  firstPaymentRequestDate: '2026-11-03T02:43:17.597942',
  secondPaymentRequestDate: '2026-11-04T09:00:00',
});

describe('MobilityDetail', () => {
  let fixture: ComponentFixture<MobilityDetail>;
  let http: HttpTestingController;
  let router: Router;
  let el: HTMLElement;

  /** Signs the user in, opens the detail of mobility 1 and returns the pending `GET`. */
  function open(user: User = PROFESSOR): TestRequest {
    TestBed.configureTestingModule({
      imports: [MobilityDetail],
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
    fixture = TestBed.createComponent(MobilityDetail);
    fixture.componentRef.setInput('id', '1');
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    return http.expectOne(URL);
  }

  function show(m: Mobility, user: User = PROFESSOR): void {
    open(user).flush(m);
    fixture.detectChanges();
  }

  const text = (selector: string) =>
    el.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim();
  const click = (selector: string) => {
    el.querySelector<HTMLElement>(selector)!.click();
    fixture.detectChanges();
  };
  const submitPanel = () => {
    el.querySelector('.action-panel form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };
  const put = (url: string) => {
    const req = http.expectOne((r) => r.urlWithParams === url);
    expect(req.request.method).toBe('PUT');
    return req;
  };
  const problem = (status: number, code: string, detail: string, errors?: unknown[]) =>
    [
      { code, status, detail, errors },
      { status, statusText: 'Error', headers: PROBLEM_HEADERS },
    ] as const;

  afterEach(() => http.verify());

  it('shows the loading state until the API answers', () => {
    const req = open();
    expect(text('p.state')).toBe('Chargement de la mobilité…');
    req.flush(COMPLETED);
    fixture.detectChanges();
    expect(el.querySelector('p.state')).toBeNull();
  });

  it('shows the completed mobility: general information, student, documents, payment dates', () => {
    show(COMPLETED);
    expect(text('h1')).toBe('Alice Tester à Universite de Lyon');
    expect(text('.back a')).toBe('Retour aux mobilités');
    expect(
      Array.from(el.querySelectorAll('.general dd')).map((d) => d.textContent?.trim()),
    ).toEqual([
      'Académique',
      'Universite de Lyon',
      'France',
      'Erasmus+',
      'Septembre 2026',
      'Terminée',
      'Paula Prof',
    ]);
    expect(el.querySelector('.mobility-state')?.getAttribute('data-state')).toBe('Terminée');
    expect(el.querySelector('.mobility-state .badge')?.className).toBe('badge badge-closed');
    expect(text('.student-name')).toBe('Alice Tester');
    expect(el.querySelector('.student a')?.getAttribute('href')).toBe('mailto:alice@example.test');
    expect(text('.iban')).toBe('BE68539007547034');
    expect(text('.departure-documents li')).toBe('Document 1 Complété');
    expect(text('.return-documents li')).toBe('Document 14 Complété');
    expect(text('.pro-eco')).toBe('ProEco Encodé');
    expect(text('.second-software')).toBe('Mobility Tool Encodé');
    const first = el.querySelector('.first-payment time')!;
    expect(first.getAttribute('datetime')).toBe('2026-11-03');
    expect(text('.first-payment')).toBe('Effectué le 3 nov. 2026');
    expect(el.querySelector('.second-payment time')?.getAttribute('datetime')).toBe('2026-11-04');
    expect(el.querySelector('.bank-details')).toBeNull();
    expect(el.querySelector('.cancellation')).toBeNull();
    expect(el.querySelectorAll('button').length).toBe(0);
  });

  it('links the CSV exports of the documents for a professor', () => {
    show(COMPLETED);
    const exports = Array.from(el.querySelectorAll<HTMLAnchorElement>('a.export'));
    expect(exports.map((a) => a.getAttribute('href'))).toEqual([
      '/api/1.0/mobilities/1/export?filter=D',
      '/api/1.0/mobilities/1/export?filter=DF',
      '/api/1.0/mobilities/1/export?filter=R',
      '/api/1.0/mobilities/1/export?filter=RF',
      '/api/1.0/mobilities/1/export',
    ]);
    expect(exports[0].getAttribute('download')).toBe('mobilite_Alice_Tester.csv');
  });

  it('offers the first payment in "A payer" and confirms it after the panel, re-reading the mobility', () => {
    show(mobility());
    expect(text('.second-payment')).toBe('Pas encore dû');
    click('.first-payment button.confirm-payment');
    expect(text('.action-panel h2')).toBe('Confirmer le premier paiement');
    expect(text('.action-panel .question')).toBe(
      'Confirmer le paiement au départ de Alice Tester ? Cette action est définitive.',
    );
    expect(document.activeElement).toBe(el.querySelector('.action-panel button.confirm'));
    submitPanel();
    expect(
      el.querySelector<HTMLButtonElement>('.action-panel button.confirm')!.disabled,
    ).toBeTrue();
    put('/api/1.0/mobilities/1/confirmPayment?version=5').flush(null);
    http
      .expectOne(URL)
      .flush(
        mobility({ state: 'En cours', version: 6, firstPaymentRequestDate: '2026-11-05T10:00:00' }),
      );
    fixture.detectChanges();
    expect(el.querySelector('.action-panel')).toBeNull();
    expect(text('.notice')).toBe('Le paiement est confirmé.');
    expect(text('.mobility-state')).toBe('En cours');
    expect(text('.first-payment')).toBe('Effectué le 5 nov. 2026');
  });

  it('offers the second payment in "Solde à payer"', () => {
    show(mobility({ state: 'Solde à payer', firstPaymentRequestDate: '2026-11-03T02:00:00' }));
    expect(text('.second-payment button')).toBe('Confirmer le deuxième paiement');
    click('.second-payment button');
    expect(text('.action-panel h2')).toBe('Confirmer le deuxième paiement');
    expect(text('.action-panel .question')).toContain("paiement à l'arrivée de Alice Tester");
    click('.action-panel button.close');
    expect(el.querySelector('.action-panel')).toBeNull();
  });

  it('expects no payment in the other states', () => {
    show(mobility({ state: 'En cours', firstPaymentRequestDate: '2026-11-03T02:00:00' }));
    expect(text('.second-payment')).toBe('Pas encore dû');
    expect(el.querySelector('.confirm-payment')).toBeNull();
  });

  it('withholds the payments while the bank details are incomplete', () => {
    const m = mobility();
    show({ ...m, nominatedStudent: { ...m.nominatedStudent, bic: undefined } });
    expect(text('.bank-details')).toContain(
      "Les données bancaires de l'étudiant sont incomplètes.",
    );
    expect(text('.first-payment')).toBe('À effectuer');
  });

  it('confirms a document with its id and the version', () => {
    show(mobility());
    expect(text('.departure-documents li')).toBe('Document 1 Complété');
    click('.return-documents li[data-document="14"] button.confirm-document');
    expect(text('.action-panel h2')).toBe('Confirmer un document');
    expect(text('.action-panel .question')).toBe(
      'Le document « Document 14 » de Alice Tester est-il complété ? Cette action est définitive.',
    );
    submitPanel();
    put('/api/1.0/mobilities/1/confirmDocument?document=14&version=5').flush(mobility());
    http.expectOne(URL).flush(mobility({ version: 6 }));
    fixture.detectChanges();
    expect(text('.notice')).toBe('Le document « Document 14 » est complété.');
  });

  it('confirms the two software encodings', () => {
    show(mobility());
    click('.pro-eco button');
    expect(text('.action-panel h2')).toBe('Confirmer un encodage');
    expect(text('.action-panel .question')).toBe(
      'La mobilité de Alice Tester est-elle encodée dans ProEco ?',
    );
    submitPanel();
    put('/api/1.0/mobilities/1/confirmProEcoEncoding?version=5').flush(null);
    http.expectOne(URL).flush(mobility({ version: 6, encodedInProEco: true }));
    fixture.detectChanges();
    expect(text('.notice')).toBe("L'encodage dans ProEco est confirmé.");
    expect(text('.pro-eco')).toBe('ProEco Encodé');

    click('.second-software button');
    expect(text('.action-panel .question')).toBe(
      'La mobilité de Alice Tester est-elle encodée dans Mobility Tool ?',
    );
    expect(text('.notice')).toBeUndefined();
    submitPanel();
    put('/api/1.0/mobilities/1/confirmSecondSoftwareEncoding?version=6').flush(null);
    http
      .expectOne(URL)
      .flush(mobility({ version: 7, encodedInProEco: true, encodedInSecondSoftware: true }));
    fixture.detectChanges();
    expect(text('.notice')).toBe("L'encodage dans Mobility Tool est confirmé.");
  });

  it('names the second software generically when the programme has none', () => {
    show(mobility({ programme: { id: 3, programmeName: 'FAME' } }));
    expect(text('.second-software')).toBe('Logiciel du programme Confirmer');
  });

  it('shows a stale version (409) in the panel with a button that reloads the mobility', () => {
    show(mobility());
    click('.pro-eco button');
    submitPanel();
    put('/api/1.0/mobilities/1/confirmProEcoEncoding?version=5').flush(
      ...problem(409, 'CONCURRENT_MODIFICATION', 'La mobilité a été modifiée entre-temps.'),
    );
    fixture.detectChanges();
    expect(text('.action-panel [role="alert"] p')).toBe('La mobilité a été modifiée entre-temps.');
    click('.action-panel button.reload');
    http.expectOne(URL).flush(mobility({ version: 6, encodedInProEco: true }));
    fixture.detectChanges();
    expect(el.querySelector('.action-panel')).toBeNull();
    expect(text('.pro-eco')).toBe('ProEco Encodé');
  });

  it('lists the field errors of another refusal, without the reload button', () => {
    show(mobility());
    click('.first-payment button');
    submitPanel();
    put('/api/1.0/mobilities/1/confirmPayment?version=5').flush(
      ...problem(400, 'VALIDATION_FAILED', 'Requête invalide.', [
        { field: 'version', message: 'La version est obligatoire.' },
      ]),
    );
    fixture.detectChanges();
    expect(text('.action-panel [role="alert"] li')).toBe('La version est obligatoire.');
    expect(el.querySelector('.action-panel button.reload')).toBeNull();
  });

  it('cancels with a denial reason chosen in the panel', () => {
    show(mobility());
    click('button.cancel-mobility');
    expect(text('.action-panel h2')).toBe('Annuler la mobilité');
    expect(text('.action-panel .question')).toBe(
      'Annuler la mobilité de Alice Tester ? Cette action est définitive.',
    );
    expect(text('#mobility-cancel-reason')).toBe('Chargement des raisons…');
    http.expectOne('/api/1.0/denialReasons').flush(REASONS);
    fixture.detectChanges();
    const select = el.querySelector<HTMLSelectElement>('#mobility-cancel-reason')!;
    expect(document.activeElement).toBe(select);
    expect(select.options.length).toBe(3);

    submitPanel();
    expect(text('#mobility-cancel-reason-error')).toBe('Ce champ est obligatoire.');
    expect(select.getAttribute('aria-invalid')).toBe('true');

    // closing and opening again keeps the reasons already read
    click('.action-panel button.close');
    click('button.cancel-mobility');
    const again = el.querySelector<HTMLSelectElement>('#mobility-cancel-reason')!;
    expect(el.querySelector('#mobility-cancel-reason-error')).toBeNull();
    again.value = again.options[2].value;
    again.dispatchEvent(new Event('change'));
    submitPanel();
    put('/api/1.0/mobilities/1/cancel?version=5&denialReason=2').flush(mobility());
    http.expectOne(URL).flush(
      mobility({
        state: 'Annulée',
        version: 6,
        stateBeforeCancellation: 'A payer',
        denialReason: { id: 2, reason: 'Désistement' },
      }),
    );
    fixture.detectChanges();
    expect(text('.notice')).toBe('La mobilité est annulée.');
    expect(text('.cancellation-reason')).toBe('Désistement');
    expect(text('.state-before-cancellation')).toBe('A payer');
    expect(el.querySelector('button.cancel-mobility')).toBeNull();
    expect(text('.return-documents li')).toBe('Document 14 À compléter');
    expect(text('.pro-eco')).toBe('ProEco Non encodé');
  });

  it('shows the server error of the denial reason next to the list', () => {
    show(mobility());
    click('button.cancel-mobility');
    http.expectOne('/api/1.0/denialReasons').flush(REASONS);
    fixture.detectChanges();
    const select = el.querySelector<HTMLSelectElement>('#mobility-cancel-reason')!;
    select.value = select.options[1].value;
    select.dispatchEvent(new Event('change'));
    submitPanel();
    put('/api/1.0/mobilities/1/cancel?version=5&denialReason=1').flush(
      ...problem(400, 'VALIDATION_FAILED', 'Requête invalide.', [
        { field: 'denialReason', message: 'Raison inconnue.' },
      ]),
    );
    fixture.detectChanges();
    expect(text('#mobility-cancel-reason-error')).toBe('Raison inconnue.');
    expect(el.querySelector('.action-panel [role="alert"] ul')).toBeNull();
  });

  it('shows a failure to read the denial reasons in the panel', () => {
    show(mobility());
    click('button.cancel-mobility');
    http
      .expectOne('/api/1.0/denialReasons')
      .flush(...problem(500, 'INTERNAL_ERROR', 'Le serveur a échoué.'));
    fixture.detectChanges();
    expect(text('.action-panel [role="alert"] p')).toBe('Le serveur a échoué.');
  });

  it("shows the student's own cancellation reason, or nothing when there is none", () => {
    show(mobility({ state: 'Annulée', cancellationReason: "J'ai changé d'avis" }));
    expect(text('.cancellation-reason')).toBe("J'ai changé d'avis");
    expect(el.querySelector('.state-before-cancellation')).toBeNull();
  });

  it('shows an empty reason for a cancellation without any', () => {
    show(mobility({ state: 'Annulée' }));
    expect(text('.cancellation-reason')).toBe('');
  });

  it('leaves out the professor in charge when the API gives no name', () => {
    show(mobility({ professorInCharge: { id: 1 } }));
    expect(el.querySelectorAll('.general dt').length).toBe(6);
  });

  it('copes with a mobility without professor, documents or partner', () => {
    show(mobility({ professorInCharge: null, documents: undefined, partner: null, country: null }));
    expect(text('h1')).toBe('Alice Tester à');
    expect(el.querySelectorAll('.documents p.muted').length).toBe(2);
    expect(text('.documents p.muted')).toBe('Aucun document.');
  });

  it("shows a student's mobility without any button, encoding or export", () => {
    show(mobility(), STUDENT);
    expect(text('.back a')).toBe('Retour à mes mobilités');
    expect(el.querySelector('.encodings')).toBeNull();
    expect(el.querySelector('a.export')).toBeNull();
    expect(el.querySelector('.cancellation')).toBeNull();
    expect(el.querySelectorAll('button').length).toBe(0);
    expect(text('.return-documents li')).toBe('Document 14 À compléter');
    expect(text('.first-payment')).toBe('À effectuer');
  });

  it('ignores a confirmation without a pending action or while one is running', () => {
    show(mobility());
    const component = fixture.componentInstance;
    component['confirm']();
    click('.first-payment button');
    submitPanel();
    submitPanel();
    put('/api/1.0/mobilities/1/confirmPayment?version=5').flush(null);
    http.expectOne(URL).flush(mobility());
  });

  it('shows a failure to read the mobility with a retry button', () => {
    open().flush(...problem(404, 'RESOURCE_NOT_FOUND', "La mobilité n'existe pas."));
    fixture.detectChanges();
    expect(text('h1')).toBe('Mobilité');
    expect(text('[role="alert"] p')).toBe("La mobilité n'existe pas.");
    click('button.retry');
    http.expectOne(URL).flush(COMPLETED);
    fixture.detectChanges();
    expect(text('h1')).toBe('Alice Tester à Universite de Lyon');
  });

  it('goes back to the sign-in page when the session expired', () => {
    const req = open();
    const navigate = spyOn(router, 'navigate').and.resolveTo(true);
    req.flush(...problem(401, 'UNAUTHENTICATED', 'Connectez-vous.'));
    expect(navigate).toHaveBeenCalledWith(['/signin']);
    expect(TestBed.inject(SessionService).user()).toBeNull();
  });

  it('goes back to the sign-in page when the session expired during a change', () => {
    show(mobility());
    const navigate = spyOn(router, 'navigate').and.resolveTo(true);
    click('.pro-eco button');
    submitPanel();
    put('/api/1.0/mobilities/1/confirmProEcoEncoding?version=5').flush(
      ...problem(401, 'UNAUTHENTICATED', 'Connectez-vous.'),
    );
    expect(navigate).toHaveBeenCalledWith(['/signin']);
  });
});
