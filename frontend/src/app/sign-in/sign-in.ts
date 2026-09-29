import { Component, booleanAttribute, computed, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiService } from '../api.service';
import { ApiProblem, toApiProblem } from '../core/api-problem';
import { fieldMessage, unmatchedFieldErrors } from '../core/form-errors';
import { User } from '../core/models';

const FIELDS = ['username', 'password'] as const;
type Field = (typeof FIELDS)[number];

@Component({
  selector: 'app-sign-in',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './sign-in.html',
  styleUrl: './sign-in.css',
})
export class SignIn {
  private readonly api = inject(ApiService);

  /** `?registered=true` after a successful sign-up (bound by withComponentInputBinding). */
  readonly registered = input(false, { transform: booleanAttribute });

  protected readonly form = inject(NonNullableFormBuilder).group({
    username: ['', Validators.required],
    password: ['', Validators.required],
  });
  protected readonly submitting = signal(false);
  protected readonly submitted = signal(false);
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly user = signal<User | null>(null);
  protected readonly otherErrors = computed(() => unmatchedFieldErrors(this.problem(), FIELDS));

  protected errorFor(field: Field): string | null {
    return fieldMessage(this.problem(), [field], this.form.controls[field], this.submitted());
  }

  protected submit(): void {
    if (this.submitting()) {
      return;
    }
    this.submitted.set(true);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.problem.set(null);
    this.user.set(null);
    this.api
      .login(this.form.getRawValue())
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: (user) => this.user.set(user),
        error: (error: unknown) => this.problem.set(toApiProblem(error)),
      });
  }
}
