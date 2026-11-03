import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, TestRequest, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { NominatedStudent, ProgrammeCountry, User } from '../core/models';
import { SessionService } from '../core/session.service';
import { PersonalData, compact } from './personal-data';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };
const STUDENT: User = {
  id: 2,
  username: 'alice',
  firstName: 'Alice',
  lastName: 'Tester',
  email: 'alice@example.test',
  role: 'Student',
  option: { code: 'BIN' },
  version: 1,
};
const COUNTRIES: ProgrammeCountry[] = [
  { countryCode: 'FR', name: 'France', programme: { id: 1 } },
  { countryCode: 'DE', name: 'Allemagne', programme: { id: 1 } },
  { countryCode: 'BE', name: 'Belgique', programme: { id: 2 } },
];
const RECORD: NominatedStudent = {
  id: 2,
  username: 'alice',
  firstName: 'Alice',
  lastName: 'Tester',
  email: 'alice@example.test',
  role: 'Student',
  option: { code: 'BIN', name: 'Bachelier en informatique de gestion' },
  registrationDate: '2026-09-01T10:00:00.123456',
  title: 'Ms',
  birthdate: '2001-04-12',
  nationality: { countryCode: 'BE', name: 'Belgique' },
  address: {
    id: 7,
    version: 1,
    street: 'Rue de la Loi',
    number: '16',
    city: 'Bruxelles',
    postalCode: '1000',
    region: '',
    country: { countryCode: 'FR', name: 'France' },
  },
  phoneNumber: '+32470000001',
  gender: 'F',
  nbrPassedYears: 1,
  iban: 'BE68539007547034',
  bankName: 'Belfius',
  bic: 'GKCCBEBB',
  cardHolder: 'Alice Tester',
  version: 3,
};

