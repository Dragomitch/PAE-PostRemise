import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';

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
  it('have localized titles and redirect unknown paths to sign-in', () => {
    const byPath = new Map(routes.map((r) => [r.path, r]));
    expect(byPath.get('signin')?.title).toBe('Connexion - EMA');
    expect(byPath.get('signup')?.title).toBe('Inscription - EMA');
    expect(byPath.get('')?.redirectTo).toBe('signin');
    expect(byPath.get('**')?.redirectTo).toBe('signin');
  });
});
