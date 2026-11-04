package com.dragomitch.ipl.pae.domain.model;

/**
 * The type of a mobility, as the Erasmus+ programme names it. The name is the value stored in
 * {@code mobility_choices.mobility_type} and sent in the JSON.
 */
public enum MobilityType {
  /** Studies abroad (student mobility for studies). */
  SMS,
  /** Internship abroad (student mobility for placements). */
  SMP
}
