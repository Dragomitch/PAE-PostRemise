import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable, Subscription, finalize, forkJoin, map, of } from 'rxjs';

import { ApiService, mobilityChoicesExportUrl } from '../api.service';
import { ApiProblem, ClientProblemCode, toApiProblem } from '../core/api-problem';
import { fieldMessage, unmatchedFieldErrors } from '../core/form-errors';
import { DenialReason, MobilityChoice, MobilityChoiceFilter } from '../core/models';
import { SessionService } from '../core/session.service';
import { MobilityChoiceForm } from './mobility-choice-form';
import {
  CANCELLATION_REASON_MAX_LENGTH,
  ChoiceState,
  choiceState,
  departureLabel,
  mobilityTypeLabel,
} from './mobility-choice-labels';

/** What the open panel above the table does with the chosen row. */
type Panel = { action: 'cancel' | 'reject'; choice: MobilityChoice };

interface Loaded {
  choices: MobilityChoice[];
  count: number | null;
  mobilityIds: number[];
}

/**
 * Filters whose list may hold choices that became mobilities: `active` leaves them out, and
 * `canceled` and `rejected` only hold closed choices.
 */
const FILTERS_WITH_MOBILITIES: readonly MobilityChoiceFilter[] = ['all', 'passed'];

/**
 * Mobility choices (the applications of the students), for both roles:
 * - a student sees their own choices (`GET /api/1.0/mobilityChoices`, the API ignores the filter),
 *   can create one (`MobilityChoiceForm`) and cancel a pending one with a reason;
 * - a professor sees every choice matching the filter, with the count of the API, the CSV export,
 *   and confirms or rejects (with a denial reason) a pending choice.
 * After every action the list is loaded again, since confirming a choice also rejects the other
 * pending choices of the student for the same term.
 */
