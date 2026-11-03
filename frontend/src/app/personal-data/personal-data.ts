import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  LOCALE_ID,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, forkJoin, of, throwError } from 'rxjs';

import { ApiService } from '../api.service';
import { ApiProblem, ClientProblemCode, toApiProblem } from '../core/api-problem';
import { fieldMessage, unmatchedFieldErrors } from '../core/form-errors';
import { Country, NominatedStudent } from '../core/models';
import { SessionService } from '../core/session.service';

/**
 * Maximum lengths of `NominatedStudentDto`, `UserDto` and `AddressDto`. The IBAN and the BIC have
 * none here: they may be typed or pasted with spaces, which are removed before sending.
 */
export const MAX_LENGTHS = {
  lastName: 35,
  firstName: 35,
  email: 255,
  street: 80,
  number: 10,
  region: 60,
  city: 60,
  postalCode: 10,
  phoneNumber: 15,
  bankName: 60,
  cardHolder: 35,
} as const;

/** Country preselected for the nationality and the address of a new record, as in the legacy form. */
export const DEFAULT_COUNTRY = 'BE';

/** Server field names (request body paths) displayed next to each input. */
const SERVER_FIELDS = {
  title: ['title'],
  lastName: ['lastName'],
  firstName: ['firstName'],
  gender: ['gender'],
  birthdate: ['birthdate'],
  nationality: ['nationality', 'nationality.countryCode'],
  street: ['address.street'],
  number: ['address.number'],
  region: ['address.region'],
  city: ['address.city'],
  postalCode: ['address.postalCode'],
  country: ['address', 'address.country', 'address.country.countryCode'],
  phoneNumber: ['phoneNumber'],
  email: ['email'],
  iban: ['iban'],
  bankName: ['bankName'],
  bic: ['bic'],
  cardHolder: ['cardHolder'],
  nbrPassedYears: ['nbrPassedYears'],
} as const satisfies Record<string, readonly string[]>;
type Field = keyof typeof SERVER_FIELDS;
const KNOWN_SERVER_FIELDS = Object.values(SERVER_FIELDS).flat();

/**
 * "Mes données personnelles": the signed-in student's nominated-student record
 * (`GET /api/1.0/nominatedStudents/{their id}`), with the fields and sections of the legacy form.
 * Until the record exists (404) the form starts from the user account (names, e-mail) and the
 * first save creates it (`POST /nominatedStudents`); afterwards every save updates it
 * (`PUT /nominatedStudents/{id}`) with the version last read, so a change made elsewhere in the
 * meantime is refused (409) and the student reloads the record.
 */
