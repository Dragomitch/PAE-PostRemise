import { MOBILITY_STATES, Mobility } from '../core/models';

/** The state in the UI language; the API spells it in French, an unknown one is shown as is. */
export function mobilityStateLabel(state: string): string {
  switch (state) {
    case MOBILITY_STATES.created:
      return $localize`:@@mobilities.state.created:Créée`;
    case MOBILITY_STATES.inPreparation:
      return $localize`:@@mobilities.state.inPreparation:En préparation`;
    case MOBILITY_STATES.toBePaid:
      return $localize`:@@mobilities.state.toBePaid:A payer`;
    case MOBILITY_STATES.inProgress:
      return $localize`:@@mobilities.state.inProgress:En cours`;
    case MOBILITY_STATES.balanceToBePaid:
      return $localize`:@@mobilities.state.balanceToBePaid:Solde à payer`;
    case MOBILITY_STATES.closed:
      return $localize`:@@mobilities.state.closed:Terminée`;
    case MOBILITY_STATES.cancelled:
      return $localize`:@@mobilities.state.cancelled:Annulée`;
    default:
      return state;
  }
}

/** Class of the state badge: finished, cancelled, or still running. */
export function mobilityStateClass(state: string): string {
  switch (state) {
    case MOBILITY_STATES.closed:
      return 'badge-closed';
    case MOBILITY_STATES.cancelled:
      return 'badge-cancelled';
    default:
      return 'badge-running';
  }
}

/** "First Last", as the legacy views wrote the student's name. */
export function studentName(mobility: Mobility): string {
  const student = mobility.nominatedStudent;
  return [student.firstName, student.lastName].filter(Boolean).join(' ');
}

/** The partner's name, or the country's when the mobility has no partner. */
export function destination(mobility: Mobility): string {
  return (
    mobility.partner?.fullName ?? mobility.country?.name ?? mobility.country?.countryCode ?? ''
  );
}
