import { FormControl, FormGroup } from '@angular/forms';

import { ApiProblem } from './api-problem';
import { clientErrorMessage, fieldMessage, matchingPasswords, unmatchedFieldErrors } from './form-errors';

function problemWith(errors: { field: string; message: string }[]): ApiProblem {
  return new ApiProblem({ status: 400, code: 'VALIDATION_FAILED', title: 't', detail: 'd', errors, fromServer: true });
}

describe('form errors', () => {
  describe('clientErrorMessage', () => {
    it('maps known validators and falls back to a generic message', () => {
      expect(clientErrorMessage(null)).toBeNull();
      expect(clientErrorMessage({ required: true })).toBe('Ce champ est obligatoire.');
      expect(clientErrorMessage({ email: true })).toBe('Saisissez une adresse e-mail valide.');
      expect(clientErrorMessage({ passwordMismatch: true })).toBe('Les mots de passe ne correspondent pas.');
      expect(clientErrorMessage({ minlength: { requiredLength: 3 } })).toBe('Valeur invalide.');
    });
  });

  describe('matchingPasswords', () => {
    const group = () =>
      new FormGroup(
        { password: new FormControl(''), confirmation: new FormControl('') },
        { validators: matchingPasswords('password', 'confirmation') },
      );

    it('flags different values once the confirmation is filled', () => {
      const form = group();
      expect(form.errors).toBeNull();
      form.setValue({ password: 'a', confirmation: 'b' });
      expect(form.errors).toEqual({ passwordMismatch: true });
      form.setValue({ password: 'b', confirmation: 'b' });
      expect(form.errors).toBeNull();
    });
  });

  describe('fieldMessage', () => {
    it('prefers the server field error, trying each server field name', () => {
      const control = new FormControl('', { nonNullable: true });
      const problem = problemWith([{ field: 'option.code', message: 'Option inconnue' }]);
      expect(fieldMessage(problem, ['option', 'option.code'], control, false)).toBe('Option inconnue');
    });

    it('shows client errors only once touched or submitted', () => {
      const control = new FormControl('', { nonNullable: true, validators: (c) => (c.value ? null : { required: true }) });
      expect(fieldMessage(null, ['x'], control, false)).toBeNull();
      expect(fieldMessage(null, ['x'], control, true)).toBe('Ce champ est obligatoire.');
      control.markAsTouched();
      expect(fieldMessage(problemWith([]), ['x'], control, false)).toBe('Ce champ est obligatoire.');
      control.setValue('ok');
      expect(fieldMessage(null, ['x'], control, true)).toBeNull();
      expect(fieldMessage(null, [], control, true, { passwordMismatch: true })).toBe(
        'Les mots de passe ne correspondent pas.',
      );
    });
  });

  describe('unmatchedFieldErrors', () => {
    it('returns messages of fields the form does not display', () => {
      expect(unmatchedFieldErrors(null, ['a'])).toEqual([]);
      const problem = problemWith([
        { field: 'a', message: 'A' },
        { field: 'role', message: 'Rôle invalide' },
      ]);
      expect(unmatchedFieldErrors(problem, ['a'])).toEqual(['Rôle invalide']);
    });
  });
});
