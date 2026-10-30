/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 */


package org.eclipse.tractusx.bpdm.gate.mapper.gatev7.outbound

import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertError.*
import org.springframework.stereotype.Component

/**
 * Maps a rejected relation input upsert to the error messages the API reports.
 */
@Component
class RelationInputUpsertErrorMapper {

    /**
     * Returns the message the API reports for [error].
     */
    fun toMessage(error: RelationInputUpsertError): String =
        when (error) {
            is UpsertLimitExceeded ->
                "At most ${error.limit} relations may be upserted at once, but ${error.actual} were given"
            is DuplicateExternalIds ->
                "External IDs occur more than once in the batch: ${error.externalIds.joinToString { "'$it'" }}"
            is NotFound ->
                "${relation(error.externalId)} can not be found."
            is AlreadyExists ->
                "Relation '${error.externalId}' already exists."
            is SourceNotFound ->
                "${relation(error.externalId)} is invalid: its source business partner '${error.sourceExternalId}' does not exist"
            is TargetNotFound ->
                "${relation(error.externalId)} is invalid: its target business partner '${error.targetExternalId}' does not exist"
            is SourceEqualsTarget ->
                "${relation(error.externalId)} is invalid: business partner '${error.businessPartnerExternalId}' is both its source and its target"
            is InvalidValidityPeriod ->
                "${relation(error.externalId)} is invalid: validFrom '${error.validFrom}' of a validity period is not before its validTo '${error.validTo}'"
        }

    private fun relation(externalId: String?) =
        if (externalId != null) "Relation '$externalId'" else "Relation without external ID"
}
