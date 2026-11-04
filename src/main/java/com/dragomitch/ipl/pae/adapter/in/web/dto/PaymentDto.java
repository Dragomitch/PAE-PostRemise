package com.dragomitch.ipl.pae.adapter.in.web.dto;

import java.time.LocalDateTime;

/**
 * A payment of a mobility: one per payment request.
 *
 * @param mobilityChoiceId the id of the mobility
 * @param user the student (id and names)
 * @param nominatedStudent the nominated student, when it is read
 * @param mobilityType SMS or SMP
 * @param academicYear the academic year
 * @param term the term
 * @param programme the programme (id and name)
 * @param country the country, when the choice has one
 * @param partner the partner, when the choice has one
 * @param paymentDate the date of the request
 * @param paymentType {@code D} for the first payment, {@code R} for the second one
 */
public record PaymentDto(
    int mobilityChoiceId,
    UserDto user,
    NominatedStudentDto nominatedStudent,
    String mobilityType,
    String academicYear,
    int term,
    ProgrammeDto programme,
    CountryDto country,
    PartnerDto partner,
    LocalDateTime paymentDate,
    String paymentType) {}
