import { ChangeDetectionStrategy, Component, DestroyRef, LOCALE_ID, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiService } from '../api.service';
import { ApiProblem, ClientProblemCode, toApiProblem } from '../core/api-problem';
import { Partner } from '../core/models';
import { SessionService } from '../core/session.service';

export type SortColumn = 'name' | 'country' | 'options' | 'official' | 'archived';
export type SortDirection = 'asc' | 'desc';
/** Professors can narrow the list on the archived flag; students never see archived partners. */
export type ArchivedFilter = 'all' | 'active' | 'archived';

/** Sort key of each column; booleans sort "no" before "yes". */
const SORT_KEYS: Record<SortColumn, (partner: Partner) => string> = {
  name: (p) => p.fullName,
  country: (p) => p.address.country.name,
  options: (p) => optionCodes(p),
  official: (p) => (p.official ? '1' : '0'),
  archived: (p) => (p.archived ? '1' : '0'),
};

/** Codes of the options a partner offers, in the order of the API (sorted by code). */
export function optionCodes(partner: Partner): string {
  return partner.options.map((option) => option.code).join(', ');
}

/** Lower case without accents, so that "universite" finds "Université". */
export function normalize(text: string): string {
  return text.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase();
}

/**
 * Read-only list of the partners the signed-in user may see, with the columns of the legacy
 * "Partenaires" view. Sorting and filtering are done in the browser on the list loaded once from
 * `GET /api/1.0/partners`: a professor's list already holds the archived partners, so the
 * archived filter needs no second request (the `filter=archived` query of the API requires a
 * name and serves the "restore a partner" search of the creation form instead).
 */
@Component({
  selector: 'app-partners',
  templateUrl: './partners.html',
  styleUrl: './partners.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Partners {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly collator = new Intl.Collator(inject(LOCALE_ID), { sensitivity: 'base', numeric: true });

  protected readonly isProfessor = this.session.isProfessor;
  protected readonly loading = signal(true);
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly partners = signal<Partner[]>([]);

  protected readonly nameFilter = signal('');
  protected readonly archivedFilter = signal<ArchivedFilter>('all');
  protected readonly sortColumn = signal<SortColumn>('name');
  protected readonly sortDirection = signal<SortDirection>('asc');

  /** Partners matching the filters, in the order chosen by the headers. */
  protected readonly rows = computed(() => {
    const text = normalize(this.nameFilter().trim());
    const archived = this.isProfessor() ? this.archivedFilter() : 'all';
    const key = SORT_KEYS[this.sortColumn()];
    const sign = this.sortDirection() === 'asc' ? 1 : -1;
    return this.partners()
      .filter((p) => normalize(p.fullName).includes(text))
      .filter((p) => archived === 'all' || p.archived === (archived === 'archived'))
      .sort((a, b) => sign * this.collator.compare(key(a), key(b)) || this.collator.compare(a.fullName, b.fullName));
  });

  /** Flags that failed to load (country without a picture): the name alone is shown. */
  private readonly brokenFlags = signal<ReadonlySet<string>>(new Set());

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.problem.set(null);
    this.api
      .partners()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (partners) => this.partners.set(partners),
        error: (error: unknown) => this.fail(toApiProblem(error)),
      });
  }

  private fail(problem: ApiProblem): void {
    this.partners.set([]);
    if (problem.code === ClientProblemCode.UNAUTHENTICATED) {
      // the session expired while the screen was open: back to the sign-in page
      this.session.clear();
      void this.router.navigate(['/signin']);
      return;
    }
    this.problem.set(problem);
  }

  protected sortBy(column: SortColumn): void {
    if (this.sortColumn() === column) {
      this.sortDirection.update((direction) => (direction === 'asc' ? 'desc' : 'asc'));
    } else {
      this.sortColumn.set(column);
      this.sortDirection.set('asc');
    }
  }

  protected ariaSort(column: SortColumn): 'ascending' | 'descending' | 'none' {
    if (this.sortColumn() !== column) {
      return 'none';
    }
    return this.sortDirection() === 'asc' ? 'ascending' : 'descending';
  }

  protected sortIndicator(column: SortColumn): string {
    const sort = this.ariaSort(column);
    return sort === 'none' ? '↕' : sort === 'ascending' ? '▲' : '▼';
  }

  protected setNameFilter(event: Event): void {
    this.nameFilter.set((event.target as HTMLInputElement).value);
  }

  protected setArchivedFilter(event: Event): void {
    this.archivedFilter.set((event.target as HTMLSelectElement).value as ArchivedFilter);
  }

  protected options(partner: Partner): string {
    return optionCodes(partner);
  }

  protected flagUrl(countryCode: string): string | null {
    return this.brokenFlags().has(countryCode) ? null : `/images/flags/${countryCode}.png`;
  }

  protected flagFailed(countryCode: string): void {
    this.brokenFlags.update((codes) => new Set(codes).add(countryCode));
  }
}
