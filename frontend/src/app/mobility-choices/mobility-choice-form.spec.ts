import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { MobilityChoice, Partner, ProgrammeCountry, User } from '../core/models';
import { SessionService } from '../core/session.service';
import { MobilityChoiceForm, departures } from './mobility-choice-form';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };
const STUDENT: User = {
  id: 2,
  username: 'alice',
  firstName: 'Alice',
  lastName: 'Tester',
  role: 'Student',
};
const YEAR = new Date().getFullYear();

const PROGRAMMES = [
  { id: 1, programmeName: 'Erasmus+' },
  { id: 2, programmeName: 'Erabel' },
];
const COUNTRIES: ProgrammeCountry[] = [
  { countryCode: 'FR', name: 'France', programme: { id: 1 } },
  { countryCode: 'DE', name: 'Allemagne', programme: { id: 1 } },
  { countryCode: 'BE', name: 'Belgique', programme: { id: 2 } },
];
function partner(id: number, fullName: string, countryCode: string): Partner {
  return {
    id,
    fullName,
    address: { country: { countryCode, name: countryCode } },
    options: [],
    official: true,
    archived: false,
  };
}
const PARTNERS = [
  partner(3, 'Université de Lyon', 'FR'),
  partner(5, 'Sans Site', 'FR'),
  partner(4, 'Hochschule Berlin', 'DE'),
];

