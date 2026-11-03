import { ROLE_PROFESSOR, ROLE_STUDENT } from './models';

/** The role as the UI names it ("Professeur", "Étudiant"); an unknown role is shown as is. */
export function roleLabel(role: string | undefined): string {
  switch (role) {
    case ROLE_PROFESSOR:
      return $localize`:@@role.professor:Professeur`;
    case ROLE_STUDENT:
      return $localize`:@@role.student:Étudiant`;
    default:
      return role ?? '';
  }
}
