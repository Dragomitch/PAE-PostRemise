import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  LOCALE_ID,
  afterNextRender,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiService } from '../api.service';
import { ApiProblem, ClientProblemCode, toApiProblem } from '../core/api-problem';
import { ROLE_PROFESSOR, User } from '../core/models';
import { roleLabel } from '../core/roles';
import { SessionService } from '../core/session.service';

/** "First Last", as the legacy list wrote the name; the username when the account has no name. */
export function fullName(user: User): string {
  return [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username;
}

/**
 * Users (professor only): every account of `GET /api/1.0/users` with the columns of the legacy
 * list (name, username, e-mail, option, role) and the registration date, sorted by name. A
 * student can be promoted to professor (`PUT /users/{id}/promote`) after a confirmation; the row
 * then reads "Professeur". A refusal of the API is shown in the confirmation panel.
 */
@Component({
  selector: 'app-users',
  templateUrl: './users.html',
  styleUrl: './users.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Users {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly locale = inject(LOCALE_ID);
  private readonly collator = new Intl.Collator(this.locale, { sensitivity: 'base', numeric: true });
  private readonly dateFormat = new Intl.DateTimeFormat(this.locale, { dateStyle: 'medium' });

  protected readonly loading = signal(true);
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly users = signal<User[]>([]);

  /** The user waiting for the professor to confirm their promotion. */
  protected readonly pending = signal<User | null>(null);
  protected readonly promoting = signal(false);
  protected readonly promoteProblem = signal<ApiProblem | null>(null);
  /** Success message of the last promotion, announced politely. */
  protected readonly notice = signal<string | null>(null);

  private readonly confirmButton = viewChild<ElementRef<HTMLButtonElement>>('confirmButton');

  protected readonly fullName = fullName;
  protected readonly roleLabel = roleLabel;

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.problem.set(null);
    this.api
      .users()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (users) => this.users.set([...users].sort((a, b) => this.compare(a, b))),
        error: (error: unknown) => {
          this.users.set([]);
          this.fail(toApiProblem(error), this.problem);
        },
      });
  }

  /** By last name, then first name, then username. */
  private compare(a: User, b: User): number {
    return (
      this.collator.compare(a.lastName ?? '', b.lastName ?? '') ||
      this.collator.compare(a.firstName ?? '', b.firstName ?? '') ||
      this.collator.compare(a.username, b.username)
    );
  }

  /** Shows the problem, or goes back to the sign-in page when the session expired. */
  private fail(problem: ApiProblem, target: { set(value: ApiProblem | null): void }): void {
    if (problem.code === ClientProblemCode.UNAUTHENTICATED) {
      this.session.clear();
      void this.router.navigate(['/signin']);
      return;
    }
    target.set(problem);
  }

  protected isProfessor(user: User): boolean {
    return user.role === ROLE_PROFESSOR;
  }

  /** The day of the registration (`yyyy-MM-dd...`, local time of the server), in the UI language. */
  protected registered(user: User): string {
    if (!user.registrationDate) {
      return '';
    }
    const [year, month, day] = user.registrationDate.substring(0, 10).split('-').map(Number);
    return this.dateFormat.format(new Date(year, month - 1, day));
  }

  protected askPromotion(user: User): void {
    this.notice.set(null);
    this.promoteProblem.set(null);
    this.pending.set(user);
    afterNextRender(() => this.confirmButton()?.nativeElement.focus(), { injector: this.injector });
  }

  protected cancelPromotion(): void {
    this.pending.set(null);
    this.promoteProblem.set(null);
  }

  protected confirmPromotion(): void {
    const user = this.pending();
    if (!user || this.promoting()) {
      return;
    }
    this.promoting.set(true);
    this.promoteProblem.set(null);
    this.api
      .promoteUser(user.id!)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.promoting.set(false)),
      )
      .subscribe({
        next: () => {
          this.users.update((users) => users.map((u) => (u.id === user.id ? { ...u, role: ROLE_PROFESSOR } : u)));
          this.pending.set(null);
          this.notice.set($localize`:@@users.notice.promoted:${fullName(user)}:NAME: est maintenant professeur.`);
        },
        error: (error: unknown) => this.fail(toApiProblem(error), this.promoteProblem),
      });
  }
}
