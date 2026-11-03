import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { SessionService } from '../core/session.service';
import { User } from '../core/models';
import { Shell } from './shell';

@Component({ template: '<p id="page">page</p>' })
class Page {}

const PROFESSOR: User = { id: 1, username: 'prof', firstName: 'Paula', lastName: 'Prof', role: 'Professor' };
const STUDENT: User = { id: 2, username: 'alice', firstName: 'Alice', lastName: 'Student', role: 'Student' };

describe('Shell', () => {
  let fixture: ComponentFixture<Shell>;
  let http: HttpTestingController;
  let router: Router;
  let el: HTMLElement;

  async function setup(user: User): Promise<void> {
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([
          { path: 'signin', component: Page },
          { path: 'partners', component: Page },
        ]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    TestBed.inject(SessionService).signIn({ username: user.username, password: 'secret' }).subscribe();
    http.expectOne('/api/1.0/session').flush(user);
    fixture = TestBed.createComponent(Shell);
    el = fixture.nativeElement as HTMLElement;
    await router.navigateByUrl('/partners');
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  const signOutButton = () => el.querySelector<HTMLButtonElement>('button.sign-out')!;
  const alert = () => el.querySelector('[role=alert]');

  it('shows the application name, the navigation and the screen outlet', async () => {
    await setup(PROFESSOR);
    expect(el.querySelector('.brand-name')?.textContent).toBe('EMA');
    expect(el.querySelector('.brand-tagline')?.textContent).toContain('Gestion des mobilités Erasmus');
    const nav = el.querySelector('nav')!;
    expect(nav.getAttribute('aria-label')).toBe('Navigation principale');
    const link = nav.querySelector('a')!;
    expect(link.textContent).toContain('Partenaires');
    expect(link.getAttribute('href')).toBe('/partners');
    expect(link.classList).toContain('active');
    expect(link.getAttribute('aria-current')).toBe('page');
    expect(el.querySelector('router-outlet')).not.toBeNull();
    expect(alert()).toBeNull();
  });

  it('shows the name and the role of a professor', async () => {
    await setup(PROFESSOR);
    expect(el.querySelector('.user-name')?.textContent).toBe('Paula Prof');
    expect(el.querySelector('.user-role')?.textContent).toBe('Professeur');
  });

  it('shows the name and the role of a student', async () => {
    await setup(STUDENT);
    expect(el.querySelector('.user-name')?.textContent).toBe('Alice Student');
    expect(el.querySelector('.user-role')?.textContent).toBe('Étudiant');
  });

  it('falls back to the username and to the raw role', async () => {
    await setup({ username: 'jdoe', role: 'Visitor' });
    expect(el.querySelector('.user-name')?.textContent).toBe('jdoe');
    expect(el.querySelector('.user-role')?.textContent).toBe('Visitor');
  });

  it('shows nothing for a user without name and role (cannot happen once signed in)', async () => {
    await setup({ username: 'jdoe' });
    TestBed.inject(SessionService).clear();
    fixture.detectChanges();
    expect(el.querySelector('.user-name')?.textContent).toBe('');
    expect(el.querySelector('.user-role')?.textContent).toBe('');
  });

  it('signs out: DELETE /session, then goes to the sign-in page', async () => {
    await setup(PROFESSOR);
    signOutButton().click();
    fixture.detectChanges();

    const req = http.expectOne('/api/1.0/session');
    expect(req.request.method).toBe('DELETE');
    expect(signOutButton().disabled).toBeTrue();
    expect(signOutButton().textContent).toContain('Déconnexion…');

    // a second sign-out while the request is in flight is ignored (the button is disabled, but
    // a keyboard shortcut or a script could still call the handler)
    (fixture.componentInstance as unknown as { signOut(): void }).signOut();
    http.expectNone('/api/1.0/session');

    req.flush(null);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(router.url).toBe('/signin');
    expect(TestBed.inject(SessionService).user()).toBeNull();
    expect(signOutButton().disabled).toBeFalse();
    expect(signOutButton().textContent).toContain('Se déconnecter');
  });

  it('stays signed in and shows the problem when the server could not close the session', async () => {
    await setup(PROFESSOR);
    signOutButton().click();
    http.expectOne('/api/1.0/session').flush(null, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(router.url).toBe('/partners');
    expect(alert()?.textContent).toContain('Une erreur est survenue sur le serveur');
    expect(el.querySelector('.user-name')?.textContent).toBe('Paula Prof');
    expect(signOutButton().disabled).toBeFalse();

    // trying again clears the message
    signOutButton().click();
    fixture.detectChanges();
    expect(alert()).toBeNull();
    http.expectOne('/api/1.0/session').flush(null);
  });
});
