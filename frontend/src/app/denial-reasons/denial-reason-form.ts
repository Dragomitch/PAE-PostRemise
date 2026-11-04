import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiService } from '../api.service';
import { ApiProblem, ClientProblemCode, toApiProblem } from '../core/api-problem';
import { fieldMessage, unmatchedFieldErrors } from '../core/form-errors';
import { DenialReason } from '../core/models';
import { SessionService } from '../core/session.service';
import { DENIAL_REASON_MAX_LENGTH, denialReasonTooLong } from './denial-reason-labels';

/** Server field of the text (`DenialReasonDto.reason`). */
const REASON_FIELDS = ['reason'];

/**
 * Creation form of a denial reason (`POST /api/1.0/denialReasons`, professors only): the text,
 * required and 300 characters at most, trimmed before it is sent. The legacy reject dialog offered
 * it in place of the list of reasons; the reject panel of the mobility choices, the cancel panel of
 * a mobility and the denial reasons screen embed it the same way. It takes the focus when it
 * appears, shows the refusals of the API (field errors next to the input) and emits the stored
 * reason, with its id, once the API answered.
 */
@Component({
  selector: 'app-denial-reason-form',
  imports: [ReactiveFormsModule],
  templateUrl: './denial-reason-form.html',
  styleUrl: './denial-reason-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DenialReasonForm {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  /** Prefix of the element ids, so that two screens embedding the form never share one. */
  readonly idPrefix = input('denial-reason-form');
  /** Label of the button that closes the form without creating anything. */
  readonly closeLabel = input($localize`:@@denialReasonForm.close:Annuler`);

  /** Emitted with the new reason (and its id) once the API stored it. */
  readonly created = output<DenialReason>();
  /** Emitted when the professor closes the form without submitting. */
  readonly closed = output<void>();

  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    reason: ['', [Validators.required, Validators.maxLength(DENIAL_REASON_MAX_LENGTH)]],
  });
  protected readonly maxLength = DENIAL_REASON_MAX_LENGTH;
  protected readonly saving = signal(false);
  protected readonly problem = signal<ApiProblem | null>(null);
  private readonly submitted = signal(false);
  protected readonly otherErrors = computed(() =>
    unmatchedFieldErrors(this.problem(), REASON_FIELDS),
  );

  private readonly field = viewChild<ElementRef<HTMLInputElement>>('field');

  constructor() {
    afterNextRender(() => this.field()?.nativeElement.focus(), { injector: inject(Injector) });
  }

  protected reasonError(): string | null {
    const control = this.form.controls.reason;
    if (control.hasError('maxlength')) {
      return denialReasonTooLong();
    }
    return fieldMessage(this.problem(), REASON_FIELDS, control, this.submitted());
  }

  protected submit(): void {
    if (this.saving()) {
      return;
    }
    this.submitted.set(true);
    const control = this.form.controls.reason;
    const reason = control.value.trim();
    if (!reason) {
      // spaces only: "required", as the API would refuse it (NotBlank)
      control.setValue('');
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.problem.set(null);
    this.api
      .createDenialReason(reason)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.saving.set(false)),
      )
      .subscribe({
        next: (created) => this.created.emit(created),
        error: (error: unknown) => {
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

  protected close(): void {
    this.closed.emit();
  }
}
