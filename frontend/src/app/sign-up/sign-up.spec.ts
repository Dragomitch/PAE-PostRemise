import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { SignUp } from './sign-up';
import { knownCodeMessage, statusFallback } from '../core/api-problem';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };

describe('SignUp', () => {
  let fixture: ComponentFixture<SignUp>;
  let http: HttpTestingController;
  let el: HTMLElement;
  let navigate: jasmine.Spy;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [SignUp],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(SignUp);
    http = TestBed.inject(HttpTestingController);
    navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function set(id: string, value: string): void {
    const input = el.querySelector<HTMLInputElement | HTMLSelectElement>(`#signup-${id}`)!;
    input.value = value;
    input.dispatchEvent(new Event(input instanceof HTMLSelectElement ? 'change' : 'input'));
  }

  function fill(overrides: Record<string, string> = {}): void {
    const values: Record<string, string> = {
      lastName: 'Doe',
      firstName: 'John',
      username: 'jdoe',
      password: 'secret',
      reenterPassword: 'secret',
      email: 'john@doe.be',
      option: 'BIN',
      ...overrides,
    };
    for (const [id, value] of Object.entries(values)) {
      set(id, value);
    }
  }

  function submit(): void {
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  const button = () => el.querySelector<HTMLButtonElement>('button[type=submit]')!;
  const alert = () => el.querySelector('[role=alert]');
  const errorOf = (id: string) => el.querySelector(`#signup-${id}-error`)?.textContent?.trim() ?? null;

  it('renders a labelled control for every field, with the IPL options', () => {
    for (const id of ['lastName', 'firstName', 'username', 'password', 'reenterPassword', 'email', 'option']) {
      expect(el.querySelector(`label[for=signup-${id}]`)).withContext(id).not.toBeNull();
    }
    const options = Array.from(el.querySelectorAll<HTMLOptionElement>('#signup-option option'));
    expect(options.map((o) => o.value)).toEqual(['', 'BIN', 'BBM', 'BCH', 'BDI', 'BIM']);
    expect(alert()).toBeNull();
  });

  it('validates on the client before calling the backend', () => {
    fill({ email: 'not-an-email', reenterPassword: 'other', firstName: '' });
    submit();
    http.expectNone('/api/1.0/users');
    expect(errorOf('email')).toBe('Saisissez une adresse e-mail valide.');
    expect(errorOf('reenterPassword')).toBe('Les mots de passe ne correspondent pas.');
    expect(errorOf('firstName')).toBe('Ce champ est obligatoire.');
    expect(errorOf('lastName')).toBeNull();
  });

  it('sends the user object directly and navigates to sign-in on success', () => {
    fill();
    submit();
    const req = http.expectOne('/api/1.0/users');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      username: 'jdoe',
      password: 'secret',
      firstName: 'John',
      lastName: 'Doe',
      email: 'john@doe.be',
      option: { code: 'BIN' },
    });
    expect(button().disabled).toBeTrue();
    expect(button().textContent).toContain('Inscription en cours');

    submit();
    http.expectNone('/api/1.0/users');

    req.flush({ id: 7, username: 'jdoe' }, { status: 201, statusText: 'Created' });
    fixture.detectChanges();
    expect(button().disabled).toBeFalse();
    expect(navigate).toHaveBeenCalledOnceWith(['/signin'], { queryParams: { registered: true } });
  });

  it('shows the backend detail and the field errors next to the matching inputs', () => {
    fill();
    submit();
    http.expectOne('/api/1.0/users').flush(
      {
        type: 'urn:pae:problem:validation-failed',
        title: 'Données invalides',
        status: 400,
        detail: 'Veuillez corriger le formulaire.',
        code: 'VALIDATION_FAILED',
        errors: [
          { field: 'email', code: 'Email', message: 'Adresse refusée par le serveur.' },
          { field: 'option.code', code: 'NotFound', message: 'Option inconnue.' },
          { field: 'role', code: 'Null', message: 'Rôle interdit.' },
        ],
      },
      { status: 400, statusText: 'Bad Request', headers: PROBLEM_HEADERS },
    );
    fixture.detectChanges();

    expect(alert()?.textContent).toContain('Veuillez corriger le formulaire.');
    expect(alert()?.textContent).toContain('Rôle interdit.');
    expect(errorOf('email')).toBe('Adresse refusée par le serveur.');
    expect(errorOf('option')).toBe('Option inconnue.');
    expect(errorOf('username')).toBeNull();
    expect(el.querySelector('#signup-email')?.getAttribute('aria-invalid')).toBe('true');
    expect(el.querySelector('#signup-email')?.getAttribute('aria-describedby')).toBe('signup-email-error');
    expect(button().disabled).toBeFalse();
    expect(navigate).not.toHaveBeenCalled();
  });

  it('uses the known-code message for USERNAME_TAKEN without detail', () => {
    fill();
    submit();
    http
      .expectOne('/api/1.0/users')
      .flush({ code: 'USERNAME_TAKEN', status: 409 }, { status: 409, statusText: 'Conflict', headers: PROBLEM_HEADERS });
    fixture.detectChanges();
    expect(alert()?.textContent).toContain(knownCodeMessage('USERNAME_TAKEN')!);
    expect(alert()?.querySelector('ul')).toBeNull();
  });

  it('shows a localized message for a proxy error page', () => {
    fill();
    submit();
    http.expectOne('/api/1.0/users').flush('<html><h1>502 Bad Gateway</h1></html>', {
      status: 502,
      statusText: 'Bad Gateway',
      headers: { 'Content-Type': 'text/html' },
    });
    fixture.detectChanges();
    expect(alert()?.textContent?.trim()).toBe(statusFallback(502).message);
    expect(alert()?.innerHTML).not.toContain('Bad Gateway');
  });
});
