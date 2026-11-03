import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';

import { Partner, User } from '../core/models';
import { SessionService } from '../core/session.service';
import { Partners, normalize, optionCodes } from './partners';

const PROBLEM_HEADERS = { 'Content-Type': 'application/problem+json' };

const PROFESSOR: User = {
  id: 1,
  username: 'prof',
  firstName: 'Paula',
  lastName: 'Prof',
  role: 'Professor',
};
const STUDENT: User = {
  id: 2,
  username: 'alice',
  firstName: 'Alice',
  lastName: 'Student',
  role: 'Student',
};

function partner(
  id: number,
  fullName: string,
  code: string,
  country: string,
  options: string[],
  flags: Partial<Partner> = {},
): Partner {
  return {
    id,
    fullName,
    address: { country: { countryCode: code, name: country } },
    options: options.map((o) => ({ code: o, departement: 'Département' })),
    official: true,
    archived: false,
    ...flags,
  };
}

const LYON = partner(1, 'Université de Lyon', 'FR', 'France', ['BCH', 'BIN']);
const BERLIN = partner(2, 'Hochschule Berlin', 'DE', 'Allemagne', ['BIN']);
const GENT = partner(3, 'Hogeschool Gent', 'BE', 'Belgique', ['BBM'], {
  official: false,
  archived: true,
});
const ALL = [LYON, BERLIN, GENT];