describe('MobilityChoiceForm', () => {
  let fixture: ComponentFixture<MobilityChoiceForm>;
  let http: HttpTestingController;
  let router: Router;
  let el: HTMLElement;
  let created: MobilityChoice[];
  let closed: number;

  function open(): void {
    TestBed.configureTestingModule({
      imports: [MobilityChoiceForm],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    TestBed.inject(SessionService).signIn({ username: 'alice', password: 'secret' }).subscribe();
    http.expectOne('/api/1.0/session').flush(STUDENT);
    fixture = TestBed.createComponent(MobilityChoiceForm);
    created = [];
    closed = 0;
    fixture.componentInstance.created.subscribe((c) => created.push(c));
    fixture.componentInstance.closed.subscribe(() => closed++);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  }

  function loadReferenceData(): void {
    http.expectOne('/api/1.0/programmes').flush(PROGRAMMES);
    http.expectOne('/api/1.0/countries').flush(COUNTRIES);
    http.expectOne('/api/1.0/partners').flush({ data: PARTNERS });
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  const select = (id: string) => el.querySelector<HTMLSelectElement>(`#mobility-choice-${id}`)!;
  const options = (id: string) => Array.from(select(id).options).map((o) => o.textContent?.trim());
  const error = (id: string) =>
    el.querySelector(`#mobility-choice-${id}-error`)?.textContent?.trim();
  function choose(id: string, value: string): void {
    select(id).value = value;
    select(id).dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }
  function submit(): void {
    el.querySelector<HTMLButtonElement>('button[type=submit]')!.click();
    fixture.detectChanges();
  }
  function fillRequired(): void {
    choose('programme', '1');
    choose('order', '2');
    choose('departure', `1 ${YEAR + 1}`);
  }

  describe('loading', () => {
    beforeEach(() => open());

    it('loads the programmes, the countries and the partners before showing the form', () => {
      expect(el.querySelector('[role=status]')?.textContent).toContain('Chargement du formulaire');
      expect(el.querySelector('form')).toBeNull();
      loadReferenceData();
      expect(el.querySelector('h2')?.textContent).toContain('Ajout de demande de mobilité');
      expect(el.querySelector('form')).not.toBeNull();
      expect(el.textContent).not.toContain('Chargement');
    });

    it('shows a failure with a retry button and a way out', () => {
      http
        .expectOne('/api/1.0/programmes')
        .flush(null, { status: 503, statusText: 'Service Unavailable' });
      // the first failure cancels the other requests
      expect(http.match(() => true).every((req) => req.cancelled)).toBeTrue();
      fixture.detectChanges();
      expect(el.querySelector('[role=alert]')?.textContent).toContain('momentanément indisponible');
      expect(el.querySelector('form')).toBeNull();

      el.querySelector<HTMLButtonElement>('button.retry')!.click();
      fixture.detectChanges();
      expect(el.querySelector('[role=alert]')).toBeNull();
      loadReferenceData();
      expect(el.querySelector('form')).not.toBeNull();
    });

    it('can be closed after a failure', () => {
      http.expectOne('/api/1.0/countries').flush(null, { status: 500, statusText: 'Server Error' });
      http.match(() => true);
      fixture.detectChanges();
      el.querySelector<HTMLButtonElement>('button.button')!.click();
      expect(closed).toBe(1);
    });

    it('goes back to the sign-in page when the session expired', () => {
      const navigate = spyOn(router, 'navigate').and.resolveTo(true);
      http
        .expectOne('/api/1.0/programmes')
        .flush(
          { code: 'UNAUTHENTICATED', status: 401 },
          { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS },
        );
      http.match(() => true);
      fixture.detectChanges();
      expect(navigate).toHaveBeenCalledOnceWith(['/signin']);
      expect(TestBed.inject(SessionService).user()).toBeNull();
    });
  });

  describe('fields', () => {
    beforeEach(() => {
      open();
      loadReferenceData();
    });

    it('has the fields and labels of the legacy form', () => {
      const labels = Array.from(el.querySelectorAll('label')).map((l) => [
        l.getAttribute('for'),
        l.textContent?.trim(),
      ]);
      expect(labels).toEqual([
        ['mobility-choice-programme', 'Programme'],
        ['mobility-choice-country', 'Pays'],
        ['mobility-choice-partner', 'Partenaire'],
        ['mobility-choice-type', 'Type de mobilité'],
        ['mobility-choice-order', 'Ordre de préférence'],
        ['mobility-choice-departure', 'Date de départ'],
      ]);
      expect(options('programme')).toEqual(['Choisissez un programme', 'Erasmus+', 'Erabel']);
      expect(options('type')).toEqual(['Académique (SMS)', 'Stage (SMP)']);
      expect(select('type').value).toBe('SMS');
      expect(options('order')).toEqual(["Choisissez l'ordre", '1', '2', '3']);
      expect(options('departure')).toEqual([
        'Choisissez la date de départ',
        `Février ${YEAR}`,
        `Septembre ${YEAR}`,
        `Février ${YEAR + 1}`,
        `Septembre ${YEAR + 1}`,
        `Février ${YEAR + 2}`,
        `Septembre ${YEAR + 2}`,
      ]);
    });

    it('offers the countries of the chosen programme, by name, and the partners of the chosen country', () => {
      expect(options('country')).toEqual(['Définir plus tard']);
      expect(options('partner')).toEqual(['Définir plus tard']);
      expect(select('partner').getAttribute('aria-describedby')).toBe(
        'mobility-choice-partner-hint',
      );

      choose('programme', '1');
      expect(options('country')).toEqual(['Définir plus tard', 'Allemagne', 'France']);
      choose('country', 'FR');
      expect(options('partner')).toEqual(['Définir plus tard', 'Sans Site', 'Université de Lyon']);
      choose('partner', '3');

      // another country drops the partner, another programme drops the country
      choose('country', 'DE');
      expect(select('partner').value).toBe('');
      expect(options('partner')).toEqual(['Définir plus tard', 'Hochschule Berlin']);
      choose('programme', '2');
      expect(select('country').value).toBe('');
      expect(options('country')).toEqual(['Définir plus tard', 'Belgique']);
      expect(options('partner')).toEqual(['Définir plus tard']);
    });

    it('requires the programme, the preference order and the departure before sending anything', () => {
      submit();
      expect(error('programme')).toBe('Ce champ est obligatoire.');
      expect(error('order')).toBe('Ce champ est obligatoire.');
      expect(error('departure')).toBe('Ce champ est obligatoire.');
      expect(error('country')).toBeUndefined();
      expect(error('partner')).toBeUndefined();
      expect(error('type')).toBeUndefined();
      expect(select('programme').getAttribute('aria-invalid')).toBe('true');
      expect(select('programme').getAttribute('aria-describedby')).toBe(
        'mobility-choice-programme-error',
      );
    });

    it('sends the choice of the signed-in student without country nor partner when they are left for later', () => {
      fillRequired();
      choose('type', 'SMP');
      submit();
      const req = http.expectOne('/api/1.0/mobilityChoice');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        user: { id: 2 },
        preferenceOrder: 2,
        mobilityType: 'SMP',
        academicYear: YEAR + 1,
        term: 1,
        programme: { id: 1 },
      });
      expect(el.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBeTrue();

      // a second submit while the request is in flight is ignored
      (fixture.componentInstance as unknown as { submit(): void }).submit();
      http.expectNone('/api/1.0/mobilityChoice');

      const choice = { id: 20 } as MobilityChoice;
      req.flush(choice);
      expect(created).toEqual([choice]);
    });

    it('sends the country and the partner when chosen', () => {
      fillRequired();
      choose('country', 'FR');
      choose('partner', '3');
      choose('departure', `2 ${YEAR}`);
      submit();
      const req = http.expectOne('/api/1.0/mobilityChoice');
      expect(req.request.body).toEqual(
        jasmine.objectContaining({
          country: { countryCode: 'FR' },
          partner: { id: 3 },
          academicYear: YEAR,
          term: 2,
          mobilityType: 'SMS',
        }),
      );
      req.flush({ id: 21 });
    });

    it("shows the server's field errors next to the inputs and the others in the alert", () => {
      fillRequired();
      submit();
      http.expectOne('/api/1.0/mobilityChoice').flush(
        {
          code: 'VALIDATION_FAILED',
          status: 400,
          detail: 'Certaines informations sont invalides.',
          errors: [
            { field: 'academicYear', code: 'Positive', message: "L'année doit être positive." },
            { field: 'preferenceOrder', code: 'Max', message: 'Au plus 3.' },
            { field: 'programme.id', code: 'Positive', message: 'Programme inconnu.' },
            { field: 'user.id', code: 'Positive', message: 'Utilisateur inconnu.' },
          ],
        },
        { status: 400, statusText: 'Bad Request', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(el.querySelector('[role=alert]')?.textContent).toContain(
        'Certaines informations sont invalides.',
      );
      expect(
        Array.from(el.querySelectorAll('[role=alert] li')).map((li) => li.textContent),
      ).toEqual(['Utilisateur inconnu.']);
      expect(error('departure')).toBe("L'année doit être positive.");
      expect(error('order')).toBe('Au plus 3.');
      expect(error('programme')).toBe('Programme inconnu.');
      expect(el.querySelector<HTMLButtonElement>('button[type=submit]')!.disabled).toBeFalse();
      expect(created).toEqual([]);
    });

    it('shows a business refusal in the alert', () => {
      fillRequired();
      choose('country', 'FR');
      submit();
      http.expectOne('/api/1.0/mobilityChoice').flush(
        {
          code: 'UNKNOWN_COUNTRY',
          status: 422,
          detail: 'Pays inconnu : FR.',
          errors: [{ field: 'country.countryCode', code: 'Invalid', message: 'Inconnu.' }],
        },
        { status: 422, statusText: 'Unprocessable Entity', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(el.querySelector('[role=alert] p')?.textContent).toBe('Pays inconnu : FR.');
      expect(el.querySelector('[role=alert] ul')).toBeNull();
      expect(error('country')).toBe('Inconnu.');
    });

    it('goes back to the sign-in page when the session expired before the submit', () => {
      const navigate = spyOn(router, 'navigate').and.resolveTo(true);
      fillRequired();
      submit();
      http
        .expectOne('/api/1.0/mobilityChoice')
        .flush(
          { code: 'UNAUTHENTICATED', status: 401 },
          { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS },
        );
      expect(navigate).toHaveBeenCalledOnceWith(['/signin']);
      expect(el.querySelector('[role=alert]')).toBeNull();
    });

    it('closes without sending anything', () => {
      fillRequired();
      el.querySelector<HTMLButtonElement>('form button.button:not([type=submit])')!.click();
      expect(closed).toBe(1);
      expect(created).toEqual([]);
    });
  });
});

describe('departures', () => {
  it('lists three years, February then September, like the legacy form', () => {
    expect(departures(2030).map((d) => [d.value, d.term, d.academicYear, d.label])).toEqual([
      ['2 2030', 2, 2030, 'Février 2030'],
      ['1 2030', 1, 2030, 'Septembre 2030'],
      ['2 2031', 2, 2031, 'Février 2031'],
      ['1 2031', 1, 2031, 'Septembre 2031'],
      ['2 2032', 2, 2032, 'Février 2032'],
      ['1 2032', 1, 2032, 'Septembre 2032'],
    ]);
    expect(departures(2030, 1).length).toBe(2);
  });
});
