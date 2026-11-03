import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  LOCALE_ID,
  OnInit,
  afterNextRender,
  computed,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Observable, finalize, switchMap } from 'rxjs';

import { ApiService, mobilityExportUrl } from '../api.service';
import { ApiProblem, ClientProblemCode, toApiProblem } from '../core/api-problem';
import { fieldMessage, unmatchedFieldErrors } from '../core/form-errors';
import {
  DenialReason,
  MOBILITY_STATES,
  Mobility,
  MobilityDocument,
  MobilityDocumentFilter,
} from '../core/models';
import { SessionService } from '../core/session.service';
import { departureLabel, mobilityTypeLabel } from '../mobility-choices/mobility-choice-labels';
import { destination, mobilityStateClass, mobilityStateLabel, studentName } from './mobility-labels';

/** A state change the professor asked for, waiting for their confirmation in the panel. */
export type MobilityAction =
  | { kind: 'proEco' }
  | { kind: 'secondSoftware' }
  | { kind: 'document'; document: MobilityDocument }
  | { kind: 'payment' }
  | { kind: 'cancel' };

/** The payment the API records next, if the state of the mobility expects one. */
export type DuePayment = 'first' | 'second' | null;

/** Server field of the cancellation's denial reason (query parameter of `PUT .../cancel`). */
const REASON_FIELDS = ['denialReason'];

/**
 * Detail of a mobility (`GET /api/1.0/mobilities/{id}`), the legacy "editor" of the mobilities
 * list: general information, the student, the documents (departure and return), the software
 * encodings, the two payments and the cancellation.
 *
 * A professor confirms the encodings, the documents and the payments (the first one in "A payer",
 * the second in "Solde à payer", as the legacy detail offered them) and cancels the mobility with a
 * denial reason; every change goes through a confirmation panel, sends the version last read and
 * reads the mobility again. A stale version is refused (409) and the panel offers to reload it. The
 * CSV exports of the documents are links the browser opens itself. A student sees the same detail
 * without any button, the encodings and the exports left out.
 */