describe('PersonalData', () => {
  let fixture: ComponentFixture<PersonalData>;
  let http: HttpTestingController;
  let router: Router;
  let el: HTMLElement;

  /** Signs `user` in and opens the screen, which asks for the countries and the record. */
  function open(user: User = STUDENT): void {
    TestBed.configureTestingModule({
      imports: [PersonalData],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    TestBed.inject(SessionService).signIn({ username: user.username, password: 'secret' }).subscribe();
    http.expectOne('/api/1.0/session').flush(user);
    fixture = TestBed.createComponent(PersonalData);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  }

  function recordRequest(): TestRequest {
    return http.expectOne('/api/1.0/nominatedStudents/2');
  }

  /** Answers the loading requests with the countries and the record (404 when `null`). */
  function load(record: NominatedStudent | null): void {
    http.expectOne('/api/1.0/countries').flush(COUNTRIES);
    if (record) {
      recordRequest().flush(record);
    } else {
      recordRequest().flush(
        { code: 'RESOURCE_NOT_FOUND', status: 404, detail: 'La ressource demandée n’existe pas.' },
        { status: 404, statusText: 'Not Found', headers: PROBLEM_HEADERS },
      );
    }
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  const control = (id: string) => el.querySelector<HTMLInputElement | HTMLSelectElement>(`#personal-data-${id}`)!;
  const value = (id: string) => control(id).value;
  const options = (id: string) => Array.from((control(id) as HTMLSelectElement).options).map((o) => o.textContent?.trim());
  const error = (id: string) => el.querySelector(`#personal-data-${id}-error`)?.textContent?.trim();
  const alert = () => el.querySelector('[role=alert]');
  const submitButton = () => el.querySelector<HTMLButtonElement>('button[type=submit]')!;

  function set(id: string, text: string): void {
    const input = control(id);
    input.value = text;
    input.dispatchEvent(new Event(input instanceof HTMLSelectElement ? 'change' : 'input'));
    fixture.detectChanges();
  }

  function submit(): void {
    submitButton().click();
    fixture.detectChanges();
  }

  /** Fills every required field a new record lacks (the names, the e-mail and the countries are prefilled). */
  function fillNew(): void {
    set('title', 'M');
    set('gender', 'F');
    set('birthdate', '2002-03-04');
    set('street', ' Rue Haute ');
    set('number', '5A');
    set('city', 'Liège');
    set('postalCode', '4000');
    set('phoneNumber', '0032486372854');
    set('iban', 'be68 5390 0754 7034');
    set('bankName', 'ING');
    set('bic', 'bbru be bb');
    set('nbrPassedYears', '2');
  }

  describe('loading', () => {
    it('asks for the countries and for the record of the signed-in student before showing the form', () => {
      open();
      expect(el.querySelector('h1')?.textContent).toContain('Mes données personnelles');
      expect(el.querySelector('[role=status]')?.textContent).toContain('Chargement de vos données personnelles');
      expect(el.querySelector('form')).toBeNull();
      load(RECORD);
      expect(el.querySelector('form')).not.toBeNull();
      expect(el.textContent).not.toContain('Chargement');
    });

    it('fills the form with the stored record', () => {
      open();
      load(RECORD);
      expect(value('title')).toBe('Ms');
      expect(value('lastName')).toBe('Tester');
      expect(value('firstName')).toBe('Alice');
      expect(value('gender')).toBe('F');
      expect(value('birthdate')).toBe('2001-04-12');
      expect(control('birthdate').getAttribute('type')).toBe('date');
      expect(value('nationality')).toBe('BE');
      expect(value('street')).toBe('Rue de la Loi');
      expect(value('number')).toBe('16');
      expect(value('region')).toBe('');
      expect(value('city')).toBe('Bruxelles');
      expect(value('postalCode')).toBe('1000');
      expect(value('country')).toBe('FR');
      expect(value('phoneNumber')).toBe('+32470000001');
      expect(value('email')).toBe('alice@example.test');
      expect(value('iban')).toBe('BE68539007547034');
      expect(value('bankName')).toBe('Belfius');
      expect(value('bic')).toBe('GKCCBEBB');
      expect(value('cardHolder')).toBe('Alice Tester');
      expect(value('nbrPassedYears')).toBe('1');
      expect(el.querySelector('.intro')).toBeNull();
    });

    it('reads a missing region and card holder as empty fields', () => {
      open();
      load({ ...RECORD, address: { ...RECORD.address, region: null }, cardHolder: null });
      expect(value('region')).toBe('');
      expect(value('cardHolder')).toBe('');
    });

    it('starts from the account when the student never saved their data (404), Belgium preselected', () => {
      open();
      load(null);
      expect(alert()).toBeNull();
      expect(el.querySelector('.intro')?.textContent).toContain('pas encore encodé vos données personnelles');
      expect(value('lastName')).toBe('Tester');
      expect(value('firstName')).toBe('Alice');
      expect(value('email')).toBe('alice@example.test');
      expect(value('nationality')).toBe('BE');
      expect(value('country')).toBe('BE');
      expect(value('title')).toBe('');
      expect(value('gender')).toBe('');
      expect(value('birthdate')).toBe('');
      expect(value('nbrPassedYears')).toBe('');
    });

    it('starts with empty names when the account has none', () => {
      open({ id: 2, username: 'alice', role: 'Student' });
      load(null);
      expect(value('lastName')).toBe('');
      expect(value('firstName')).toBe('');
      expect(value('email')).toBe('');
    });

    it('shows a failure with a retry button', () => {
      open();
      http.expectOne('/api/1.0/countries').flush(null, { status: 503, statusText: 'Service Unavailable' });
      // the first failure cancels the other request
      expect(recordRequest().cancelled).toBeTrue();
      fixture.detectChanges();
      expect(alert()?.textContent).toContain('momentanément indisponible');
      expect(el.querySelector('form')).toBeNull();

      el.querySelector<HTMLButtonElement>('button.retry')!.click();
      fixture.detectChanges();
      expect(alert()).toBeNull();
      load(RECORD);
      expect(el.querySelector('form')).not.toBeNull();
    });

    it('reports a refusal of the record other than 404', () => {
      open();
      recordRequest().flush(
        { code: 'ACCESS_DENIED', status: 403, detail: 'Vous n’avez pas les droits nécessaires.' },
        { status: 403, statusText: 'Forbidden', headers: PROBLEM_HEADERS },
      );
      expect(http.expectOne('/api/1.0/countries').cancelled).toBeTrue();
      fixture.detectChanges();
      expect(alert()?.textContent).toContain('Vous n’avez pas les droits nécessaires.');
      expect(el.querySelector('form')).toBeNull();
    });

    it('goes back to the sign-in page when the session expired', () => {
      open();
      const navigate = spyOn(router, 'navigate').and.resolveTo(true);
      recordRequest().flush({ code: 'UNAUTHENTICATED', status: 401 }, { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS });
      http.expectOne('/api/1.0/countries');
      fixture.detectChanges();
      expect(navigate).toHaveBeenCalledOnceWith(['/signin']);
      expect(TestBed.inject(SessionService).user()).toBeNull();
      expect(alert()).toBeNull();
    });
  });

  describe('fields', () => {
    beforeEach(() => {
      open();
      load(RECORD);
    });

    it('has the sections, fields and labels of the legacy form', () => {
      expect(Array.from(el.querySelectorAll('legend')).map((l) => l.textContent?.trim())).toEqual([
        'Informations générales',
        'Adresse',
        'Contact',
        'Données bancaires',
        'Données académiques',
      ]);
      const labels = Array.from(el.querySelectorAll('label')).map((l) => [l.getAttribute('for')?.replace('personal-data-', ''), l.textContent?.trim()]);
      expect(labels).toEqual([
        ['title', 'Titre'],
        ['lastName', 'Nom'],
        ['firstName', 'Prénom'],
        ['gender', 'Sexe'],
        ['birthdate', 'Date de naissance'],
        ['nationality', 'Nationalité'],
        ['street', 'Rue'],
        ['number', 'Numéro'],
        ['region', 'Région'],
        ['city', 'Ville'],
        ['postalCode', 'Code postal'],
        ['country', 'Pays'],
        ['phoneNumber', 'Téléphone'],
        ['email', 'E-mail'],
        ['iban', 'N° de compte (IBAN)'],
        ['bankName', 'Banque'],
        ['bic', 'Code BIC'],
        ['cardHolder', 'Titulaire du compte'],
        ['nbrPassedYears', 'Année(s) réussie(s) dans l’enseignement supérieur'],
      ]);
      expect(options('title')).toEqual(['Choisissez…', 'Madame', 'Monsieur', 'Mademoiselle']);
      expect(options('gender')).toEqual(['Choisissez…', 'Homme', 'Femme', 'Autre']);
      // the countries, by name
      expect(options('nationality')).toEqual(['Choisissez un pays', 'Allemagne', 'Belgique', 'France']);
      expect(options('country')).toEqual(['Choisissez un pays', 'Allemagne', 'Belgique', 'France']);
      expect(control('region').getAttribute('placeholder')).toBe('Optionnel');
      expect(control('phoneNumber').getAttribute('placeholder')).toBe('0032486372854');
      expect(control('lastName').getAttribute('maxlength')).toBe('35');
      expect(control('phoneNumber').getAttribute('maxlength')).toBe('15');
      expect(submitButton().textContent?.trim()).toBe('Sauvegarder');
    });

    it('explains that an empty card holder means the student', () => {
      expect(control('cardHolder').getAttribute('aria-describedby')).toBe('personal-data-cardHolder-hint');
      expect(el.querySelector('#personal-data-cardHolder-hint')?.textContent).toContain('Laissez vide si le compte est à votre nom.');
      set('cardHolder', 'x'.repeat(36));
      submit();
      expect(error('cardHolder')).toBe('Valeur invalide.');
      expect(control('cardHolder').getAttribute('aria-describedby')).toBe('personal-data-cardHolder-error');
      expect(el.querySelector('#personal-data-cardHolder-hint')).toBeNull();
    });

    it('checks the required fields, the e-mail and the number of years before sending anything', () => {
      for (const id of ['lastName', 'street', 'phoneNumber', 'iban', 'bic']) {
        set(id, '');
      }
      set('email', 'not-an-address');
      set('nbrPassedYears', '0');
      submit();
      expect(error('lastName')).toBe('Ce champ est obligatoire.');
      expect(error('street')).toBe('Ce champ est obligatoire.');
      expect(error('phoneNumber')).toBe('Ce champ est obligatoire.');
      expect(error('iban')).toBe('Ce champ est obligatoire.');
      expect(error('bic')).toBe('Ce champ est obligatoire.');
      expect(error('email')).toBe('Saisissez une adresse e-mail valide.');
      expect(error('nbrPassedYears')).toBe('Valeur invalide.');
      expect(error('region')).toBeUndefined();
      expect(error('firstName')).toBeUndefined();
      expect(control('street').getAttribute('aria-invalid')).toBe('true');
      expect(control('street').getAttribute('aria-describedby')).toBe('personal-data-street-error');
      expect(control('city').getAttribute('aria-invalid')).toBe('false');
      expect(control('city').getAttribute('aria-describedby')).toBeNull();
    });
  });

  describe('first save', () => {
    beforeEach(() => {
      open();
      load(null);
    });

    it('requires every field but the region and the card holder', () => {
      submit();
      for (const id of ['title', 'gender', 'birthdate', 'street', 'number', 'city', 'postalCode', 'phoneNumber', 'iban', 'bankName', 'bic', 'nbrPassedYears']) {
        expect(error(id)).withContext(id).toBe('Ce champ est obligatoire.');
      }
      expect(error('region')).toBeUndefined();
      expect(error('cardHolder')).toBeUndefined();
      expect(error('lastName')).toBeUndefined();
    });

    it('creates the record with POST, from the account and the form, then updates it with PUT', () => {
      fillNew();
      submit();
      const req = http.expectOne('/api/1.0/nominatedStudents');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        id: 2,
        username: 'alice',
        option: { code: 'BIN' },
        title: 'M',
        lastName: 'Tester',
        firstName: 'Alice',
        gender: 'F',
        birthdate: '2002-03-04',
        nationality: { countryCode: 'BE' },
        address: { street: 'Rue Haute', number: '5A', region: '', city: 'Liège', postalCode: '4000', country: { countryCode: 'BE' } },
        phoneNumber: '0032486372854',
        email: 'alice@example.test',
        iban: 'BE68539007547034',
        bankName: 'ING',
        bic: 'BBRUBEBB',
        cardHolder: '',
        nbrPassedYears: 2,
      });
      expect(submitButton().disabled).toBeTrue();
      expect(submitButton().getAttribute('aria-busy')).toBe('true');

      // a second submit while the request is in flight is ignored
      (fixture.componentInstance as unknown as { submit(): void }).submit();
      http.expectNone('/api/1.0/nominatedStudents');

      const saved: NominatedStudent = { ...req.request.body, cardHolder: 'Alice Tester', version: 1 };
      req.flush(saved);
      fixture.detectChanges();
      expect(el.querySelector('[role=status]')?.textContent).toContain('Les modifications ont bien été enregistrées.');
      expect(el.querySelector('.intro')).toBeNull();
      expect(value('cardHolder')).toBe('Alice Tester');
      expect(submitButton().disabled).toBeFalse();

      set('phoneNumber', '+32470000009');
      expect(el.querySelector('[role=status]')?.textContent).toContain('Les modifications ont bien été enregistrées.');
      submit();
      expect(el.querySelector('[role=status]')?.textContent?.trim()).toBe('');
      const update = http.expectOne('/api/1.0/nominatedStudents/2');
      expect(update.request.method).toBe('PUT');
      expect(update.request.body).toEqual(jasmine.objectContaining({ phoneNumber: '+32470000009', version: 1, cardHolder: 'Alice Tester' }));
      update.flush({ ...update.request.body, version: 2 });
    });

    it('shows the business refusals of the API in the alert', () => {
      fillNew();
      submit();
      http.expectOne('/api/1.0/nominatedStudents').flush(
        { code: 'UNKNOWN_COUNTRY', status: 422, detail: 'Pays inconnu : BE.' },
        { status: 422, statusText: 'Unprocessable Entity', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(alert()?.querySelector('p')?.textContent).toBe('Pays inconnu : BE.');
      expect(alert()?.querySelector('ul')).toBeNull();
      expect(alert()?.querySelector('button.reload')).toBeNull();
      expect(submitButton().disabled).toBeFalse();
    });
  });

  describe('update', () => {
    beforeEach(() => {
      open();
      load(RECORD);
    });

    it('sends the stored record with the changes of the form and its version, with PUT', () => {
      set('phoneNumber', ' +32470000002 ');
      set('region', ' Brabant ');
      set('nationality', 'FR');
      submit();
      const req = http.expectOne('/api/1.0/nominatedStudents/2');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({
        ...RECORD,
        nationality: { countryCode: 'FR' },
        address: { ...RECORD.address, region: 'Brabant', country: { countryCode: 'FR' } },
        phoneNumber: '+32470000002',
      });
      req.flush({ ...req.request.body, version: 4 });
      fixture.detectChanges();
      expect(el.querySelector('.notice')?.textContent).toContain('Les modifications ont bien été enregistrées.');
      expect(value('phoneNumber')).toBe('+32470000002');

      // the next save carries the new version
      submit();
      const next = http.expectOne('/api/1.0/nominatedStudents/2');
      expect(next.request.body.version).toBe(4);
      next.flush(next.request.body);
    });

    it("shows the server's field errors next to the inputs and the others in the alert", () => {
      submit();
      http.expectOne('/api/1.0/nominatedStudents/2').flush(
        {
          code: 'VALIDATION_FAILED',
          status: 400,
          detail: 'Certaines informations sont invalides. Corrigez les champs indiqués.',
          errors: [
            { field: 'address.street', code: 'NotBlank', message: 'ne peut pas être vide' },
            { field: 'birthdate', code: 'Past', message: 'doit être une date passée' },
            { field: 'iban', code: 'Iban', message: 'doit être un IBAN valide' },
            { field: 'nationality.countryCode', code: 'Size', message: 'doit contenir exactement 2 caractères' },
            { field: 'address.country.countryCode', code: 'NotBlank', message: 'ne peut pas être vide' },
            { field: 'phoneNumber', code: 'PhoneNumber', message: 'doit être un numéro de téléphone valide' },
            { field: 'option.code', code: 'NotBlank', message: "L'option est obligatoire." },
          ],
        },
        { status: 400, statusText: 'Bad Request', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(alert()?.textContent).toContain('Certaines informations sont invalides.');
      expect(Array.from(el.querySelectorAll('[role=alert] li')).map((li) => li.textContent)).toEqual(["L'option est obligatoire."]);
      expect(error('street')).toBe('ne peut pas être vide');
      expect(error('birthdate')).toBe('doit être une date passée');
      expect(error('iban')).toBe('doit être un IBAN valide');
      expect(error('nationality')).toBe('doit contenir exactement 2 caractères');
      expect(error('country')).toBe('ne peut pas être vide');
      expect(error('phoneNumber')).toBe('doit être un numéro de téléphone valide');
      expect(control('birthdate').getAttribute('aria-invalid')).toBe('true');
      expect(el.querySelector('.notice')?.textContent?.trim()).toBe('');
    });

    it('offers to reload the record when it was changed elsewhere (409)', () => {
      submit();
      http.expectOne('/api/1.0/nominatedStudents/2').flush(
        {
          code: 'CONCURRENT_MODIFICATION',
          status: 409,
          detail: 'Ces données ont été modifiées par quelqu’un d’autre entre-temps. Rechargez-les puis recommencez.',
        },
        { status: 409, statusText: 'Conflict', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(alert()?.textContent).toContain('modifiées par quelqu’un d’autre');
      const reload = alert()!.querySelector<HTMLButtonElement>('button.reload')!;
      expect(reload.textContent).toContain('Recharger mes données');

      reload.click();
      fixture.detectChanges();
      expect(el.querySelector('form')).toBeNull();
      load({ ...RECORD, phoneNumber: '+32470000005', version: 5 });
      expect(alert()).toBeNull();
      expect(value('phoneNumber')).toBe('+32470000005');

      submit();
      const req = http.expectOne('/api/1.0/nominatedStudents/2');
      expect(req.request.body.version).toBe(5);
      req.flush(req.request.body);
    });

    it('goes back to the sign-in page when the session expired before the save', () => {
      const navigate = spyOn(router, 'navigate').and.resolveTo(true);
      submit();
      http.expectOne('/api/1.0/nominatedStudents/2').flush({ code: 'UNAUTHENTICATED', status: 401 }, { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS });
      fixture.detectChanges();
      expect(navigate).toHaveBeenCalledOnceWith(['/signin']);
      expect(alert()).toBeNull();
    });
  });
});

describe('compact', () => {
  it('removes the spaces and upper-cases an IBAN or a BIC', () => {
    expect(compact(' be68 5390\t0754 7034 ')).toBe('BE68539007547034');
    expect(compact('gkccbebb')).toBe('GKCCBEBB');
  });
});
