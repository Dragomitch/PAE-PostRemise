import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  LOCALE_ID,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiService } from '../api.service';
import { ApiProblem, ClientProblemCode, toApiProblem } from '../core/api-problem';
import { Mobility } from '../core/models';
import { SessionService } from '../core/session.service';
import { departureLabel, mobilityTypeLabel } from '../mobility-choices/mobility-choice-labels';
import {
  destination,
  mobilityStateClass,
  mobilityStateLabel,
  studentName,
} from './mobility-labels';

/**
 * Mobilities: every mobility of `GET /api/1.0/mobilities` for a professor ("Gestion des
 * mobilités"), the student's own ones for a student ("Mes mobilités"), with the columns of the
 * legacy list (the option for a professor, student, partner, type, programme, departure, state),
 * the latest departures first. The student's name opens the detail (`/mobilities/:id`).
 */
@Component({
  selector: 'app-mobilities',
  imports: [RouterLink],
  templateUrl: './mobilities.html',
  styleUrl: './mobilities.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Mobilities {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly collator = new Intl.Collator(inject(LOCALE_ID), { sensitivity: 'base' });

  protected readonly isProfessor = this.session.isProfessor;
  protected readonly loading = signal(true);
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly mobilities = signal<Mobility[]>([]);

  protected readonly studentName = studentName;
  protected readonly destination = destination;
  protected readonly stateLabel = mobilityStateLabel;
  protected readonly stateClass = mobilityStateClass;

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.problem.set(null);
    this.api
      .mobilities()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (mobilities) =>
          this.mobilities.set([...mobilities].sort((a, b) => this.compare(a, b))),
        error: (error: unknown) => {
          this.mobilities.set([]);
          const problem = toApiProblem(error);
          if (problem.code === ClientProblemCode.UNAUTHENTICATED) {
            this.session.clear();
            void this.router.navigate(['/signin']);
            return;
          }
          this.problem.set(problem);
        },
      });
  }

  /** The latest departure first (year, then February before September), then by student. */
  private compare(a: Mobility, b: Mobility): number {
    return (
      b.academicYear - a.academicYear ||
      b.term - a.term ||
      this.collator.compare(a.nominatedStudent.lastName ?? '', b.nominatedStudent.lastName ?? '') ||
      this.collator.compare(a.nominatedStudent.firstName ?? '', b.nominatedStudent.firstName ?? '')
    );
  }

  protected departure(mobility: Mobility): string {
    return departureLabel(mobility.term, mobility.academicYear);
  }

  protected mobilityType(mobility: Mobility): string {
    return mobilityTypeLabel(mobility.mobilityType);
  }

  /** Flags that failed to load (country without a picture): the name alone is shown. */
  private readonly brokenFlags = signal<ReadonlySet<string>>(new Set());

  protected flagUrl(countryCode: string): string | null {
    return this.brokenFlags().has(countryCode) ? null : `/images/flags/${countryCode}.png`;
  }

  protected flagFailed(countryCode: string): void {
    this.brokenFlags.update((codes) => new Set(codes).add(countryCode));
  }
}
