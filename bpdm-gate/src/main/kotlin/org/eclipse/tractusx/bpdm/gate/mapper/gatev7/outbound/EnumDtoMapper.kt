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

import org.eclipse.tractusx.bpdm.gate.model.changelog.ChangelogType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationSharingStateErrorCode
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationSharingStateType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.RelationType
import org.eclipse.tractusx.bpdm.gate.model.raw.relation.SharableRelationType
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.BusinessPartnerSharingError
import org.eclipse.tractusx.bpdm.gate.model.sharingstate.SharingStateType
import org.springframework.stereotype.Component
import org.eclipse.tractusx.bpdm.gate.api.exception.BusinessPartnerSharingError as BusinessPartnerSharingErrorDto
import org.eclipse.tractusx.bpdm.gate.api.model.ChangelogType as ChangelogTypeDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateErrorCode as RelationSharingStateErrorCodeDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationSharingStateType as RelationSharingStateTypeDto
import org.eclipse.tractusx.bpdm.gate.api.model.RelationType as RelationTypeDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharableRelationType as SharableRelationTypeDto
import org.eclipse.tractusx.bpdm.gate.api.model.SharingStateType as SharingStateTypeDto

/**
 * Maps the Gate's enumerated values to their counterparts in the API.
 */
@Component
class EnumDtoMapper {

    /**
     * Returns the API changelog type for [changelogType].
     */
    fun toDto(changelogType: ChangelogType): ChangelogTypeDto =
        when (changelogType) {
            ChangelogType.CREATE -> ChangelogTypeDto.CREATE
            ChangelogType.UPDATE -> ChangelogTypeDto.UPDATE
        }

    /**
     * Returns the API sharing state type for [sharingStateType].
     */
    fun toDto(sharingStateType: SharingStateType): SharingStateTypeDto =
        when (sharingStateType) {
            SharingStateType.Pending -> SharingStateTypeDto.Pending
            SharingStateType.Success -> SharingStateTypeDto.Success
            SharingStateType.Error -> SharingStateTypeDto.Error
            SharingStateType.Initial -> SharingStateTypeDto.Initial
            SharingStateType.Ready -> SharingStateTypeDto.Ready
        }

    /**
     * Returns the API sharing error code for [sharingError].
     */
    fun toDto(sharingError: BusinessPartnerSharingError): BusinessPartnerSharingErrorDto =
        when (sharingError) {
            BusinessPartnerSharingError.SharingProcessError -> BusinessPartnerSharingErrorDto.SharingProcessError
            BusinessPartnerSharingError.SharingTimeout -> BusinessPartnerSharingErrorDto.SharingTimeout
            BusinessPartnerSharingError.BpnNotInPool -> BusinessPartnerSharingErrorDto.BpnNotInPool
            BusinessPartnerSharingError.MissingTaskID -> BusinessPartnerSharingErrorDto.MissingTaskID
            BusinessPartnerSharingError.NaturalPersonError -> BusinessPartnerSharingErrorDto.NaturalPersonError
            BusinessPartnerSharingError.BpnErrorNotFound -> BusinessPartnerSharingErrorDto.BpnErrorNotFound
            BusinessPartnerSharingError.BpnErrorTooManyOptions -> BusinessPartnerSharingErrorDto.BpnErrorTooManyOptions
            BusinessPartnerSharingError.MandatoryFieldValidationFailed -> BusinessPartnerSharingErrorDto.MandatoryFieldValidationFailed
            BusinessPartnerSharingError.BlacklistCountryPresent -> BusinessPartnerSharingErrorDto.BlacklistCountryPresent
            BusinessPartnerSharingError.UnknownSpecialCharacters -> BusinessPartnerSharingErrorDto.UnknownSpecialCharacters
        }

    /**
     * Returns the API relation type for the relation input type [relationType].
     */
    fun toDto(relationType: RelationType): RelationTypeDto =
        when (relationType) {
            RelationType.IsManagedBy -> RelationTypeDto.IsManagedBy
            RelationType.IsAlternativeHeadquarterFor -> RelationTypeDto.IsAlternativeHeadquarterFor
            RelationType.IsOwnedBy -> RelationTypeDto.IsOwnedBy
            RelationType.IsReplacedBy -> RelationTypeDto.IsReplacedBy
        }

    /**
     * Returns the API relation type for the relation output type [relationType].
     */
    fun toDto(relationType: SharableRelationType): SharableRelationTypeDto =
        when (relationType) {
            SharableRelationType.IsAlternativeHeadquarterFor -> SharableRelationTypeDto.IsAlternativeHeadquarterFor
            SharableRelationType.IsManagedBy -> SharableRelationTypeDto.IsManagedBy
            SharableRelationType.IsOwnedBy -> SharableRelationTypeDto.IsOwnedBy
            SharableRelationType.IsReplacedBy -> SharableRelationTypeDto.IsReplacedBy
        }

    /**
     * Returns the API relation sharing state type for [sharingStateType].
     */
    fun toDto(sharingStateType: RelationSharingStateType): RelationSharingStateTypeDto =
        when (sharingStateType) {
            RelationSharingStateType.Ready -> RelationSharingStateTypeDto.Ready
            RelationSharingStateType.Pending -> RelationSharingStateTypeDto.Pending
            RelationSharingStateType.Error -> RelationSharingStateTypeDto.Error
            RelationSharingStateType.Success -> RelationSharingStateTypeDto.Success
        }

    /**
     * Returns the API relation sharing error code for [errorCode].
     */
    fun toDto(errorCode: RelationSharingStateErrorCode): RelationSharingStateErrorCodeDto =
        when (errorCode) {
            RelationSharingStateErrorCode.SharingProcessError -> RelationSharingStateErrorCodeDto.SharingProcessError
            RelationSharingStateErrorCode.SharingTimeout -> RelationSharingStateErrorCodeDto.SharingTimeout
        }
}