@Component({
  selector: 'app-personal-data',
  imports: [ReactiveFormsModule],
  templateUrl: './personal-data.html',
  styleUrl: './personal-data.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PersonalData {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly collator = new Intl.Collator(inject(LOCALE_ID), {
    sensitivity: 'base',
    numeric: true,
  });

  protected readonly maxLengths = MAX_LENGTHS;
  protected readonly titles: readonly { value: string; label: string }[] = [
    { value: 'M', label: $localize`:@@personalData.title.m:Madame` },
    { value: 'Mr', label: $localize`:@@personalData.title.mr:Monsieur` },
    { value: 'Ms', label: $localize`:@@personalData.title.ms:Mademoiselle` },
  ];
  protected readonly genders: readonly { value: string; label: string }[] = [
    { value: 'M', label: $localize`:@@personalData.gender.m:Homme` },
    { value: 'F', label: $localize`:@@personalData.gender.f:Femme` },
    { value: 'O', label: $localize`:@@personalData.gender.o:Autre` },
  ];

  protected readonly loading = signal(true);
  protected readonly loadProblem = signal<ApiProblem | null>(null);
  protected readonly countries = signal<Country[]>([]);
  /** The stored record, `null` while the student never saved their data. */
  protected readonly record = signal<NominatedStudent | null>(null);

  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    title: ['', Validators.required],
    lastName: ['', [Validators.required, Validators.maxLength(MAX_LENGTHS.lastName)]],
    firstName: ['', [Validators.required, Validators.maxLength(MAX_LENGTHS.firstName)]],
    gender: ['', Validators.required],
    birthdate: ['', Validators.required],
    nationality: ['', Validators.required],
    street: ['', [Validators.required, Validators.maxLength(MAX_LENGTHS.street)]],
    number: ['', [Validators.required, Validators.maxLength(MAX_LENGTHS.number)]],
    region: ['', Validators.maxLength(MAX_LENGTHS.region)],
    city: ['', [Validators.required, Validators.maxLength(MAX_LENGTHS.city)]],
    postalCode: ['', [Validators.required, Validators.maxLength(MAX_LENGTHS.postalCode)]],
    country: ['', Validators.required],
    phoneNumber: ['', [Validators.required, Validators.maxLength(MAX_LENGTHS.phoneNumber)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(MAX_LENGTHS.email)]],
    iban: ['', Validators.required],
    bankName: ['', [Validators.required, Validators.maxLength(MAX_LENGTHS.bankName)]],
    bic: ['', Validators.required],
    cardHolder: ['', Validators.maxLength(MAX_LENGTHS.cardHolder)],
    nbrPassedYears: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  protected readonly submitting = signal(false);
  protected readonly submitted = signal(false);
  protected readonly problem = signal<ApiProblem | null>(null);
  /** The record changed elsewhere since it was read (409): only a reload helps. */
  protected readonly conflict = computed(() => this.problem()?.status === 409);
  protected readonly otherErrors = computed(() =>
    unmatchedFieldErrors(this.problem(), KNOWN_SERVER_FIELDS),
  );
  /** Success message of the last save, announced politely. */
  protected readonly notice = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.loadProblem.set(null);
    this.problem.set(null);
    this.notice.set(null);
    this.submitted.set(false);
    forkJoin({ countries: this.api.countries(), record: this.fetchRecord() })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: ({ countries, record }) => {
          this.countries.set([...countries].sort((a, b) => this.collator.compare(a.name, b.name)));
          this.record.set(record);
          this.fill(record);
        },
        error: (error: unknown) => this.fail(toApiProblem(error), this.loadProblem),
      });
  }

  /** The stored record, or `null` when there is none yet (404). */
  private fetchRecord(): Observable<NominatedStudent | null> {
    return this.api
      .nominatedStudent(this.session.user()!.id!)
      .pipe(
        catchError((problem: ApiProblem) =>
          problem.status === 404 ? of(null) : throwError(() => problem),
        ),
      );
  }

  /** Puts the record in the form, or the account's names and e-mail when there is no record yet. */
  private fill(record: NominatedStudent | null): void {
    if (!record) {
      const user = this.session.user()!;
      this.form.reset({
        lastName: user.lastName ?? '',
        firstName: user.firstName ?? '',
        email: user.email ?? '',
        nationality: DEFAULT_COUNTRY,
        country: DEFAULT_COUNTRY,
      });
      return;
    }
    this.form.reset({
      title: record.title,
      lastName: record.lastName,
      firstName: record.firstName,
      gender: record.gender,
      birthdate: record.birthdate,
      nationality: record.nationality.countryCode,
      street: record.address.street,
      number: record.address.number,
      region: record.address.region ?? '',
      city: record.address.city,
      postalCode: record.address.postalCode,
      country: record.address.country.countryCode,
      phoneNumber: record.phoneNumber,
      email: record.email,
      iban: record.iban,
      bankName: record.bankName,
      bic: record.bic,
      cardHolder: record.cardHolder ?? '',
      nbrPassedYears: record.nbrPassedYears,
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

  protected errorFor(field: Field): string | null {
    return fieldMessage(
      this.problem(),
      SERVER_FIELDS[field],
      this.form.controls[field],
      this.submitted(),
    );
  }

  protected describedBy(field: Field, hint: string | null = null): string | null {
    return this.errorFor(field) ? `personal-data-${field}-error` : hint;
  }

  /**
   * The request body: the stored record (or, for a first save, the account's id, username and
   * option, which the API requires) with the values of the form. Text is trimmed; the IBAN and the
   * BIC lose their spaces and are upper-cased, as the API expects them.
   */
  private body(): NominatedStudent {
    const value = this.form.getRawValue();
    const user = this.session.user()!;
    const base = this.record() ?? { id: user.id!, username: user.username, option: user.option };
    return {
      ...base,
      title: value.title,
      lastName: value.lastName.trim(),
      firstName: value.firstName.trim(),
      gender: value.gender,
      birthdate: value.birthdate,
      nationality: { countryCode: value.nationality },
      address: {
        ...this.record()?.address,
        street: value.street.trim(),
        number: value.number.trim(),
        region: value.region.trim(),
        city: value.city.trim(),
        postalCode: value.postalCode.trim(),
        country: { countryCode: value.country },
      },
      phoneNumber: value.phoneNumber.trim(),
      email: value.email.trim(),
      iban: compact(value.iban),
      bankName: value.bankName.trim(),
      bic: compact(value.bic),
      cardHolder: value.cardHolder.trim(),
      nbrPassedYears: value.nbrPassedYears!,
    };
  }

  protected submit(): void {
    if (this.submitting()) {
      return;
    }
    this.submitted.set(true);
    this.notice.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const body = this.body();
    const request = this.record()
      ? this.api.updateNominatedStudent(body)
      : this.api.createNominatedStudent(body);
    this.submitting.set(true);
    this.problem.set(null);
    request
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: (saved) => {
          this.record.set(saved);
          this.fill(saved);
          this.submitted.set(false);
          this.notice.set(
            $localize`:@@personalData.notice.saved:Les modifications ont bien été enregistrées.`,
          );
        },
        error: (error: unknown) => this.fail(toApiProblem(error), this.problem),
      });
  }
}

/** Without spaces and in capitals: `be68 5390 0754 7034` becomes `BE68539007547034`. */
export function compact(text: string): string {
  return text.replace(/\s+/g, '').toUpperCase();
}