describe('Partners', () => {
  let fixture: ComponentFixture<Partners>;
  let http: HttpTestingController;
  let router: Router;
  let el: HTMLElement;

  /** Signs `user` in, opens the screen and returns the pending `GET /partners`. */
  function open(user: User): TestRequest {
    TestBed.configureTestingModule({
      imports: [Partners],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    TestBed.inject(SessionService)
      .signIn({ username: user.username, password: 'secret' })
      .subscribe();
    http.expectOne('/api/1.0/session').flush(user);
    fixture = TestBed.createComponent(Partners);
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
    return http.expectOne('/api/1.0/partners');
  }

  function show(user: User, partners: Partner[] = ALL): void {
    open(user).flush({ data: partners });
    fixture.detectChanges();
  }

  afterEach(() => http.verify());

  const names = () =>
    Array.from(el.querySelectorAll('tbody th[scope=row]')).map((c) => c.textContent?.trim());
  const headers = () => Array.from(el.querySelectorAll('thead th'));
  const header = (index: number) => headers()[index] as HTMLElement;
  const sortBy = (index: number) => {
    header(index).querySelector<HTMLButtonElement>('button')!.click();
    fixture.detectChanges();
  };
  const ariaSorts = () => headers().map((h) => h.getAttribute('aria-sort'));
  const alert = () => el.querySelector('[role=alert]');
  const filterInput = () => el.querySelector<HTMLInputElement>('#partners-filter')!;

  function type(text: string): void {
    const input = filterInput();
    input.value = text;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function pick(value: string): void {
    const select = el.querySelector<HTMLSelectElement>('#partners-archived')!;
    select.value = value;
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
  }

  describe('loading and failure', () => {
    it('shows a loading state until the partners arrive', () => {
      const req = open(PROFESSOR);
      expect(req.request.method).toBe('GET');
      expect(el.querySelector('[role=status]')?.textContent).toContain(
        'Chargement des partenaires',
      );
      expect(el.querySelector('table')).toBeNull();

      req.flush({ data: ALL });
      fixture.detectChanges();
      expect(el.textContent).not.toContain('Chargement des partenaires');
      expect(el.querySelector('table')).not.toBeNull();
    });

    it('shows the backend problem in an alert, with a retry button', () => {
      open(PROFESSOR).flush(
        {
          code: 'ACCESS_DENIED',
          status: 403,
          title: 'Interdit',
          detail: 'Accès refusé par le serveur.',
        },
        { status: 403, statusText: 'Forbidden', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(alert()?.textContent).toContain('Accès refusé par le serveur.');
      expect(el.querySelector('table')).toBeNull();
      expect(el.textContent).not.toContain('Chargement');
    });

    it('shows a localized message when the server cannot be reached, never the raw error', () => {
      open(PROFESSOR).error(new ProgressEvent('error'));
      fixture.detectChanges();
      expect(alert()?.textContent).toContain('Impossible de joindre le serveur');
    });

    it('loads again on retry and shows the partners', () => {
      open(PROFESSOR).flush(null, { status: 500, statusText: 'Server Error' });
      fixture.detectChanges();
      expect(alert()?.textContent).toContain('Une erreur est survenue sur le serveur');

      el.querySelector<HTMLButtonElement>('button.retry')!.click();
      fixture.detectChanges();
      expect(alert()).toBeNull();
      expect(el.querySelector('[role=status]')?.textContent).toContain(
        'Chargement des partenaires',
      );
      http.expectOne('/api/1.0/partners').flush({ data: ALL });
      fixture.detectChanges();
      expect(names().length).toBe(3);
    });

    it('goes back to the sign-in page when the session has expired', () => {
      const req = open(PROFESSOR);
      const navigate = spyOn(router, 'navigate').and.resolveTo(true);
      req.flush(
        { code: 'UNAUTHENTICATED', status: 401 },
        { status: 401, statusText: 'Unauthorized', headers: PROBLEM_HEADERS },
      );
      fixture.detectChanges();
      expect(navigate).toHaveBeenCalledOnceWith(['/signin']);
      expect(TestBed.inject(SessionService).user()).toBeNull();
      expect(alert()).toBeNull();
    });

    it('does not update a screen that was closed while the request was in flight', () => {
      const req = open(PROFESSOR);
      fixture.destroy();
      expect(req.cancelled).toBeTrue();
    });
  });

  describe('as a professor', () => {
    beforeEach(() => show(PROFESSOR));

    it('lists every partner with the columns of the legacy view plus the archived flag', () => {
      expect(el.querySelector('h1')?.textContent).toContain('Partenaires');
      expect(headers().map((h) => h.textContent?.replace(/[↕▲▼]/g, '').trim())).toEqual([
        'Nom du partenaire',
        'Pays',
        'Options',
        'Officiel',
        'Archivé',
      ]);
      expect(names()).toEqual(['Hochschule Berlin', 'Hogeschool Gent', 'Université de Lyon']);

      const lyon = Array.from(el.querySelectorAll('tbody tr')).find((r) =>
        r.textContent?.includes('Lyon'),
      )!;
      const cells = lyon.querySelectorAll('td');
      expect(cells[0].textContent).toContain('France');
      expect(cells[0].querySelector('img')?.getAttribute('src')).toBe('/images/flags/FR.png');
      expect(cells[1].textContent?.trim()).toBe('BCH, BIN');
      expect(cells[2].textContent?.trim()).toBe('Oui');
      expect(cells[3].textContent?.trim()).toBe('Non');

      const gent = Array.from(el.querySelectorAll('tbody tr')).find((r) =>
        r.textContent?.includes('Gent'),
      )!;
      expect(gent.querySelectorAll('td')[2].textContent?.trim()).toBe('Non');
      expect(gent.querySelectorAll('td')[3].textContent?.trim()).toBe('Oui');
    });

    it('describes the flag as decoration since the country name is written next to it', () => {
      expect(el.querySelector('img.flag')?.getAttribute('alt')).toBe('');
    });

    it('counts the partners shown', () => {
      expect(el.querySelector('.count')?.textContent?.replace(/\s+/g, ' ').trim()).toBe(
        'Partenaires affichés : 3 sur 3',
      );
      type('berlin');
      expect(el.querySelector('.count')?.textContent?.replace(/\s+/g, ' ').trim()).toBe(
        'Partenaires affichés : 1 sur 3',
      );
    });

    it('is read-only: no form, no edit control', () => {
      expect(el.querySelector('form')).toBeNull();
      expect(el.querySelectorAll('tbody button').length).toBe(0);
    });

    it('hides the picture of a flag that does not load and keeps the country name', () => {
      const berlinRow = () =>
        Array.from(el.querySelectorAll('tbody tr')).find((r) => r.textContent?.includes('Berlin'))!;
      berlinRow().querySelector('img')!.dispatchEvent(new Event('error'));
      fixture.detectChanges();
      expect(berlinRow().querySelector('img')).toBeNull();
      expect(berlinRow().querySelector('td')?.textContent).toContain('Allemagne');
      expect(el.querySelectorAll('img.flag').length).toBe(2);
    });

    describe('sorting', () => {
      it('sorts by name, ascending, to start with', () => {
        expect(ariaSorts()).toEqual(['ascending', 'none', 'none', 'none', 'none']);
        expect(header(0).textContent).toContain('▲');
        expect(header(1).textContent).toContain('↕');
      });

      it('reverses the order when the sorted header is clicked again', () => {
        sortBy(0);
        expect(names()).toEqual(['Université de Lyon', 'Hogeschool Gent', 'Hochschule Berlin']);
        expect(ariaSorts()[0]).toBe('descending');
        expect(header(0).textContent).toContain('▼');

        sortBy(0);
        expect(names()).toEqual(['Hochschule Berlin', 'Hogeschool Gent', 'Université de Lyon']);
        expect(ariaSorts()[0]).toBe('ascending');
      });

      it('sorts by country and starts ascending on a new column', () => {
        sortBy(0); // descending on the name...
        sortBy(1); // ...then another column: back to ascending
        expect(ariaSorts()).toEqual(['none', 'ascending', 'none', 'none', 'none']);
        expect(names()).toEqual(['Hochschule Berlin', 'Hogeschool Gent', 'Université de Lyon']); // Allemagne, Belgique, France
        sortBy(1);
        expect(names()).toEqual(['Université de Lyon', 'Hogeschool Gent', 'Hochschule Berlin']);
      });

      it('sorts by the option codes', () => {
        sortBy(2); // BBM, BCH BIN, BIN
        expect(names()).toEqual(['Hogeschool Gent', 'Université de Lyon', 'Hochschule Berlin']);
      });

      it('sorts by official: unofficial first, then by name', () => {
        sortBy(3);
        expect(names()).toEqual(['Hogeschool Gent', 'Hochschule Berlin', 'Université de Lyon']);
        sortBy(3);
        expect(names()).toEqual(['Hochschule Berlin', 'Université de Lyon', 'Hogeschool Gent']);
      });

      it('sorts by archived: archived last, then by name', () => {
        sortBy(4);
        expect(names()).toEqual(['Hochschule Berlin', 'Université de Lyon', 'Hogeschool Gent']);
        sortBy(4);
        expect(names()).toEqual(['Hogeschool Gent', 'Hochschule Berlin', 'Université de Lyon']);
      });
    });

    describe('filtering', () => {
      it('keeps the partners whose name contains the text, ignoring case', () => {
        type('HO');
        expect(names()).toEqual(['Hochschule Berlin', 'Hogeschool Gent']);
      });

      it('ignores accents on both sides', () => {
        type('universite');
        expect(names()).toEqual(['Université de Lyon']);
        type('Université');
        expect(names()).toEqual(['Université de Lyon']);
      });

      it('ignores spaces around the text and shows everything again when it is cleared', () => {
        type('  lyon ');
        expect(names()).toEqual(['Université de Lyon']);
        type('');
        expect(names().length).toBe(3);
      });

      it('says so when nothing matches, and keeps the filter box', () => {
        type('zzz');
        expect(el.querySelector('table')).toBeNull();
        expect(el.textContent).toContain('Aucun partenaire ne correspond à votre recherche.');
        expect(filterInput().value).toBe('zzz');
      });

      it('keeps the sort order while filtering', () => {
        sortBy(0);
        type('ho');
        expect(names()).toEqual(['Hogeschool Gent', 'Hochschule Berlin']);
      });

      it('offers an archived filter: all, active, archived', () => {
        const select = el.querySelector<HTMLSelectElement>('#partners-archived')!;
        expect(el.querySelector('label[for=partners-archived]')?.textContent).toContain('Statut');
        expect(Array.from(select.options).map((o) => o.value)).toEqual([
          'all',
          'active',
          'archived',
        ]);
        expect(select.value).toBe('all');

        pick('archived');
        expect(names()).toEqual(['Hogeschool Gent']);
        pick('active');
        expect(names()).toEqual(['Hochschule Berlin', 'Université de Lyon']);
        pick('all');
        expect(names().length).toBe(3);
      });

      it('combines the archived filter and the name filter', () => {
        pick('archived');
        type('berlin');
        expect(names()).toEqual([]);
        expect(el.textContent).toContain('Aucun partenaire ne correspond');
        type('gent');
        expect(names()).toEqual(['Hogeschool Gent']);
      });
    });
  });

  describe('as a student', () => {
    beforeEach(() => show(STUDENT, [LYON, BERLIN]));

    it('has no archived column and no archived filter', () => {
      expect(headers().map((h) => h.textContent?.replace(/[↕▲▼]/g, '').trim())).toEqual([
        'Nom du partenaire',
        'Pays',
        'Options',
        'Officiel',
      ]);
      expect(el.querySelector('#partners-archived')).toBeNull();
      expect(el.querySelectorAll('tbody tr:first-child td').length).toBe(3);
      expect(names()).toEqual(['Hochschule Berlin', 'Université de Lyon']);
    });

    it('still sorts and filters by name', () => {
      sortBy(0);
      expect(names()).toEqual(['Université de Lyon', 'Hochschule Berlin']);
      type('lyon');
      expect(names()).toEqual(['Université de Lyon']);
    });

    it('never applies a stale archived filter to a student', () => {
      const component = fixture.componentInstance as unknown as {
        archivedFilter: { set(v: string): void };
      };
      component.archivedFilter.set('archived');
      fixture.detectChanges();
      expect(names().length).toBe(2);
    });
  });

  describe('without any partner', () => {
    it('says there is nothing to display', () => {
      show(STUDENT, []);
      expect(el.querySelector('table')).toBeNull();
      expect(el.textContent).toContain('Aucun partenaire à afficher.');
      expect(el.textContent).not.toContain('ne correspond');
    });
  });
});

describe('partner helpers', () => {
  it('joins the option codes', () => {
    expect(optionCodes(LYON)).toBe('BCH, BIN');
    expect(optionCodes({ ...LYON, options: [] })).toBe('');
  });

  it('normalizes accents and case', () => {
    expect(normalize('Université Réunion')).toBe('universite reunion');
  });
});
