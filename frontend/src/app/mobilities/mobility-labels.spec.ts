import { Mobility } from '../core/models';
import {
  destination,
  mobilityStateClass,
  mobilityStateLabel,
  studentName,
} from './mobility-labels';

describe('mobility labels', () => {
  it('name the states of the API (French source)', () => {
    expect(
      [
        'Créée',
        'En préparation',
        'A payer',
        'En cours',
        'Solde à payer',
        'Terminée',
        'Annulée',
      ].map(mobilityStateLabel),
    ).toEqual([
      'Créée',
      'En préparation',
      'A payer',
      'En cours',
      'Solde à payer',
      'Terminée',
      'Annulée',
    ]);
    expect(mobilityStateLabel('Inconnu')).toBe('Inconnu');
  });

  it('class the states as running, closed or cancelled', () => {
    expect(mobilityStateClass('En cours')).toBe('badge-running');
    expect(mobilityStateClass('Terminée')).toBe('badge-closed');
    expect(mobilityStateClass('Annulée')).toBe('badge-cancelled');
  });

  const mobility = (patch: Partial<Mobility>): Mobility =>
    ({ nominatedStudent: { id: 2, firstName: 'Alice', lastName: 'Tester' }, ...patch }) as Mobility;

  it('write the student as "First Last", skipping a missing part', () => {
    expect(studentName(mobility({}))).toBe('Alice Tester');
    expect(studentName(mobility({ nominatedStudent: { id: 2, lastName: 'Tester' } }))).toBe(
      'Tester',
    );
  });

  it('name the partner, else the country, else nothing', () => {
    const country = { countryCode: 'FR', name: 'France' };
    expect(destination(mobility({ partner: { id: 1, fullName: 'Lyon' }, country }))).toBe('Lyon');
    expect(destination(mobility({ partner: null, country }))).toBe('France');
    expect(destination(mobility({ country: { countryCode: 'FR' } }))).toBe('FR');
    expect(destination(mobility({}))).toBe('');
  });
});
