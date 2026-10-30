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

package org.eclipse.tractusx.bpdm.gate.mapper.apiv7

import org.eclipse.tractusx.bpdm.gate.api.v7.model.RelationV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.RelationOutputV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.RelationSharingStateV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.RelationSharingStateErrorCodeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.RelationSharingStateTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.RelationTypeV7
import org.eclipse.tractusx.bpdm.gate.api.v7.model.RelationValidityPeriodV7Dto
import org.eclipse.tractusx.bpdm.gate.api.v7.model.SharableRelationTypeV7
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationInputRecord
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationOutputRecord
import org.eclipse.tractusx.bpdm.gate.model.output.relation.RelationSharingStateRecord
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertError
import org.eclipse.tractusx.bpdm.gate.model.parsed.relation.RelationInputUpsertError.*
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.SharableRelationType
import org.springframework.stereotype.Component

/**
 * Maps relation records and errors to their API v7 form.
 */
@Component
class ApiV7RelationMapper {

    /**
     * Returns the API relation for [record].
     */
    fun toDto(record: RelationInputRecord): RelationV7Dto =
        RelationV7Dto(
            externalId = record.externalId,
            relationType = toDto(record.relationType),
            businessPartnerSourceExternalId = record.sourceExternalId,
            businessPartnerTargetExternalId = record.targetExternalId,
            validityPeriods = record.validityPeriods.map { RelationValidityPeriodV7Dto(validFrom = it.validFrom, validTo = it.validTo) },
            reasonCode = record.reasonCode,
            updatedAt = record.updatedAt,
            createdAt = record.createdAt
        )

    /**
     * Returns the API relation output for [record].
     */
    fun toDto(record: RelationOutputRecord): RelationOutputV7Dto =
        RelationOutputV7Dto(
            externalId = record.externalId,
            relationType = toDto(record.relationType),
            sourceBpn = record.sourceBpn,
            targetBpn = record.targetBpn,
            validityPeriods = record.validityPeriods.map { RelationValidityPeriodV7Dto(validFrom = it.validFrom, validTo = it.validTo) },
            reasonCode = record.reasonCode,
            updatedAt = record.updatedAt
        )

    /**
     * Returns the API relation sharing state for [record].
     */
    fun toDto(record: RelationSharingStateRecord): RelationSharingStateV7Dto =
        RelationSharingStateV7Dto(
            externalId = record.externalId,
            sharingStateType = toDto(record.sharingStateType),
            sharingErrorCode = record.sharingErrorCode?.let(::toDto),
            sharingErrorMessage = record.sharingErrorMessage,
            taskId = record.taskId,
            updatedAt = record.updatedAt
        )

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

    /**
     * Returns the API relation type for the relation input type [relationType].
     */
    fun toDto(relationType: RelationType): RelationTypeV7 =
        when (relationType) {
            RelationType.IsManagedBy -> RelationTypeV7.IsManagedBy
            RelationType.IsAlternativeHeadquarterFor -> RelationTypeV7.IsAlternativeHeadquarterFor
            RelationType.IsOwnedBy -> RelationTypeV7.IsOwnedBy
            RelationType.IsReplacedBy -> RelationTypeV7.IsReplacedBy
        }

    /**
     * Returns the API relation type for the relation output type [relationType].
     */
    fun toDto(relationType: SharableRelationType): SharableRelationTypeV7 =
        when (relationType) {
            SharableRelationType.IsAlternativeHeadquarterFor -> SharableRelationTypeV7.IsAlternativeHeadquarterFor
            SharableRelationType.IsManagedBy -> SharableRelationTypeV7.IsManagedBy
            SharableRelationType.IsOwnedBy -> SharableRelationTypeV7.IsOwnedBy
            SharableRelationType.IsReplacedBy -> SharableRelationTypeV7.IsReplacedBy
        }

    /**
     * Returns the API relation sharing state type for [sharingStateType].
     */
    fun toDto(sharingStateType: RelationSharingStateType): RelationSharingStateTypeV7 =
        when (sharingStateType) {
            RelationSharingStateType.Ready -> RelationSharingStateTypeV7.Ready
            RelationSharingStateType.Pending -> RelationSharingStateTypeV7.Pending
            RelationSharingStateType.Error -> RelationSharingStateTypeV7.Error
            RelationSharingStateType.Success -> RelationSharingStateTypeV7.Success
        }

    /**
     * Returns the API relation sharing error code for [errorCode].
     */
    fun toDto(errorCode: RelationSharingStateErrorCode): RelationSharingStateErrorCodeV7 =
        when (errorCode) {
            RelationSharingStateErrorCode.SharingProcessError -> RelationSharingStateErrorCodeV7.SharingProcessError
            RelationSharingStateErrorCode.SharingTimeout -> RelationSharingStateErrorCodeV7.SharingTimeout
        }
}