@Component({
  selector: 'app-mobility-detail',
  imports: [RouterLink, ReactiveFormsModule],
  templateUrl: './mobility-detail.html',
  styleUrl: './mobility-detail.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MobilityDetail implements OnInit {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);
  private readonly dateFormat = new Intl.DateTimeFormat(inject(LOCALE_ID), {
    dateStyle: 'medium',
  });

  /** The `:id` of the route (component input binding). */
  readonly id = input.required<string>();

  protected readonly isProfessor = this.session.isProfessor;
  protected readonly loading = signal(true);
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly mobility = signal<Mobility | null>(null);

  protected readonly pending = signal<MobilityAction | null>(null);
  protected readonly busy = signal(false);
  protected readonly actionProblem = signal<ApiProblem | null>(null);
  /** The mobility changed since it was read (409): only a reload helps. */
  protected readonly conflict = computed(() => this.actionProblem()?.status === 409);
  protected readonly otherErrors = computed(() =>
    unmatchedFieldErrors(this.actionProblem(), REASON_FIELDS),
  );
  /** Success message of the last change, announced politely. */
  protected readonly notice = signal<string | null>(null);

  protected readonly denialReasons = signal<DenialReason[] | null>(null);
  protected readonly reason = new FormControl('', {
    nonNullable: true,
    validators: Validators.required,
  });
  private readonly submitted = signal(false);
  /** Error next to the denial reason: the server's, or "required" once the panel was submitted. */
  protected reasonError(): string | null {
    return fieldMessage(this.actionProblem(), REASON_FIELDS, this.reason, this.submitted());
  }

  private readonly panelField = viewChild<ElementRef<HTMLElement>>('panelField');

  protected readonly studentName = studentName;
  protected readonly destination = destination;
  protected readonly stateLabel = mobilityStateLabel;
  protected readonly stateClass = mobilityStateClass;

  protected readonly departureDocuments = computed(() => this.documents('D'));
  protected readonly returnDocuments = computed(() => this.documents('R'));
  protected readonly cancelled = computed(
    () => this.mobility()?.state === MOBILITY_STATES.cancelled,
  );
  /** Documents and encodings can be confirmed by a professor until the mobility is cancelled. */
  protected readonly canConfirm = computed(() => this.isProfessor() && !this.cancelled());
  protected readonly canCancel = computed(() => {
    const state = this.mobility()?.state;
    return (
      this.isProfessor() && state !== MOBILITY_STATES.closed && state !== MOBILITY_STATES.cancelled
    );
  });
  /** The payments need the student's IBAN and BIC, as the legacy detail checked. */
  protected readonly bankDetailsComplete = computed(() => {
    const student = this.mobility()?.nominatedStudent;
    return !!student?.iban && !!student?.bic;
  });
  protected readonly duePayment = computed<DuePayment>(() => {
    const m = this.mobility();
    if (!m?.firstPaymentRequestDate) {
      return m?.state === MOBILITY_STATES.toBePaid ? 'first' : null;
    }
    return !m.secondPaymentRequestDate && m.state === MOBILITY_STATES.balanceToBePaid
      ? 'second'
      : null;
  });
  protected readonly canPay = computed(
    () => this.isProfessor() && this.bankDetailsComplete() && this.duePayment() !== null,
  );
  protected readonly panelTitle = computed(() => {
    const action = this.pending();
    return action ? this.title(action) : '';
  });
  protected readonly panelQuestion = computed(() => {
    const action = this.pending();
    const m = this.mobility();
    return action && m ? this.question(action, m) : '';
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.problem.set(null);
    this.pending.set(null);
    this.actionProblem.set(null);
    this.api
      .mobility(Number(this.id()))
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (mobility) => this.mobility.set(mobility),
        error: (error: unknown) => {
          this.mobility.set(null);
          this.fail(toApiProblem(error), this.problem);
        },
      });
  }

  private documents(category: 'D' | 'R'): MobilityDocument[] {
    return (this.mobility()?.documents ?? []).filter((d) => d.category === category);
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

  protected departure(mobility: Mobility): string {
    return departureLabel(mobility.term, mobility.academicYear);
  }

  protected mobilityType(mobility: Mobility): string {
    return mobilityTypeLabel(mobility.mobilityType);
  }

  protected secondSoftware(mobility: Mobility): string {
    return (
      mobility.programme.externalSoftName ||
      $localize`:@@mobility.encoding.secondSoftware:Logiciel du programme`
    );
  }

  protected professorInCharge(mobility: Mobility): string {
    const professor = mobility.professorInCharge;
    return [professor?.firstName, professor?.lastName].filter(Boolean).join(' ');
  }

  /** The reason of a cancelled mobility: the student's own words or the professor's denial reason. */
  protected cancellationReason(mobility: Mobility): string {
    return mobility.cancellationReason || mobility.denialReason?.reason || '';
  }

  /** `yyyy-MM-dd` of a server date, for the `datetime` attribute. */
  protected day(date: string): string {
    return date.substring(0, 10);
  }

  /** The day of a server date (`yyyy-MM-dd...`, local time of the server) in the UI language. */
  protected formatDay(date: string): string {
    const [year, month, day] = this.day(date).split('-').map(Number);
    return this.dateFormat.format(new Date(year, month - 1, day));
  }

  protected exportUrl(mobility: Mobility, filter?: MobilityDocumentFilter): string {
    return mobilityExportUrl(mobility.id, filter);
  }

  /** File name the legacy export gave: `mobilite_First_Last.csv`. */
  protected exportFileName(mobility: Mobility): string {
    return `mobilite_${studentName(mobility).replace(/\s+/g, '_')}.csv`;
  }

  /** Opens the confirmation panel of an action and moves the focus into it. */
  protected ask(action: MobilityAction): void {
    this.notice.set(null);
    this.actionProblem.set(null);
    this.submitted.set(false);
    this.pending.set(action);
    if (action.kind === 'cancel') {
      this.reason.reset('');
      this.loadDenialReasons();
    }
    afterNextRender(() => this.panelField()?.nativeElement.focus(), { injector: this.injector });
  }

  protected closePanel(): void {
    this.pending.set(null);
    this.actionProblem.set(null);
  }

  private loadDenialReasons(): void {
    if (this.denialReasons()) {
      return;
    }
    this.api
      .denialReasons()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (reasons) => this.denialReasons.set(reasons),
        error: (error: unknown) => this.fail(toApiProblem(error), this.actionProblem),
      });
  }

  /** Sends the pending action with the version last read, then reads the mobility again. */
  protected confirm(): void {
    const action = this.pending();
    const mobility = this.mobility();
    if (!action || !mobility || this.busy()) {
      return;
    }
    if (action.kind === 'cancel') {
      this.submitted.set(true);
      this.reason.markAsTouched();
      if (this.reason.invalid) {
        return;
      }
    }
    this.busy.set(true);
    this.actionProblem.set(null);
    this.request(action, mobility)
      .pipe(
        switchMap(() => this.api.mobility(mobility.id)),
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: (fresh) => {
          this.mobility.set(fresh);
          this.pending.set(null);
          this.notice.set(this.doneMessage(action, mobility));
        },
        error: (error: unknown) => this.fail(toApiProblem(error), this.actionProblem),
      });
  }

  private request(action: MobilityAction, mobility: Mobility): Observable<void> {
    const { id, version } = mobility;
    switch (action.kind) {
      case 'proEco':
        return this.api.confirmProEcoEncoding(id, version);
      case 'secondSoftware':
        return this.api.confirmSecondSoftwareEncoding(id, version);
      case 'document':
        return this.api.confirmMobilityDocument(id, action.document.id, version);
      case 'payment':
        return this.api.confirmMobilityPayment(id, version);
      case 'cancel':
        return this.api.cancelMobility(id, version, Number(this.reason.value));
    }
  }

  private title(action: MobilityAction): string {
    switch (action.kind) {
      case 'proEco':
      case 'secondSoftware':
        return $localize`:@@mobility.panel.encoding.title:Confirmer un encodage`;
      case 'document':
        return $localize`:@@mobility.panel.document.title:Confirmer un document`;
      case 'payment':
        return this.duePayment() === 'second'
          ? $localize`:@@mobility.panel.payment.second.title:Confirmer le deuxième paiement`
          : $localize`:@@mobility.panel.payment.first.title:Confirmer le premier paiement`;
      case 'cancel':
        return $localize`:@@mobility.panel.cancel.title:Annuler la mobilité`;
    }
  }

  private question(action: MobilityAction, mobility: Mobility): string {
    const student = studentName(mobility);
    switch (action.kind) {
      case 'proEco':
        return $localize`:@@mobility.panel.proEco.question:La mobilité de ${student}:STUDENT: est-elle encodée dans ProEco ?`;
      case 'secondSoftware':
        return $localize`:@@mobility.panel.secondSoftware.question:La mobilité de ${student}:STUDENT: est-elle encodée dans ${this.secondSoftware(mobility)}:SOFTWARE: ?`;
      case 'document':
        return $localize`:@@mobility.panel.document.question:Le document « ${action.document.name}:DOCUMENT: » de ${student}:STUDENT: est-il complété ? Cette action est définitive.`;
      case 'payment':
        return this.duePayment() === 'second'
          ? $localize`:@@mobility.panel.payment.second.question:Confirmer le paiement à l'arrivée de ${student}:STUDENT: ? Cette action est définitive.`
          : $localize`:@@mobility.panel.payment.first.question:Confirmer le paiement au départ de ${student}:STUDENT: ? Cette action est définitive.`;
      case 'cancel':
        return $localize`:@@mobility.panel.cancel.question:Annuler la mobilité de ${student}:STUDENT: ? Cette action est définitive.`;
    }
  }

  private doneMessage(action: MobilityAction, mobility: Mobility): string {
    switch (action.kind) {
      case 'proEco':
        return $localize`:@@mobility.notice.proEco:L'encodage dans ProEco est confirmé.`;
      case 'secondSoftware':
        return $localize`:@@mobility.notice.secondSoftware:L'encodage dans ${this.secondSoftware(mobility)}:SOFTWARE: est confirmé.`;
      case 'document':
        return $localize`:@@mobility.notice.document:Le document « ${action.document.name}:DOCUMENT: » est complété.`;
      case 'payment':
        return $localize`:@@mobility.notice.payment:Le paiement est confirmé.`;
      case 'cancel':
        return $localize`:@@mobility.notice.cancel:La mobilité est annulée.`;
    }
  }
}
