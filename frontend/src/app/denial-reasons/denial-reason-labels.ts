/** Maximum length of a denial reason (`DenialReasonDto.REASON_MAX_LENGTH`). */
export const DENIAL_REASON_MAX_LENGTH = 300;

/** Message of a reason longer than the API accepts. */
export function denialReasonTooLong(): string {
  return $localize`:@@denialReasons.reason.tooLong:La raison ne peut pas dépasser ${DENIAL_REASON_MAX_LENGTH}:MAX: caractères.`;
}
