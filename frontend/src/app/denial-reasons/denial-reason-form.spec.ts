import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { DenialReason, User } from '../core/models';
import { SessionService } from '../core/session.service';
import { DenialReasonForm } from './denial-reason-form';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };
const PROFESSOR: User = {
  id: 1,
  username: 'prof',
  firstName: 'Paula',
  lastName: 'Prof',
  role: 'Professor',
};

describe('DenialReasonForm', () => {
  let fixture: ComponentFixture<DenialReasonForm>;
  let http: HttpTestingController;
  let router: Router;
  let el: HTMLElement;
  let created: DenialReason[];
  let closed: number;

  function open(inputs: Record<string, string> = {}): void {
    TestBed.configureTestingModule({
      imports: [DenialReasonForm],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    TestBed.inject(SessionService).signIn({ username: 'prof', password: 'secret' }).subscribe();
    http.expectOne('/api/1.0/session').flush(PROFESSOR);
    fixture = TestBed.createComponent(DenialReasonForm);
    for (const [name, value] of Object.entries(inputs)) {
      fixture.componentRef.setInput(name, value);
    }
    created = [];
    closed = 0;
    fixture.componentInstance.created.subscribe((r) => created.push(r));
    fixture.componentInstance.closed.subscribe(() => closed++);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  const input = () => el.querySelector<HTMLInputElement>('input')!;
  const error = () => el.querySelector('.field-error');
  const alert = () => el.querySelector('[role=alert]');
  const submitButton = () => el.querySelector<HTMLButtonElement>('button.create')!;
  function type(value: string): void {
    input().value = value;
    input().dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }
  function submit(): void {
    submitButton().click();
    fixture.detectChanges();
  }

  it('asks for the text of the new reason and takes the focus', async () => {
    open();
    const label = el.querySelector('label')!;
    expect(label.textContent).toContain('Nouvelle raison');
    expect(label.getAttribute('for')).toBe('denial-reason-form-reason');
    expect(input().id).toBe('denial-reason-form-reason');
    expect(input().getAttribute('maxlength')).toBe('300');
    expect(el.querySelector('form')?.getAttribute('aria-labelledby')).toBe(
      'denial-reason-form-label',
    );
    expect(submitButton().textContent).toContain('Créer');
    expect(el.querySelector('button.close-form')?.textContent).toContain('Annuler');
    await fixture.whenStable();
    expect(document.activeElement).toBe(input());
  });

  it('takes the id prefix and the close label of the embedding screen', () => {
    open({ idPrefix: 'reject-new', closeLabel: 'Choisir parmi les raisons existantes' });
    expect(input().id).toBe('reject-new-reason');
    expect(el.querySelector('label')?.id).toBe('reject-new-label');
    expect(el.querySelector('button.close-form')?.textContent).toContain(
      'Choisir parmi les raisons existantes',
    );
  });

  it('requires a text that is not only spaces before sending anything', () => {
    open();
    submit();
    expect(error()?.textContent).toContain('Ce champ est obligatoire.');
    expect(input().getAttribute('aria-invalid')).toBe('true');
    expect(input().getAttribute('aria-describedby')).toBe('denial-reason-form-reason-error');

    type('   ');
    submit();
    expect(input().value).toBe('');
    expect(error()?.textContent).toContain('Ce champ est obligatoire.');
    http.expectNone('/api/1.0/denialReasons');
    expect(created).toEqual([]);
  });

  it('refuses a text longer than 300 characters', () => {
    open();
    type('x'.repeat(301));
    expect(error()?.textContent).toContain('La raison ne peut pas dépasser 300 caractères.');
    submit();
    http.expectNone('/api/1.0/denialReasons');
  });

  it('posts the trimmed text and emits the stored reason', () => {
    open();
    type('  Dossier incomplet  ');
    expect(error()).toBeNull();
    submit();
    const req = http.expectOne('/api/1.0/denialReasons');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ reason: 'Dossier incomplet' });
    expect(submitButton().disabled).toBeTrue();
    expect(submitButton().getAttribute('aria-busy')).toBe('true');

    // a second submit while the first one is in flight sends nothing
    fixture.componentInstance['submit']();
    http.expectNone('/api/1.0/denialReasons');

    req.flush({ id: 7, reason: 'Dossier incomplet', version: 0 });
    fixture.detectChanges();
    expect(created).toEqual([{ id: 7, reason: 'Dossier incomplet', version: 0 }]);
    expect(submitButton().disabled).toBeFalse();
  });

  it("shows the API's field error next to the input and the others in the alert", () => {
    open();
    type('Dossier incomplet');
    submit();
    http.expectOne('/api/1.0/denialReasons').flush(
      {
        code: 'VALIDATION_FAILED',
        status: 400,
        detail: 'Certains champs sont invalides.',
        errors: [
          { field: 'reason', code: 'Size', message: 'Trop long.' },
          { field: 'version', code: 'Min', message: 'Version invalide.' },
        ],
      },
      { status: 400, statusText: 'Bad Request', headers: PROBLEM_HEADERS },
    );
    fixture.detectChanges();
    expect(error()?.textContent).toBe('Trop long.');
    expect(input().getAttribute('aria-invalid')).toBe('true');
    expect(alert()?.textContent).toContain('Certains champs sont invalides.');
    expect(Array.from(alert()!.querySelectorAll('li')).map((li) => li.textContent)).toEqual([
      'Version invalide.',
    ]);
    expect(created).toEqual([]);
  });

  it('shows a refusal without field errors', () => {
    open();
    type('Dossier incomplet');
    submit();
    http
      .expectOne('/api/1.0/denialReasons')
      .flush(
        { code: 'ACCESS_DENIED', status: 403, detail: 'Accès refusé.' },
        { status: 403, statusText: 'Forbidden', headers: PROBLEM_HEADERS },
      );
    fixture.detectChanges();
    expect(alert()?.textContent).toContain('Accès refusé.');
    expect(alert()?.querySelector('ul')).toBeNull();
    expect(error()).toBeNull();
  });

  it('goes back to the sign-in page when the session expired', () => {
    open();
    const navigate = spyOn(router, 'navigate').and.resolveTo(true);
    type('Dossier incomplet');
    submit();
    http
      .expectOne('/api/1.0/denialReasons')
      .flush(
        { code: 'UNAUTHENTICATED', status: 401 },
        { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS },
      );
    fixture.detectChanges();
    expect(navigate).toHaveBeenCalledOnceWith(['/signin']);
    expect(TestBed.inject(SessionService).user()).toBeNull();
    expect(alert()).toBeNull();
  });

  it('emits closed when the professor closes it', () => {
    open();
    el.querySelector<HTMLButtonElement>('button.close-form')!.click();
    expect(closed).toBe(1);
    http.expectNone('/api/1.0/denialReasons');
  });
});
