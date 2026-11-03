import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { MOCK_PLATFORM_LOCATION_CONFIG } from '@angular/common/testing';

import { ApiService, mobilityChoicesExportUrl } from './api.service';
import { appConfig } from './app.config';
import { ApiProblem } from './core/api-problem';
import {
  MobilityChoice,
  MobilityChoiceRequest,
  NominatedStudent,
  Partner,
  SignUpRequest,
  User,
} from './core/models';

const USER: User = { id: 1, username: 'jdoe', firstName: 'John', lastName: 'Doe', email: 'j@d.be' };

describe('ApiService', () => {
  describe('with the application providers', () => {
    beforeEach(() => {
      TestBed.configureTestingModule({ providers: [...appConfig.providers] });
    });

    it('can be injected because the app config provides HttpClient', () => {
      expect(TestBed.inject(HttpClient)).toBeTruthy();
      expect(TestBed.inject(ApiService)).toBeTruthy();
    });
  });

  describe('requests', () => {
    let service: ApiService;
    let http: HttpTestingController;
    let bootstrapRequests: TestRequest[] = [];

    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [
          ...appConfig.providers,
          provideHttpClientTesting(),
          { provide: LOCALE_ID, useValue: 'en' },
          // the XSRF interceptor only sends the token to the page's own origin, which it reads
          // from PlatformLocation: the test bed's mock location needs a real URL for that
          { provide: MOCK_PLATFORM_LOCATION_CONFIG, useValue: { startUrl: 'http://localhost/' } },
        ],
      });
      service = TestBed.inject(ApiService);
      http = TestBed.inject(HttpTestingController);
      // the app initializer fetches the XSRF-TOKEN cookie once (see core/csrf-bootstrap.ts)
      bootstrapRequests = http.match('/api/1.0/options');
      bootstrapRequests.forEach((req) => req.flush([]));
    });

    it('fetches the XSRF-TOKEN cookie at start-up, before any form is submitted', () => {
      expect(bootstrapRequests.length).withContext('one GET at start-up').toBe(1);
      expect(bootstrapRequests[0].request.method).toBe('GET');
    });

    afterEach(() => http.verify());

    // Relative URLs keep the browser on its own origin: the dev-server proxy (ng serve) or
    // nginx (Docker) forwards /api to the backend, so no cross-origin request is made.
    it('posts the credentials as JSON to the session endpoint and returns the user', () => {
      let user: User | undefined;
      service.login({ username: 'jdoe', password: 'secret' }).subscribe((u) => (user = u));

      const req = http.expectOne('/api/1.0/session');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'jdoe', password: 'secret' });
      req.flush(USER);
      expect(user).toEqual(USER);
    });

    it('posts the user object itself (no {data} wrapper) to the users endpoint', () => {
      const body: SignUpRequest = {
        username: 'jdoe',
        password: 'secret',
        firstName: 'John',
        lastName: 'Doe',
        email: 'j@d.be',
        option: { code: 'BIN' },
      };
      service.signup(body).subscribe();

      const req = http.expectOne('/api/1.0/users');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(body);
      expect(req.request.body).not.toEqual(jasmine.objectContaining({ data: jasmine.anything() }));
      req.flush(USER, { status: 201, statusText: 'Created' });
    });

    it('reads the current user from GET /session', () => {
      let user: User | undefined;
      service.currentUser().subscribe((u) => (user = u));
      const req = http.expectOne('/api/1.0/session');
      expect(req.request.method).toBe('GET');
      req.flush(USER);
      expect(user).toEqual(USER);
    });

    it('closes the session with DELETE /session', () => {
      let done = false;
      service.logout().subscribe({ complete: () => (done = true) });
      const req = http.expectOne('/api/1.0/session');
      expect(req.request.method).toBe('DELETE');
      req.flush(null, { status: 204, statusText: 'No Content' });
      expect(done).toBeTrue();
    });

    it('lists the partners from GET /partners, unwrapping the data member', () => {
      const partner = {
        id: 1,
        fullName: 'Université de Lyon',
        address: { country: { countryCode: 'FR', name: 'France' } },
        options: [{ code: 'BIN' }],
        official: true,
        archived: false,
      };
      let partners: Partner[] | undefined;
      service.partners().subscribe((p) => (partners = p));
      const req = http.expectOne('/api/1.0/partners');
      expect(req.request.method).toBe('GET');
      req.flush({ data: [partner] });
      expect(partners).toEqual([partner]);
    });

    it('fails with an ApiProblem when the partners cannot be listed', () => {
      let problem: unknown;
      service.partners().subscribe({ error: (e) => (problem = e) });
      http
        .expectOne('/api/1.0/partners')
        .flush(null, { status: 503, statusText: 'Service Unavailable' });
      expect(problem).toBeInstanceOf(ApiProblem);
      expect((problem as ApiProblem).code).toBe('SERVICE_UNAVAILABLE');
    });

    describe('mobility choices', () => {
      const CHOICE: MobilityChoice = {
        id: 7,
        user: { id: 2, firstName: 'Alice', lastName: 'Tester' },
        preferenceOrder: 1,
        mobilityType: 'SMS',
        academicYear: 2026,
        term: 1,
        programme: { id: 1, programmeName: 'Erasmus+' },
        country: { countryCode: 'FR', name: 'France' },
        partner: { id: 3, fullName: 'Université de Lyon' },
      };

      it('lists the choices without a filter (the API applies its default)', () => {
        let choices: MobilityChoice[] | undefined;
        service.mobilityChoices().subscribe((c) => (choices = c));
        const req = http.expectOne((r) => r.url === '/api/1.0/mobilityChoices');
        expect(req.request.method).toBe('GET');
        expect(req.request.params.keys()).toEqual([]);
        req.flush({ data: [CHOICE] });
        expect(choices).toEqual([CHOICE]);
      });

      it('sends the filter as a query parameter', () => {
        service.mobilityChoices('rejected').subscribe();
        const req = http.expectOne('/api/1.0/mobilityChoices?filter=rejected');
        expect(req.request.method).toBe('GET');
        req.flush({ data: [] });
      });

      it('reads the count of a filter', () => {
        let count: number | undefined;
        service.countMobilityChoices('all').subscribe((c) => (count = c));
        const req = http.expectOne('/api/1.0/mobilityChoices/count?filter=all');
        expect(req.request.method).toBe('GET');
        req.flush({ count: 4 });
        expect(count).toBe(4);
      });

      it('reads the count without a filter', () => {
        service.countMobilityChoices().subscribe();
        http.expectOne('/api/1.0/mobilityChoices/count').flush({ count: 0 });
      });

      it('creates a choice with POST /mobilityChoice (singular), the choice itself as body', () => {
        const body: MobilityChoiceRequest = {
          user: { id: 2 },
          preferenceOrder: 1,
          mobilityType: 'SMP',
          academicYear: 2027,
          term: 2,
          programme: { id: 1 },
          country: { countryCode: 'FR' },
          partner: { id: 3 },
        };
        let created: MobilityChoice | undefined;
        service.createMobilityChoice(body).subscribe((c) => (created = c));
        const req = http.expectOne('/api/1.0/mobilityChoice');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(body);
        req.flush(CHOICE);
        expect(created).toEqual(CHOICE);
      });

      it('cancels a choice with the reason as a query parameter', () => {
        let done = false;
        service
          .cancelMobilityChoice(7, "J'ai changé d'avis & de pays")
          .subscribe({ complete: () => (done = true) });
        const req = http.expectOne((r) => r.url === '/api/1.0/mobilityChoices/7/cancel');
        expect(req.request.method).toBe('PUT');
        expect(req.request.params.get('reason')).toBe("J'ai changé d'avis & de pays");
        expect(req.request.urlWithParams).toContain(
          "reason=J'ai%20chang%C3%A9%20d'avis%20%26%20de%20pays",
        );
        expect(req.request.body).toBeNull();
        req.flush(null);
        expect(done).toBeTrue();
      });

      it('rejects a choice with the id of the denial reason', () => {
        service.rejectMobilityChoice(7, 4).subscribe();
        const req = http.expectOne('/api/1.0/mobilityChoices/7/reject?reason=4');
        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toBeNull();
        req.flush(null);
      });

      it('confirms a choice', () => {
        service.confirmMobilityChoice(7).subscribe();
        const req = http.expectOne('/api/1.0/mobilityChoices/7/confirm');
        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toBeNull();
        req.flush(null);
      });

      it('fails with the ApiProblem of the backend when an action is refused', () => {
        let problem: unknown;
        service.confirmMobilityChoice(7).subscribe({ error: (e) => (problem = e) });
        http.expectOne('/api/1.0/mobilityChoices/7/confirm').flush(
          { code: 'PARTNER_REQUIRED_TO_CONFIRM', status: 422, detail: 'Choose a partner first.' },
          {
            status: 422,
            statusText: 'Unprocessable Entity',
            headers: { 'Content-Type': 'application/problem+json' },
          },
        );
        expect(problem).toBeInstanceOf(ApiProblem);
        expect((problem as ApiProblem).code).toBe('PARTNER_REQUIRED_TO_CONFIRM');
        expect((problem as ApiProblem).detail).toBe('Choose a partner first.');
      });

      it('lists the ids of the mobilities', () => {
        let ids: number[] | undefined;
        service.mobilityIds().subscribe((i) => (ids = i));
        const req = http.expectOne('/api/1.0/mobilities');
        expect(req.request.method).toBe('GET');
        req.flush({
          data: [
            { id: 7, state: 'Créée' },
            { id: 9, state: 'Terminée' },
          ],
        });
        expect(ids).toEqual([7, 9]);
      });

      it('builds the URL of the CSV export of a filter', () => {
        expect(mobilityChoicesExportUrl('active')).toBe(
          '/api/1.0/mobilityChoices/export?filter=active',
        );
        expect(mobilityChoicesExportUrl('passed')).toBe(
          '/api/1.0/mobilityChoices/export?filter=passed',
        );
      });
    });

    describe('reference data (plain arrays)', () => {
      it('lists the programmes', () => {
        let names: string[] | undefined;
        service
          .programmes()
          .subscribe((p) => (names = p.map((programme) => programme.programmeName)));
        const req = http.expectOne('/api/1.0/programmes');
        expect(req.request.method).toBe('GET');
        req.flush([
          { id: 1, programmeName: 'Erasmus+' },
          { id: 2, programmeName: 'Erabel' },
        ]);
        expect(names).toEqual(['Erasmus+', 'Erabel']);
      });

      it('lists the countries with their programme', () => {
        const countries = [{ countryCode: 'FR', name: 'France', programme: { id: 1 } }];
        let got: unknown;
        service.countries().subscribe((c) => (got = c));
        http.expectOne('/api/1.0/countries').flush(countries);
        expect(got).toEqual(countries);
      });

      it('lists the denial reasons', () => {
        const reasons = [{ id: 1, reason: 'Dossier incomplet' }];
        let got: unknown;
        service.denialReasons().subscribe((r) => (got = r));
        const req = http.expectOne('/api/1.0/denialReasons');
        expect(req.request.method).toBe('GET');
        req.flush(reasons);
        expect(got).toEqual(reasons);
      });

      it('fails with an ApiProblem when the reference data cannot be read', () => {
        const problems: ApiProblem[] = [];
        service.programmes().subscribe({ error: (e) => problems.push(e) });
        service.countries().subscribe({ error: (e) => problems.push(e) });
        service.denialReasons().subscribe({ error: (e) => problems.push(e) });
        service.mobilityChoices().subscribe({ error: (e) => problems.push(e) });
        service.countMobilityChoices().subscribe({ error: (e) => problems.push(e) });
        service
          .createMobilityChoice({} as MobilityChoiceRequest)
          .subscribe({ error: (e) => problems.push(e) });
        service.cancelMobilityChoice(1, 'x').subscribe({ error: (e) => problems.push(e) });
        service.rejectMobilityChoice(1, 1).subscribe({ error: (e) => problems.push(e) });
        service.mobilityIds().subscribe({ error: (e) => problems.push(e) });
        http
          .match(() => true)
          .forEach((req) => req.flush(null, { status: 500, statusText: 'Server Error' }));
        expect(problems.length).toBe(9);
        expect(
          problems.every((p) => p instanceof ApiProblem && p.code === 'SERVER_ERROR'),
        ).toBeTrue();
      });
    });

    describe('personal data and users', () => {
      const STUDENT = {
        id: 2,
        username: 'alice',
        firstName: 'Alice',
        lastName: 'Tester',
        email: 'alice@example.test',
        option: { code: 'BIN' },
        title: 'Ms',
        birthdate: '2001-04-12',
        nationality: { countryCode: 'BE' },
        address: {
          street: 'Rue de la Loi',
          number: '16',
          city: 'Bruxelles',
          postalCode: '1000',
          region: '',
          country: { countryCode: 'BE' },
        },
        phoneNumber: '+32470000001',
        gender: 'F',
        nbrPassedYears: 1,
        iban: 'BE68539007547034',
        bankName: 'Belfius',
        bic: 'GKCCBEBB',
        cardHolder: '',
        version: 3,
      } satisfies NominatedStudent;

      it('reads the personal data of a student with GET /nominatedStudents/{id}', () => {
        let student: NominatedStudent | undefined;
        service.nominatedStudent(2).subscribe((s) => (student = s));
        const req = http.expectOne('/api/1.0/nominatedStudents/2');
        expect(req.request.method).toBe('GET');
        req.flush(STUDENT);
        expect(student).toEqual(STUDENT);
      });

      it('fails with a RESOURCE_NOT_FOUND problem while the personal data were never saved', () => {
        let problem: ApiProblem | undefined;
        service.nominatedStudent(2).subscribe({ error: (e: ApiProblem) => (problem = e) });
        http.expectOne('/api/1.0/nominatedStudents/2').flush(
          {
            code: 'RESOURCE_NOT_FOUND',
            status: 404,
            detail: 'La ressource demandée n’existe pas.',
          },
          {
            status: 404,
            statusText: 'Not Found',
            headers: { 'Content-Type': 'application/problem+json' },
          },
        );
        expect(problem?.status).toBe(404);
        expect(problem?.code).toBe('RESOURCE_NOT_FOUND');
      });

      it('creates the personal data with POST /nominatedStudents, the student itself as body', () => {
        let saved: NominatedStudent | undefined;
        service.createNominatedStudent(STUDENT).subscribe((s) => (saved = s));
        const req = http.expectOne('/api/1.0/nominatedStudents');
        expect(req.request.method).toBe('POST');
        expect(req.request.body).toEqual(STUDENT);
        req.flush({ ...STUDENT, version: 1 });
        expect(saved?.version).toBe(1);
      });

      it('updates the personal data with PUT /nominatedStudents/{id} and returns the new version', () => {
        let saved: NominatedStudent | undefined;
        service.updateNominatedStudent(STUDENT).subscribe((s) => (saved = s));
        const req = http.expectOne('/api/1.0/nominatedStudents/2');
        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toEqual(STUDENT);
        req.flush({ ...STUDENT, version: 4 });
        expect(saved?.version).toBe(4);
      });

      it('lists the users from GET /users, unwrapping the data member', () => {
        let users: User[] | undefined;
        service.users().subscribe((u) => (users = u));
        const req = http.expectOne('/api/1.0/users');
        expect(req.request.method).toBe('GET');
        req.flush({ data: [USER] });
        expect(users).toEqual([USER]);
      });

      it('promotes a user with PUT /users/{id}/promote and no body', () => {
        let done = false;
        service.promoteUser(3).subscribe(() => (done = true));
        const req = http.expectOne('/api/1.0/users/3/promote');
        expect(req.request.method).toBe('PUT');
        expect(req.request.body).toBeNull();
        req.flush(null);
        expect(done).toBeTrue();
      });
    });

    it('sends the UI language as Accept-Language', () => {
      service.currentUser().subscribe({ error: () => undefined });
      const req = http.expectOne('/api/1.0/session');
      expect(req.request.headers.get('Accept-Language')).toBe('en');
      req.flush(null, { status: 401, statusText: 'Unauthorized' });
    });

    it('fails with an ApiProblem, never a raw HttpErrorResponse', () => {
      let problem: unknown;
      service.currentUser().subscribe({ error: (e) => (problem = e) });
      http.expectOne('/api/1.0/session').flush(
        { code: 'UNAUTHENTICATED', status: 401, detail: 'Please sign in.' },
        {
          status: 401,
          statusText: 'Unauthorized',
          headers: { 'Content-Type': 'application/problem+json' },
        },
      );
      expect(problem).toBeInstanceOf(ApiProblem);
      expect((problem as ApiProblem).code).toBe('UNAUTHENTICATED');
      expect((problem as ApiProblem).detail).toBe('Please sign in.');
    });

    describe('CSRF (Spring Security SPA setup)', () => {
      const cookie = 'XSRF-TOKEN';

      afterEach(() => {
        document.cookie = `${cookie}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
      });

      it('echoes the XSRF-TOKEN cookie as X-XSRF-TOKEN on mutating requests only', () => {
        document.cookie = `${cookie}=token-123; path=/`;

        service.login({ username: 'a', password: 'b' }).subscribe();
        const post = http.expectOne((r) => r.method === 'POST');
        expect(post.request.headers.get('X-XSRF-TOKEN')).toBe('token-123');
        post.flush(USER);

        service.currentUser().subscribe();
        const get = http.expectOne((r) => r.method === 'GET');
        expect(get.request.headers.has('X-XSRF-TOKEN')).toBeFalse();
        get.flush(USER);
      });

      it('never sends the token to another origin', () => {
        document.cookie = `${cookie}=token-123; path=/`;

        TestBed.inject(HttpClient).post('https://elsewhere.example.com/api', {}).subscribe();
        const post = http.expectOne('https://elsewhere.example.com/api');
        expect(post.request.headers.has('X-XSRF-TOKEN')).toBeFalse();
        post.flush(null);
      });
    });
  });
});
