import { MobilityChoice } from '../core/models';

/** State of a choice: rejected by a professor, cancelled by the student, or still pending. */
export type ChoiceState = 'rejected' | 'canceled' | 'pending';

/** Maximum length of a cancellation reason (`MobilityChoiceDto.CANCELLATION_REASON_MAX_LENGTH`). */
export const CANCELLATION_REASON_MAX_LENGTH = 500;

export function choiceState(choice: MobilityChoice): ChoiceState {
  if (choice.denialReason) {
    return 'rejected';
  }
  return choice.cancellationReason ? 'canceled' : 'pending';
}

/** Departure as the legacy UI wrote it: "Septembre 2026" (term 1) or "Février 2027" (term 2). */
export function departureLabel(term: number, academicYear: number): string {
  return term === 1
    ? $localize`:@@mobilityChoices.departure.september:Septembre ${academicYear}:YEAR:`
    : $localize`:@@mobilityChoices.departure.february:Février ${academicYear}:YEAR:`;
}

export function mobilityTypeLabel(type: string): string {
  return type === 'SMS'
    ? $localize`:@@mobilityChoices.type.sms:Académique`
    : $localize`:@@mobilityChoices.type.smp:Stage`;
}
