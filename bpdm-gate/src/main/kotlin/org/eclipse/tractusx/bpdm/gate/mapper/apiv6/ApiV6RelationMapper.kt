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

package org.eclipse.tractusx.bpdm.gate.mapper.apiv6

import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.RelationTypeV6
import org.eclipse.tractusx.bpdm.gate.api.v6.model.relation.input.RelationV6Dto
import org.eclipse.tractusx.bpdm.gate.exception.BpdmRelationAlreadyExistsException
import org.eclipse.tractusx.bpdm.gate.exception.BpdmRelationInputDeleteException
import org.eclipse.tractusx.bpdm.gate.exception.BpdmRelationInputUpsertException
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationInputRecord
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputDeleteError
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertError.*
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationType
import org.springframework.stereotype.Component

/**
 * Maps relation records and errors to their API v6 form.
 */
@Component
class ApiV6RelationMapper {

    /**
     * Returns the API relation for [record].
     */
    fun toDto(record: RelationInputRecord): RelationV6Dto =
        RelationV6Dto(
            externalId = record.externalId,
            relationType = toDto(record.relationType),
            businessPartnerSourceExternalId = record.sourceExternalId,
            businessPartnerTargetExternalId = record.targetExternalId,
            updatedAt = record.updatedAt,
            createdAt = record.createdAt
        )

    /**
     * Returns the conflict for a relation that already exists if [errors] name one, and the bad request reporting every
     * error otherwise.
     */
    fun toUpsertException(errors: List<RelationInputUpsertError>): RuntimeException =
        errors.filterIsInstance<AlreadyExists>().firstOrNull()
            ?.let { BpdmRelationAlreadyExistsException(it.externalId) }
            ?: BpdmRelationInputUpsertException(errors.map(::toMessage))

    /**
     * Returns the bad request reporting every error in [errors].
     */
    fun toDeleteException(errors: List<RelationInputDeleteError>): RuntimeException =
        BpdmRelationInputDeleteException(errors.map(::toMessage))

    private fun toMessage(error: RelationInputDeleteError): String =
        when (error) {
            is RelationInputDeleteError.NotFound -> "Relation '${error.externalId}' can not be found."
        }

    private fun toMessage(error: RelationInputUpsertError): String =
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
                "${relation(error.externalId)} is invalid: validFrom '${error.validFrom}' of a validity period is after its validTo '${error.validTo}'"
        }

    private fun relation(externalId: String?) =
        if (externalId != null) "Relation '$externalId'" else "Relation without external ID"

    private fun toDto(relationType: RelationType): RelationTypeV6 =
        when (relationType) {
            RelationType.IsManagedBy -> RelationTypeV6.IsManagedBy
            RelationType.IsAlternativeHeadquarterFor -> RelationTypeV6.IsAlternativeHeadquarterFor
            RelationType.IsOwnedBy -> RelationTypeV6.IsOwnedBy
            RelationType.IsReplacedBy -> RelationTypeV6.IsReplacedBy
        }
}
