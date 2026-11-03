import { ChangeDetectionStrategy, Component, DestroyRef, LOCALE_ID, computed, inject, output, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';

import { ApiService } from '../api.service';
import { ApiProblem, ClientProblemCode, toApiProblem } from '../core/api-problem';
import { fieldMessage, unmatchedFieldErrors } from '../core/form-errors';
import { MobilityChoice, MobilityChoiceRequest, MobilityType, Partner, Programme, ProgrammeCountry } from '../core/models';
import { SessionService } from '../core/session.service';
import { departureLabel } from './mobility-choice-labels';

/** Highest preference order (`MobilityChoiceDto.MAX_ORDER_CHOICE`). */
export const MAX_PREFERENCE_ORDER = 3;

/** A departure the student can pick: the legacy form offered three years, February then September. */
export interface Departure {
  value: string;
  term: number;
  academicYear: number;
  label: string;
}

/** The departures of the legacy form, from the given year on: "2 2026" (Février 2026), "1 2026"... */
export function departures(fromYear: number, years = 3): Departure[] {
  const result: Departure[] = [];
  for (let academicYear = fromYear; academicYear < fromYear + years; academicYear++) {
    for (const term of [2, 1]) {
      result.push({ value: `${term} ${academicYear}`, term, academicYear, label: departureLabel(term, academicYear) });
    }
  }
  return result;
}

/** Server field names (request body paths) displayed next to each input. */
const SERVER_FIELDS = {
  programme: ['programme', 'programme.id'],
  country: ['country', 'country.countryCode'],
  partner: ['partner', 'partner.id'],
  mobilityType: ['mobilityType'],
  preferenceOrder: ['preferenceOrder'],
  departure: ['academicYear', 'term'],
} as const satisfies Record<string, readonly string[]>;
type Field = keyof typeof SERVER_FIELDS;
const KNOWN_SERVER_FIELDS = Object.values(SERVER_FIELDS).flat();

/**
 * Creation form of a mobility choice for the signed-in student, with the fields of the legacy
 * form: programme, country (of that programme) and partner (of that country), all but the
 * programme optional ("Définir plus tard"), mobility type, preference order and departure.
 * The partners are those `GET /api/1.0/partners` gives the student (the official partners of
 * their option), narrowed to the chosen country in the browser.
 */
@Component({
  selector: 'app-mobility-choice-form',
  imports: [ReactiveFormsModule],
  templateUrl: './mobility-choice-form.html',
  styleUrl: './mobility-choice-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MobilityChoiceForm {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly collator = new Intl.Collator(inject(LOCALE_ID), { sensitivity: 'base', numeric: true });

  /** Emitted with the new choice once the API stored it. */
  readonly created = output<MobilityChoice>();
  /** Emitted when the student closes the form without submitting. */
  readonly closed = output<void>();

  protected readonly loading = signal(true);
  protected readonly loadProblem = signal<ApiProblem | null>(null);
  protected readonly programmes = signal<Programme[]>([]);
  private readonly allCountries = signal<ProgrammeCountry[]>([]);
  private readonly allPartners = signal<Partner[]>([]);

  protected readonly orders = Array.from({ length: MAX_PREFERENCE_ORDER }, (_, i) => i + 1);
  protected readonly mobilityTypes: readonly { value: MobilityType; label: string }[] = [
    { value: 'SMS', label: $localize`:@@mobilityChoiceForm.type.sms:Académique (SMS)` },
    { value: 'SMP', label: $localize`:@@mobilityChoiceForm.type.smp:Stage (SMP)` },
  ];
  protected readonly departures = departures(new Date().getFullYear());

  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    programme: ['', Validators.required],
    country: [''],
    partner: [''],
    mobilityType: ['SMS' as MobilityType | '', Validators.required],
    preferenceOrder: ['', Validators.required],
    departure: ['', Validators.required],
  });
  private readonly programme = toSignal(this.form.controls.programme.valueChanges, { initialValue: '' });
  private readonly country = toSignal(this.form.controls.country.valueChanges, { initialValue: '' });

  /** The countries of the chosen programme, by name. */
  protected readonly countries = computed(() => {
    const programme = Number(this.programme());
    return this.allCountries()
      .filter((c) => c.programme?.id === programme)
      .sort((a, b) => this.collator.compare(a.name, b.name));
  });
  /** The student's partners in the chosen country, by name. */
  protected readonly partners = computed(() => {
    const country = this.country();
    return country
      ? this.allPartners()
          .filter((p) => p.address.country.countryCode === country)
          .sort((a, b) => this.collator.compare(a.fullName, b.fullName))
      : [];
  });

  protected readonly submitting = signal(false);
  protected readonly submitted = signal(false);
  protected readonly problem = signal<ApiProblem | null>(null);
  protected readonly otherErrors = computed(() => unmatchedFieldErrors(this.problem(), KNOWN_SERVER_FIELDS));

  constructor() {
    // a country or a partner that the new programme or country does not offer is dropped
    this.form.controls.programme.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.form.controls.country.setValue('');
    });
    this.form.controls.country.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      this.form.controls.partner.setValue('');
    });
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.loadProblem.set(null);
    forkJoin({ programmes: this.api.programmes(), countries: this.api.countries(), partners: this.api.partners() })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: ({ programmes, countries, partners }) => {
          this.programmes.set(programmes);
          this.allCountries.set(countries);
          this.allPartners.set(partners);
        },
        error: (error: unknown) => this.fail(toApiProblem(error), this.loadProblem),
      });
  }

  private fail(problem: ApiProblem, target: { set(value: ApiProblem | null): void }): void {
    if (problem.code === ClientProblemCode.UNAUTHENTICATED) {
      this.session.clear();
      void this.router.navigate(['/signin']);
      return;
    }
    target.set(problem);
  }

  protected errorFor(field: Field): string | null {
    return fieldMessage(this.problem(), SERVER_FIELDS[field], this.form.controls[field], this.submitted());
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
    const value = this.form.getRawValue();
    const departure = this.departures.find((d) => d.value === value.departure)!;
    const request: MobilityChoiceRequest = {
      user: { id: this.session.user()!.id! },
      preferenceOrder: Number(value.preferenceOrder),
      mobilityType: value.mobilityType as MobilityType,
      academicYear: departure.academicYear,
      term: departure.term,
      programme: { id: Number(value.programme) },
    };
    if (value.country) {
      request.country = { countryCode: value.country };
    }
    if (value.partner) {
      request.partner = { id: Number(value.partner) };
    }
    this.submitting.set(true);
    this.problem.set(null);
    this.api
      .createMobilityChoice(request)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: (choice) => this.created.emit(choice),
        error: (error: unknown) => this.fail(toApiProblem(error), this.problem),
      });
  }

  protected close(): void {
    this.closed.emit();
  }
}
