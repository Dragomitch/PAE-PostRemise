import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { SignIn } from './sign-in';
import { knownCodeMessage, statusFallback } from '../core/api-problem';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };

describe('SignIn', () => {
  let fixture: ComponentFixture<SignIn>;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [SignIn],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    fixture = TestBed.createComponent(SignIn);
    http = TestBed.inject(HttpTestingController);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  function type(selector: string, value: string): void {
    const input = el.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function submit(): void {
    el.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function fillAndSubmit(): void {
    type('#signin-username', 'jdoe');
    type('#signin-password', 'secret');
    submit();
  }

  const button = () => el.querySelector<HTMLButtonElement>('button[type=submit]')!;
  const alert = () => el.querySelector('[role=alert]');
  const status = () => el.querySelector('[role=status]');

  it('renders labelled inputs and no message initially', () => {
    expect(el.querySelector('h1')?.textContent).toContain('Bienvenue à bord');
    expect(el.querySelector('label[for=signin-username]')).not.toBeNull();
    expect(el.querySelector('label[for=signin-password]')).not.toBeNull();
    expect(alert()).toBeNull();
    expect(status()).toBeNull();
    expect(el.querySelector('a[href="/signup"]')).not.toBeNull();
  });

  it('shows required errors and sends nothing when the form is empty', () => {
    submit();
    http.expectNone('/api/1.0/session');
    const usernameError = el.querySelector('#signin-username-error');
    expect(usernameError?.textContent).toContain('Ce champ est obligatoire.');
    expect(el.querySelector('#signin-password-error')).not.toBeNull();
    const input = el.querySelector('#signin-username')!;
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(input.getAttribute('aria-describedby')).toBe('signin-username-error');
  });

  it('disables the submit button while the request is in flight, then shows the user', () => {
    fillAndSubmit();
    const req = http.expectOne('/api/1.0/session');
    expect(req.request.body).toEqual({ username: 'jdoe', password: 'secret' });
    expect(button().disabled).toBeTrue();
    expect(button().getAttribute('aria-busy')).toBe('true');
    expect(button().textContent).toContain('Connexion en cours');

    // A second submit while in flight is ignored.
    submit();
    http.expectNone('/api/1.0/session');

    req.flush({ username: 'jdoe', firstName: 'John' });
    fixture.detectChanges();
    expect(button().disabled).toBeFalse();
    expect(button().textContent).toContain('Se connecter');
    expect(status()?.textContent).toContain('Vous êtes connecté en tant que John.');
    expect(alert()).toBeNull();
  });

  it('greets by username when the first name is unknown', () => {
    fillAndSubmit();
    http.expectOne('/api/1.0/session').flush({ username: 'jdoe' });
    fixture.detectChanges();
    expect(status()?.textContent).toContain('jdoe');
  });

  it('shows the backend localized detail in an alert and re-enables the button', () => {
    fillAndSubmit();
    http.expectOne('/api/1.0/session').flush(
      { code: 'INVALID_CREDENTIALS', status: 401, title: 'Non autorisé', detail: 'Identifiants refusés par le serveur.' },
      { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS },
    );
    fixture.detectChanges();
    expect(alert()?.textContent).toContain('Identifiants refusés par le serveur.');
    expect(button().disabled).toBeFalse();
    expect(status()).toBeNull();
  });

  it('falls back to the known-code message when the backend sends no detail', () => {
    fillAndSubmit();
    http
      .expectOne('/api/1.0/session')
      .flush({ code: 'INVALID_CREDENTIALS', status: 401 }, { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();
    expect(alert()?.textContent).toContain(knownCodeMessage('INVALID_CREDENTIALS')!);
  });

  it('shows a localized network message, never the raw error', () => {
    fillAndSubmit();
    http.expectOne('/api/1.0/session').error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(alert()?.textContent).toContain(statusFallback(0).message);
  });

  it('shows server field errors next to the inputs and other errors in the alert', () => {
    fillAndSubmit();
    http.expectOne('/api/1.0/session').flush(
      {
        code: 'VALIDATION_FAILED',
        status: 400,
        detail: 'Données invalides.',
        errors: [
          { field: 'username', code: 'Size', message: 'Trop court.' },
          { field: 'captcha', code: 'NotNull', message: 'Captcha manquant.' },
        ],
      },
      { status: 400, statusText: 'Bad Request', headers: PROBLEM_HEADERS },
    );
    fixture.detectChanges();
    expect(el.querySelector('#signin-username-error')?.textContent).toContain('Trop court.');
    expect(el.querySelector('#signin-password-error')).toBeNull();
    expect(alert()?.querySelectorAll('li').length).toBe(1);
    expect(alert()?.textContent).toContain('Captcha manquant.');
  });

  it('clears the previous error when submitting again', () => {
    fillAndSubmit();
    http.expectOne('/api/1.0/session').error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(alert()).not.toBeNull();

    submit();
    expect(alert()).toBeNull();
    http.expectOne('/api/1.0/session').flush({ username: 'jdoe' });
  });

  it('confirms a fresh registration until the user signs in', () => {
    fixture.componentRef.setInput('registered', 'true');
    fixture.detectChanges();
    expect(status()?.textContent).toContain('Votre compte a été créé');

    fillAndSubmit();
    http.expectOne('/api/1.0/session').flush({ username: 'jdoe', firstName: 'John' });
    fixture.detectChanges();
    expect(el.querySelectorAll('[role=status]').length).toBe(1);
    expect(status()?.textContent).toContain('John');
  });
});