@Component({
  selector: 'app-mobility-choices',
  imports: [ReactiveFormsModule, MobilityChoiceForm],
  templateUrl: './mobility-choices.html',
  styleUrl: './mobility-choices.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MobilityChoices {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);

  protected readonly isProfessor = this.session.isProfessor;
  protected readonly filters: readonly { value: MobilityChoiceFilter; label: string }[] = [
    { value: 'active', label: $localize`:@@mobilityChoices.filter.active:Actives` },
    { value: 'all', label: $localize`:@@mobilityChoices.filter.all:Toutes` },
    { value: 'canceled', label: $localize`:@@mobilityChoices.filter.canceled:Annulées` },
    { value: 'rejected', label: $localize`:@@mobilityChoices.filter.rejected:Rejetées` },
    { value: 'passed', label: $localize`:@@mobilityChoices.filter.passed:Années précédentes` },
  ];
  protected readonly filter = signal<MobilityChoiceFilter>('active');
  protected readonly exportUrl = computed(() => mobilityChoicesExportUrl(this.filter()));

  protected readonly loading = signal(true);
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly choices = signal<MobilityChoice[]>([]);
  protected readonly count = signal<number | null>(null);
  /** Choices of the list that became mobilities (they read "confirmed" and have no action). */
  private readonly mobilityIds = signal<ReadonlySet<number>>(new Set());

  /** Success message of the last action, announced politely. */
  protected readonly notice = signal<string | null>(null);
  protected readonly creating = signal(false);

  /** A confirm, cancel or reject request is in flight: every action button is disabled. */
  protected readonly busy = signal(false);
  protected readonly panel = signal<Panel | null>(null);
  protected readonly panelSubmitted = signal(false);
  protected readonly panelProblem = signal<ApiProblem | null>(null);
  /** Failure of a confirmation (no panel to show it in). */
  protected readonly actionProblem = signal<ApiProblem | null>(null);
  protected readonly denialReasons = signal<DenialReason[] | null>(null);
  protected readonly otherPanelErrors = computed(() =>
    unmatchedFieldErrors(this.panelProblem(), ['reason']),
  );

  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly cancelForm = this.fb.group({
    reason: ['', [Validators.required, Validators.maxLength(CANCELLATION_REASON_MAX_LENGTH)]],
  });
  protected readonly rejectForm = this.fb.group({ reason: ['', Validators.required] });
  protected readonly reasonMaxLength = CANCELLATION_REASON_MAX_LENGTH;

  private readonly panelField = viewChild<ElementRef<HTMLElement>>('panelField');
  private loadSubscription?: Subscription;

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loadSubscription?.unsubscribe();
    this.loading.set(true);
    this.problem.set(null);
    this.loadSubscription = this.fetch()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: ({ choices, count, mobilityIds }) => {
          this.choices.set(choices);
          this.count.set(count);
          this.mobilityIds.set(new Set(mobilityIds));
        },
        error: (error: unknown) => {
          this.choices.set([]);
          this.count.set(null);
          this.mobilityIds.set(new Set());
          this.fail(toApiProblem(error), this.problem);
        },
      });
  }

  /**
   * The professor's list comes with the count of the API for the same filter and, when the filter
   * may list choices that became mobilities, the ids of the mobilities. The student's list never
   * holds such choices (the API leaves them out).
   */
  private fetch(): Observable<Loaded> {
    if (!this.isProfessor()) {
      return this.api
        .mobilityChoices()
        .pipe(map((choices) => ({ choices, count: null, mobilityIds: [] })));
    }
    const filter = this.filter();
    return forkJoin({
      choices: this.api.mobilityChoices(filter),
      count: this.api.countMobilityChoices(filter),
      mobilityIds: FILTERS_WITH_MOBILITIES.includes(filter) ? this.api.mobilityIds() : of([]),
    });
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

  protected setFilter(event: Event): void {
    this.filter.set((event.target as HTMLSelectElement).value as MobilityChoiceFilter);
    this.closePanel();
    this.notice.set(null);
    this.actionProblem.set(null);
    this.load();
  }

  protected state(choice: MobilityChoice): ChoiceState {
    return choiceState(choice, this.mobilityIds());
  }

  protected departure(choice: MobilityChoice): string {
    return departureLabel(choice.term, choice.academicYear);
  }

  protected mobilityType(choice: MobilityChoice): string {
    return mobilityTypeLabel(choice.mobilityType);
  }

  /** Flags that failed to load (country without a picture): the name alone is shown. */
  private readonly brokenFlags = signal<ReadonlySet<string>>(new Set());

  protected flagUrl(countryCode: string): string | null {
    return this.brokenFlags().has(countryCode) ? null : `/images/flags/${countryCode}.png`;
  }

  protected flagFailed(countryCode: string): void {
    this.brokenFlags.update((codes) => new Set(codes).add(countryCode));
  }

  protected studentName(choice: MobilityChoice): string {
    return [choice.user.lastName, choice.user.firstName].filter(Boolean).join(' ');
  }

  /** One line that tells which choice the open panel is about. */
  protected summary(choice: MobilityChoice): string {
    const destination = choice.partner?.fullName ?? choice.country?.name ?? '';
    return [
      $localize`:@@mobilityChoices.summary.preference:Préférence ${choice.preferenceOrder}:ORDER:`,
      this.isProfessor() ? this.studentName(choice) : '',
      destination,
      this.departure(choice),
    ]
      .filter(Boolean)
      .join(' · ');
  }

  // --- creation (student) ----------------------------------------------------------------------

  protected openCreation(): void {
    this.closePanel();
    this.notice.set(null);
    this.creating.set(true);
  }

  protected closeCreation(): void {
    this.creating.set(false);
  }

  protected created(): void {
    this.creating.set(false);
    this.notice.set(
      $localize`:@@mobilityChoices.notice.created:La demande de mobilité a été enregistrée.`,
    );
    this.load();
  }

  // --- cancel (student) and reject (professor) -------------------------------------------------

  protected openCancel(choice: MobilityChoice): void {
    this.openPanel({ action: 'cancel', choice });
    this.cancelForm.reset();
  }

  protected openReject(choice: MobilityChoice): void {
    this.openPanel({ action: 'reject', choice });
    this.rejectForm.reset();
    if (this.denialReasons() === null) {
      this.api
        .denialReasons()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (reasons) => this.denialReasons.set(reasons),
          error: (error: unknown) => this.fail(toApiProblem(error), this.panelProblem),
        });
    }
  }

  private openPanel(panel: Panel): void {
    this.creating.set(false);
    this.notice.set(null);
    this.actionProblem.set(null);
    this.panelProblem.set(null);
    this.panelSubmitted.set(false);
    this.panel.set(panel);
    afterNextRender(() => this.panelField()?.nativeElement.focus(), { injector: this.injector });
  }

  protected closePanel(): void {
    this.panel.set(null);
    this.panelProblem.set(null);
  }

  protected reasonError(): string | null {
    const action = this.panel()?.action;
    const control =
      action === 'reject' ? this.rejectForm.controls.reason : this.cancelForm.controls.reason;
    if (control.hasError('maxlength')) {
      return $localize`:@@mobilityChoices.cancel.tooLong:La raison ne peut pas dépasser ${CANCELLATION_REASON_MAX_LENGTH}:MAX: caractères.`;
    }
    return fieldMessage(this.panelProblem(), ['reason'], control, this.panelSubmitted());
  }

  protected submitPanel(): void {
    const panel = this.panel();
    if (!panel || this.busy()) {
      return;
    }
    this.panelSubmitted.set(true);
    const form = panel.action === 'cancel' ? this.cancelForm : this.rejectForm;
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }
    const reason = form.getRawValue().reason;
    const request =
      panel.action === 'cancel'
        ? this.api.cancelMobilityChoice(panel.choice.id, reason.trim())
        : this.api.rejectMobilityChoice(panel.choice.id, Number(reason));
    const notice =
      panel.action === 'cancel'
        ? $localize`:@@mobilityChoices.notice.canceled:La demande de mobilité a été annulée.`
        : $localize`:@@mobilityChoices.notice.rejected:La demande de mobilité a été refusée.`;
    this.panelProblem.set(null);
    this.run(request, notice, this.panelProblem);
  }

  // --- confirm (professor) ---------------------------------------------------------------------

  protected confirm(choice: MobilityChoice): void {
    if (this.busy()) {
      return;
    }
    this.closePanel();
    this.notice.set(null);
    this.actionProblem.set(null);
    this.run(
      this.api.confirmMobilityChoice(choice.id),
      $localize`:@@mobilityChoices.notice.confirmed:La demande de mobilité a été confirmée.`,
      this.actionProblem,
    );
  }

  /** Sends an action; on success closes the panel, says so and loads the list again. */
  private run(
    request: Observable<void>,
    notice: string,
    problem: { set(value: ApiProblem | null): void },
  ): void {
    this.busy.set(true);
    request
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: () => {
          this.panel.set(null);
          this.notice.set(notice);
          this.load();
        },
        error: (error: unknown) => this.fail(toApiProblem(error), problem),
      });
  }
}
