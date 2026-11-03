package com.dragomitch.ipl.pae.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A document of a mobility, to fill in before the departure or after the return.
 *
 * @param id the id
 * @param name the name
 * @param category {@code D} (departure document) or {@code R} (return document)
 * @param filledIn whether the document of the mobility is filled in
 * @param programme the programme of the document, when it is read
 * @param version the version
 */
public record DocumentDto(
    int id,
    String name,
    @Schema(description = "D (departure document) or R (return document).") char category,
    boolean filledIn,
    ProgrammeDto programme,
    @Schema(description = ApiDocs.VERSION) int version) {}
