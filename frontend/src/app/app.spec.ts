import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';
import { authGuard } from './core/auth.guard';
import { Partners } from './partners/partners';
import { PersonalData } from './personal-data/personal-data';
import { Shell } from './shell/shell';
import { Users } from './users/users';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders the language switcher and the router outlet', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('header app-language-switcher')).not.toBeNull();
    expect(compiled.querySelector('main router-outlet')).not.toBeNull();
  });
});

describe('routes', () => {
  const byPath = new Map(routes.map((r) => [r.path, r]));
  const shell = routes.find((r) => r.path === '')!;
  const partners = shell.children!.find((r) => r.path === 'partners')!;

  it('have localized titles', () => {
    expect(byPath.get('signin')?.title).toBe('Connexion - EMA');
    expect(byPath.get('signup')?.title).toBe('Inscription - EMA');
    expect(partners.title).toBe('Partenaires - EMA');
  });

  it('keep the authenticated screens behind the guard, inside the shell', () => {
    expect(shell.component).toBe(Shell);
    expect(shell.canActivate).toEqual([authGuard]);
    expect(partners.component).toBe(Partners);
    expect(partners.canActivate).toBeUndefined(); // guarded once, by the shell
    expect(byPath.get('signin')?.canActivate).toBeUndefined();
    expect(byPath.get('signup')?.canActivate).toBeUndefined();
  });

  it('send the root and unknown paths to the partners (the guard sends anonymous visitors to sign-in)', () => {
    expect(shell.children!.find((r) => r.path === '')?.redirectTo).toBe('partners');
    expect(byPath.get('**')?.redirectTo).toBe('partners');
  });

  it('limit the personal data to the students and the users to the professors', () => {
    const personalData = shell.children!.find((r) => r.path === 'personal-data')!;
    const users = shell.children!.find((r) => r.path === 'users')!;
    expect(personalData.component).toBe(PersonalData);
    expect(personalData.title).toBe('Mes données personnelles - EMA');
    expect(personalData.canActivate?.length).toBe(1);
    expect(users.component).toBe(Users);
    expect(users.title).toBe('Utilisateurs - EMA');
    expect(users.canActivate?.length).toBe(1);
  });
});
