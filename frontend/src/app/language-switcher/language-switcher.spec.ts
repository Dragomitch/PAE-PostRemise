import { Component, LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { LanguageSwitcher } from './language-switcher';

@Component({ template: '' })
class Blank {}

describe('LanguageSwitcher', () => {
  function create(localeId: string): ComponentFixture<LanguageSwitcher> {
    TestBed.configureTestingModule({
      imports: [LanguageSwitcher],
      providers: [
        { provide: LOCALE_ID, useValue: localeId },
        provideRouter([
          { path: 'signin', component: Blank },
          { path: 'signup', component: Blank },
        ]),
      ],
    });
    const fixture = TestBed.createComponent(LanguageSwitcher);
    fixture.detectChanges();
    return fixture;
  }

  function links(fixture: ComponentFixture<LanguageSwitcher>): HTMLAnchorElement[] {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a'));
  }

  it('offers English from the French app, pointing at the same route', async () => {
    const fixture = create('fr');
    expect(links(fixture).map((a) => a.getAttribute('href'))).toEqual(['/en/']);

    await TestBed.inject(Router).navigateByUrl('/signup?registered=true');
    fixture.detectChanges();

    const [link] = links(fixture);
    expect(links(fixture).length).toBe(1);
    expect(link.getAttribute('href')).toBe('/en/signup?registered=true');
    expect(link.getAttribute('hreflang')).toBe('en');
    expect(link.getAttribute('lang')).toBe('en');
    expect(link.textContent?.trim()).toBe('English');
  });

  it('offers French from the English app', async () => {
    const fixture = create('en-US');
    await TestBed.inject(Router).navigateByUrl('/signin');
    fixture.detectChanges();
    expect(links(fixture).map((a) => [a.getAttribute('href'), a.textContent?.trim()])).toEqual([
      ['/fr/signin', 'Français'],
    ]);
  });

  it('is an accessible navigation landmark', () => {
    const fixture = create('fr');
    const nav = (fixture.nativeElement as HTMLElement).querySelector('nav');
    expect(nav?.getAttribute('aria-label')).toBeTruthy();
  });
});
