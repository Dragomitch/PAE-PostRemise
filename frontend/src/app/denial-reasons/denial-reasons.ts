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
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiService } from '../api.service';
import { ApiProblem, ClientProblemCode, toApiProblem } from '../core/api-problem';
import { fieldMessage, unmatchedFieldErrors } from '../core/form-errors';
import { DenialReason } from '../core/models';
import { SessionService } from '../core/session.service';
import { DenialReasonForm } from './denial-reason-form';
import { DENIAL_REASON_MAX_LENGTH, denialReasonTooLong } from './denial-reason-labels';

/** Server field of the text (`DenialReasonDto.reason`). */
const REASON_FIELDS = ['reason'];

/**
 * Denial reasons (professor only): the reasons of `GET /api/1.0/denialReasons` in the order of the
 * API (by id), the ones a professor picks when rejecting a mobility choice or cancelling a
 * mobility. A new reason is created with the form of the reject panel (`DenialReasonForm`); the
 * text of a reason is edited in its row (`PUT /denialReasons/{id}` with the version last read). A
 * reason that changed or disappeared meanwhile is refused (409) and the row offers to reload the
 * list.
 */
@Component({
  selector: 'app-denial-reasons',
  imports: [ReactiveFormsModule, DenialReasonForm],
  templateUrl: './denial-reasons.html',
  styleUrl: './denial-reasons.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DenialReasons {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly injector = inject(Injector);

  protected readonly loading = signal(true);
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly reasons = signal<DenialReason[]>([]);
  /** Success message of the last creation or change, announced politely. */
  protected readonly notice = signal<string | null>(null);
  protected readonly creating = signal(false);

  /** The reason whose row holds the edition form. */
  protected readonly editing = signal<DenialReason | null>(null);
  protected readonly saving = signal(false);
  protected readonly editProblem = signal<ApiProblem | null>(null);
  /** The reason changed or disappeared since it was read (409): only a reload helps. */
  protected readonly conflict = computed(() => this.editProblem()?.status === 409);
  protected readonly otherEditErrors = computed(() =>
    unmatchedFieldErrors(this.editProblem(), REASON_FIELDS),
  );
  protected readonly editReason = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required, Validators.maxLength(DENIAL_REASON_MAX_LENGTH)],
  });
  /** The form of the edited row. */
  protected readonly editForm = new FormGroup({ reason: this.editReason });
  private readonly editSubmitted = signal(false);
  protected readonly maxLength = DENIAL_REASON_MAX_LENGTH;

  private readonly editField = viewChild<ElementRef<HTMLInputElement>>('editField');
  private readonly newButton = viewChild<ElementRef<HTMLButtonElement>>('newButton');

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.problem.set(null);
    this.editing.set(null);
    this.editProblem.set(null);
    this.api
      .denialReasons()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (reasons) => this.reasons.set(reasons),
        error: (error: unknown) => {
          this.reasons.set([]);
          this.fail(toApiProblem(error), this.problem);
        },
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

  // --- creation --------------------------------------------------------------------------------

  protected openCreation(): void {
    this.cancelEdit();
    this.notice.set(null);
    this.creating.set(true);
  }

  protected closeCreation(): void {
    this.creating.set(false);
    afterNextRender(() => this.newButton()?.nativeElement.focus(), { injector: this.injector });
  }

  protected created(reason: DenialReason): void {
    this.reasons.update((reasons) => [...reasons, reason]);
    this.creating.set(false);
    this.notice.set(
      $localize`:@@denialReasons.notice.created:La raison « ${reason.reason}:REASON: » a été créée.`,
    );
  }

  // --- edition ---------------------------------------------------------------------------------

  protected edit(reason: DenialReason): void {
    this.creating.set(false);
    this.notice.set(null);
    this.editProblem.set(null);
    this.editSubmitted.set(false);
    this.editReason.reset(reason.reason);
    this.editing.set(reason);
    afterNextRender(() => this.editField()?.nativeElement.focus(), { injector: this.injector });
  }

  protected cancelEdit(): void {
    this.editing.set(null);
    this.editProblem.set(null);
  }

  protected editError(): string | null {
    if (this.editReason.hasError('maxlength')) {
      return denialReasonTooLong();
    }
    return fieldMessage(this.editProblem(), REASON_FIELDS, this.editReason, this.editSubmitted());
  }

  protected save(): void {
    const reason = this.editing();
    if (!reason || this.saving()) {
      return;
    }
    this.editSubmitted.set(true);
    const text = this.editReason.value.trim();
    if (!text) {
      // spaces only: "required", as the API would refuse it (NotBlank)
      this.editReason.setValue('');
    }
    if (this.editReason.invalid) {
      this.editReason.markAsTouched();
      return;
    }
    this.saving.set(true);
    this.editProblem.set(null);
    this.api
      .updateDenialReason({ ...reason, reason: text })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (saved) => {
          this.reasons.update((reasons) => reasons.map((r) => (r.id === saved.id ? saved : r)));
          this.editing.set(null);
          this.notice.set(
            $localize`:@@denialReasons.notice.updated:La raison « ${saved.reason}:REASON: » a été modifiée.`,
          );
        },
        error: (error: unknown) => this.fail(toApiProblem(error), this.editProblem),
      });
  }
}
