import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

import { ApiProblem } from './api-problem';

/** Localized message for the first client-side validation error of a control, or `null`. */
export function clientErrorMessage(errors: ValidationErrors | null): string | null {
  if (!errors) {
    return null;
  }
  if (errors['required']) {
    return $localize`:@@form.error.required:Ce champ est obligatoire.`;
  }
  if (errors['email']) {
    return $localize`:@@form.error.email:Saisissez une adresse e-mail valide.`;
  }
  if (errors['passwordMismatch']) {
    return $localize`:@@form.error.passwordMismatch:Les mots de passe ne correspondent pas.`;
  }
  return $localize`:@@form.error.invalid:Valeur invalide.`;
}

/** Group validator: `password` and `confirmation` controls must hold the same value. */
export function matchingPasswords(password: string, confirmation: string): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const confirmationValue = group.get(confirmation)?.value;
    return confirmationValue && group.get(password)?.value !== confirmationValue
      ? { passwordMismatch: true }
      : null;
  };
}

/**
 * Error shown next to an input: the server's field error wins (it reflects the last submit),
 * otherwise the client-side validation error once the control was touched or the form submitted.
 */
export function fieldMessage(
  problem: ApiProblem | null,
  serverFields: readonly string[],
  control: AbstractControl,
  submitted: boolean,
  extraErrors: ValidationErrors | null = null,
): string | null {
  for (const field of serverFields) {
    const serverMessage = problem?.fieldError(field);
    if (serverMessage) {
      return serverMessage;
    }
  }
  if (!control.touched && !submitted) {
    return null;
  }
  return clientErrorMessage(control.errors ?? extraErrors);
}

/** Server field errors that do not belong to any input of the form (shown in the alert). */
export function unmatchedFieldErrors(problem: ApiProblem | null, knownFields: readonly string[]): string[] {
  return (problem?.errors ?? []).filter((e) => !knownFields.includes(e.field)).map((e) => e.message);
}
