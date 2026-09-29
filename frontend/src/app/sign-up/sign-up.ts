import { Component, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { ApiService } from '../api.service';
import { ApiProblem, toApiProblem } from '../core/api-problem';
import { fieldMessage, matchingPasswords, unmatchedFieldErrors } from '../core/form-errors';
import { SignUpRequest } from '../core/models';

/** Server field names (request body paths) displayed next to each input. */
const SERVER_FIELDS = {
  lastName: ['lastName'],
  firstName: ['firstName'],
  username: ['username'],
  password: ['password'],
  reenterPassword: [],
  email: ['email'],
  option: ['option', 'option.code'],
} as const satisfies Record<string, readonly string[]>;
type Field = keyof typeof SERVER_FIELDS;
const KNOWN_SERVER_FIELDS = Object.values(SERVER_FIELDS).flat();

@Component({
  selector: 'app-sign-up',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './sign-up.html',
  styleUrl: './sign-up.css',
})
export class SignUp {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  /** IPL options (codes of the `options` table). Labels are UI text, hence localized. */
  protected readonly options = [
    { code: 'BIN', label: $localize`:@@option.BIN:Bachelier en informatique de gestion` },
    { code: 'BBM', label: $localize`:@@option.BBM:Bachelier en biologie médicale` },
    { code: 'BCH', label: $localize`:@@option.BCH:Bachelier en chimie` },
    { code: 'BDI', label: $localize`:@@option.BDI:Bachelier en diététique` },
    { code: 'BIM', label: $localize`:@@option.BIM:Bachelier en imagerie médicale` },
  ];

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      lastName: ['', Validators.required],
      firstName: ['', Validators.required],
      username: ['', Validators.required],
      password: ['', Validators.required],
      reenterPassword: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      option: ['', Validators.required],
    },
    { validators: matchingPasswords('password', 'reenterPassword') },
  );
  protected readonly submitting = signal(false);
  protected readonly submitted = signal(false);
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly otherErrors = computed(() => unmatchedFieldErrors(this.problem(), KNOWN_SERVER_FIELDS));

  protected errorFor(field: Field): string | null {
    const groupErrors = field === 'reenterPassword' ? this.form.errors : null;
    return fieldMessage(this.problem(), SERVER_FIELDS[field], this.form.controls[field], this.submitted(), groupErrors);
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
    const { lastName, firstName, username, password, email, option } = this.form.getRawValue();
    const request: SignUpRequest = { username, password, firstName, lastName, email, option: { code: option } };
    this.submitting.set(true);
    this.problem.set(null);
    this.api
      .signup(request)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/signin'], { queryParams: { registered: true } }),
        error: (error: unknown) => this.problem.set(toApiProblem(error)),
      });
  }
}
